// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.hint;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.NonBlockingReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.SlowOperations;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.EdtScheduler;
import com.intellij.util.ui.AnimatedIcon;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import kotlinx.coroutines.Job;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ParameterInfoTaskRunnerUtil {
  public static final int DEFAULT_PROGRESS_POPUP_DELAY_MS = 1000;

  /**
   * @param progressTitle null means no loading panel should be shown
   */
  public static <T> void runTask(Project project,
                                 NonBlockingReadAction<T> nonBlockingReadAction,
                                 Consumer<? super T> continuationConsumer,
                                 @Nullable @NlsContexts.ProgressTitle String progressTitle,
                                 Editor editor) {
    runTask(project, nonBlockingReadAction, continuationConsumer, progressTitle, editor, true);
  }


  /**
   * @param progressTitle   null means no loading panel should be shown
   * @param stopOnScrolling cancel execution on scrolling
   */
  public static <T> void runTask(Project project,
                                 NonBlockingReadAction<T> nonBlockingReadAction,
                                 Consumer<? super T> continuationConsumer,
                                 @Nullable @NlsContexts.ProgressTitle String progressTitle,
                                 Editor editor,
                                 boolean stopOnScrolling) {
    AtomicReference<CancellablePromise<?>> cancellablePromiseRef = new AtomicReference<>();
    Consumer<Boolean> stopAction =
      startProgressAndCreateStopAction(editor.getProject(), progressTitle, cancellablePromiseRef, editor);

    final VisibleAreaListener visibleAreaListener = new CancelProgressOnScrolling(cancellablePromiseRef);

    if (stopOnScrolling) {
      editor.getScrollingModel().addVisibleAreaListener(visibleAreaListener);
    }

    final Component focusOwner = getFocusOwner(project);

    cancellablePromiseRef.set(
      nonBlockingReadAction.finishOnUiThread(
          ModalityState.defaultModalityState(),
          continuation -> {
            CancellablePromise<?> promise = cancellablePromiseRef.get();
            if (promise != null && promise.isSucceeded()) {
              Component owner = getFocusOwner(project);
              if (Objects.equals(focusOwner, owner) || owner instanceof ParameterInfoController.WrapperPanel) {
                try (AccessToken ignore = SlowOperations.knownIssue("IJPL-173194")) {
                  continuationConsumer.accept(continuation);
                }
              }
            }
          })
        .expireWith(editor instanceof EditorImpl ? ((EditorImpl)editor).getDisposable() : project)
        .submit(AppExecutorUtil.getAppExecutorService())
        .onProcessed(ignore -> {
          stopAction.accept(false);
          if (stopOnScrolling) {
            editor.getScrollingModel().removeVisibleAreaListener(visibleAreaListener);
          }
        }));
  }

  private static Component getFocusOwner(Project project) {
    return IdeFocusManager.getInstance(project).getFocusOwner();
  }

  private static @NotNull Consumer<Boolean> startProgressAndCreateStopAction(Project project,
                                                                             @NlsContexts.ProgressTitle String progressTitle,
                                                                             AtomicReference<? extends CancellablePromise<?>> promiseRef,
                                                                             Editor editor) {
    AtomicReference<Consumer<Boolean>> stopActionRef = new AtomicReference<>();

    Consumer<Boolean> originalStopAction = (cancel) -> {
      stopActionRef.set(null);
      if (cancel) {
        CancellablePromise<?> promise = promiseRef.get();
        if (promise != null) {
          promise.cancel();
        }
      }
    };

    if (progressTitle == null) {
      stopActionRef.set(originalStopAction);
    }
    else {
      final Disposable disposable = Disposer.newDisposable();
      Disposer.register(project, disposable);

      JBLoadingPanel loadingPanel =
        new JBLoadingPanel(null, panel -> new LoadingDecorator(panel, disposable, 0, false, new AsyncProcessIcon("ShowParameterInfo")) {
          @Override
          protected @NotNull NonOpaquePanel customizeLoadingLayer(JPanel parent, JLabel text, AnimatedIcon icon) {
            parent.setLayout(new FlowLayout(FlowLayout.LEFT));
            NonOpaquePanel result = new NonOpaquePanel();
            result.add(icon);
            parent.add(result);
            return result;
          }
        });
      loadingPanel.add(new JBLabel(EmptyIcon.ICON_18));
      loadingPanel.add(new JBLabel(progressTitle));

      ComponentPopupBuilder builder =
        JBPopupFactory.getInstance().createComponentPopupBuilder(loadingPanel, null)
          .setProject(project)
          .setCancelCallback(() -> {
            Consumer<Boolean> stopAction = stopActionRef.get();
            if (stopAction != null) {
              stopAction.accept(true);
            }
            return true;
          });
      JBPopup popup = builder.createPopup();
      Disposer.register(disposable, popup);
      Job showPopupFuture =
        EdtScheduler.getInstance().schedule(DEFAULT_PROGRESS_POPUP_DELAY_MS, ModalityState.defaultModalityState(), () -> {
          if (!popup.isDisposed() && !popup.isVisible() && !editor.isDisposed()) {
            RelativePoint popupPosition = JBPopupFactory.getInstance().guessBestPopupLocation(editor);
            loadingPanel.startLoading();
            popup.show(popupPosition);
          }
        });

      stopActionRef.set((cancel) -> {
        try {
          loadingPanel.stopLoading();
          originalStopAction.accept(cancel);
        }
        finally {
          showPopupFuture.cancel(null);
          UIUtil.invokeLaterIfNeeded(() -> {
            if (popup.isVisible()) {
              popup.setUiVisible(false);
            }
            Disposer.dispose(disposable);
          });
        }
      });
    }

    return stopActionRef.get();
  }
}
