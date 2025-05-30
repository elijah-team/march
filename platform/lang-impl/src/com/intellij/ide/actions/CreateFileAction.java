// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel;
import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsageCounterCollector;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.intellij.ide.actions.FileNameStateKt.checkFileNameInput;

public class CreateFileAction extends CreateElementActionBase implements DumbAware {
  public CreateFileAction() {
  }

  /**
   * @deprecated Use {@link #CreateFileAction(Supplier, Supplier, Supplier)}
   */
  @Deprecated
  public CreateFileAction(@NlsActions.ActionText String text,
                          @NlsActions.ActionDescription String description,
                          Icon icon) {
    super(text, description, icon);
  }

  /**
   * @deprecated Use {@link #CreateFileAction(Supplier, Supplier, Supplier)}
   */
  @Deprecated(forRemoval = true)
  public CreateFileAction(@NotNull Supplier<String> dynamicText, @NotNull Supplier<String> dynamicDescription, final Icon icon) {
    super(dynamicText, dynamicDescription, icon);
  }

  public CreateFileAction(@NotNull Supplier<String> dynamicText, @NotNull Supplier<String> dynamicDescription, @Nullable Supplier<? extends @Nullable Icon> icon) {
    super(dynamicText, dynamicDescription, icon);
  }

  @Override
  public boolean isDumbAware() {
    return CreateFileAction.class.equals(getClass());
  }

  @Override
  protected PsiElement @NotNull [] invokeDialog(final @NotNull Project project, @NotNull PsiDirectory directory) {
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  protected void invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory, @NotNull Consumer<? super PsiElement[]> elementsConsumer) {
    MyInputValidator validator = new MyValidator(project, directory);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      try {
        elementsConsumer.accept(validator.create("test"));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    else {
      createLightWeightPopup(validator, elementsConsumer).showCenteredInCurrentWindow(project);
    }
  }

  private @NotNull JBPopup createLightWeightPopup(@NotNull MyInputValidator validator,
                                                  @NotNull Consumer<? super PsiElement[]> consumer) {
    NewItemSimplePopupPanel contentPanel = new NewItemSimplePopupPanel();
    JTextField nameField = contentPanel.getTextField();
    JBPopup popup = NewItemPopupUtil.createNewItemPopup(IdeBundle.message("title.new.file"), contentPanel, nameField);
    contentPanel.setApplyAction(event -> {
      String name = nameField.getText();
      if (validator.checkInput(name) && validator.canClose(name)) {
        popup.closeOk(event);
        consumer.accept(validator.getCreatedElements());
      }
      else {
        String errorMessage = validator instanceof InputValidatorEx
                              ? ((InputValidatorEx)validator).getErrorText(name)
                              : LangBundle.message("incorrect.name");
        contentPanel.setError(errorMessage);
      }
    });

    return popup;
  }

  @Override
  protected PsiElement @NotNull [] create(@NotNull String newName, @NotNull PsiDirectory directory) throws Exception {
    MkDirs mkdirs = new MkDirs(newName, directory);
    PsiFile file = WriteAction.compute(() -> mkdirs.directory.createFile(getFileName(mkdirs.newName)));
    FileTypeUsageCounterCollector.logCreated(file.getProject(), file.getVirtualFile());
    return new PsiElement[]{file};
  }

  public static @NotNull PsiDirectory findOrCreateSubdirectory(@NotNull PsiDirectory parent, @NotNull String subdirName) {
    final PsiDirectory sub = parent.findSubdirectory(subdirName);
    return sub == null ? WriteAction.compute(() -> parent.createSubdirectory(subdirName)) : sub;
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return IdeBundle.message("progress.creating.file", directory.getVirtualFile().getPresentableUrl(), File.separator, newName);
  }

  @Override
  protected String getErrorTitle() {
    return IdeBundle.message("title.cannot.create.file");
  }

  protected String getFileName(String newName) {
    if (getDefaultExtension() == null || !FileUtilRt.getExtension(newName).isEmpty()) {
      return newName;
    }
    return newName + "." + getDefaultExtension();
  }

  protected @Nullable String getDefaultExtension() {
    return null;
  }

  public static final class MkDirs {
    public final @NotNull String newName;
    public final @NotNull PsiDirectory directory;

    public MkDirs(@NotNull String newName, @NotNull PsiDirectory directory) {
      if (SystemInfo.isWindows) {
        newName = newName.replace('\\', '/');
      }
      if (newName.contains("/")) {
        final List<String> subDirs = StringUtil.split(newName, "/");
        newName = ContainerUtil.getLastItem(subDirs);
        boolean firstToken = true;
        for (int i = 0; i < subDirs.size()-1; i++) {
          String dir = subDirs.get(i);
          if (firstToken && "~".equals(dir)) {
            final VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
            if (userHomeDir == null) throw new IncorrectOperationException("User home directory not found");
            final PsiDirectory directory1 = directory.getManager().findDirectory(userHomeDir);
            if (directory1 == null) throw new IncorrectOperationException("User home directory not found");
            directory = directory1;
          }
          else if ("..".equals(dir)) {
            final PsiDirectory parentDirectory = directory.getParentDirectory();
            if (parentDirectory == null) throw new IncorrectOperationException("Not a valid directory");
            directory = parentDirectory;
          }
          else if (!".".equals(dir)) {
            directory = findOrCreateSubdirectory(directory, dir);
          }
          firstToken = false;
        }
      }

      this.newName = newName;
      this.directory = directory;
    }
  }

  protected class MyValidator extends MyInputValidator implements InputValidatorEx {
    private @NlsContexts.DetailedDescription String myErrorText;

    public MyValidator(Project project, PsiDirectory directory){
      super(project, directory);
    }

    @Override
    public boolean checkInput(String inputString) {
      PsiDirectory directory = getDirectory();
      String fileName = getFileName(inputString);
      String error = checkFileNameInput(inputString, fileName, directory);
      myErrorText = error;
      return error == null;
    }

    @Override
    public String getErrorText(String inputString) {
      return myErrorText;
    }

    @Override
    public boolean canClose(final String inputString) {
      if (inputString.isEmpty()) {
        return super.canClose(inputString);
      }

      return super.canClose(getFileName(inputString));
    }
  }
}
