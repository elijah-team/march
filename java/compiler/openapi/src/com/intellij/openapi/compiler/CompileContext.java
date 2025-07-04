// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * An interface allowing access and modification of the data associated with the current compile session.
 */
@ApiStatus.NonExtendable
public interface CompileContext extends UserDataHolder {
  /**
   * Allows adding a message to be shown in `Compiler` message view.
   * If correct url, line and column numbers are supplied, the navigation to the specified file is available from the view.
   *
   * @param category  the category of a message (information, error, warning).
   * @param message   the text of the message.
   * @param url       a url to the file to which the message applies, null if not available.
   * @param lineNum   a line number, -1 if not available.
   * @param columnNum a column number, -1 if not available.
   */
  default void addMessage(@NotNull CompilerMessageCategory category,
                          @Nls(capitalization = Nls.Capitalization.Sentence) String message,
                          @Nullable String url,
                          int lineNum,
                          int columnNum) {
    addMessage(category, message, url, lineNum, columnNum, null);
  }

  /**
   * Allows to add a message to be shown in Compiler message view, with a specified Navigatable
   * that is used to navigate to the error location.
   *
   * @param category    the category of a message (information, error, warning).
   * @param message     the text of the message.
   * @param url         a url to the file to which the message applies, null if not available.
   * @param lineNum     a line number, -1 if not available.
   * @param columnNum   a column number, -1 if not available.
   * @param navigatable the navigatable pointing to the error location.
   */
  default void addMessage(@NotNull CompilerMessageCategory category,
                          @Nls(capitalization = Nls.Capitalization.Sentence) String message,
                          @Nullable String url, int lineNum, int columnNum, @Nullable Navigatable navigatable) {
    addMessage(category, message, url, lineNum, columnNum, navigatable, Collections.emptyList());
  }

  /**
   * Allows to add a message to be shown in Compiler message view, with a specified Navigatable
   * that is used to navigate to the error location.
   *  @param category    the category of a message (information, error, warning).
   * @param message     the text of the message.
   * @param url         a url to the file to which the message applies, null if not available.
   * @param lineNum     a line number, -1 if not available.
   * @param columnNum   a column number, -1 if not available.
   * @param navigatable optional the navigatable pointing to the error location or null. If not specified, navigation will attempt to set cursor (line:column) position, if available.
   * @param moduleNames module name describing the context for the message (e.g. the module where the error occurred).
   *                    If a module cycle is compiled, the argument will contain names of modules that form the dependency cycle.
   */
  void addMessage(@NotNull CompilerMessageCategory category,
                  @Nls(capitalization = Nls.Capitalization.Sentence) String message,
                  @Nullable String url, int lineNum, int columnNum, @Nullable Navigatable navigatable, Collection<String> moduleNames);

  /**
   * Returns all messages of the specified category added during the current compile session.
   *
   * @param category the category for which messages are requested.
   * @return all compiler messages of the specified category
   */
  CompilerMessage @NotNull [] getMessages(@NotNull CompilerMessageCategory category);

  /**
   * Returns the count of messages of the specified category added during the current compile session.
   *
   * @param category the category for which messages are requested.
   * @return the number of messages of the specified category
   */
  int getMessageCount(@Nullable CompilerMessageCategory category);

  /**
   * Returns the progress indicator of the compilation process.
   *
   * @return the progress indicator instance.
   */
  @NotNull
  ProgressIndicator getProgressIndicator();

  /**
   * Returns the current compile scope.
   *
   * @return current compile scope
   */
  CompileScope getCompileScope();

  /**
   * Returns the compile scope which would be used if the entire project was rebuilt.
   * {@link #getCompileScope()} may return the scope, that is more narrow than ProjectCompileScope.
   *
   * @return project-wide compile scope.
   */
  CompileScope getProjectCompileScope();

  /**
   * A compiler may call this method to request complete project rebuild.
   * This may be necessary, for example, when compiler caches are corrupted.
   * @deprecated calls of this method don't affect the behavior of the compilation engine; code which performs compilation should be 
   * migrated to be run in a separate JPS build process, see {@link Compiler} for details. 
   */
  @Deprecated(forRemoval = true)
  default void requestRebuildNextTime(String message) {
  }

  /**
   * @deprecated the compilation engine doesn't support triggering rebuild from the IDE process; code which performs compilation should be 
   * migrated to be run in a separate JPS build process, see {@link Compiler} for details. 
   */
  @Deprecated(forRemoval = true)
  default boolean isRebuildRequested() {
    return false;
  }


  /**
   * @deprecated the compilation engine doesn't support triggering rebuild from the IDE process; code which performs compilation should be
   * migrated to be run in a separate JPS build process, see {@link Compiler} for details.
   */
  @Deprecated(forRemoval = true)
  @Nullable
  default String getRebuildReason() {
    return null;
  }

  /**
   * Returns the module to which the specified file belongs. This method is aware of the file->module mapping
   * for generated files.
   *
   * @param file the file to check.
   * @return the module to which the file belongs
   */
  Module getModuleByFile(@NotNull VirtualFile file);

  /**
   * Returns the output directory for the specified module.
   *
   * @param module the module to check.
   * @return the output directory for the module specified, null if corresponding VirtualFile is not valid or directory not specified
   */
  @Nullable
  VirtualFile getModuleOutputDirectory(@NotNull Module module);

  /**
   * Returns the test output directory for the specified module.
   *
   * @param module the module to check.
   * @return the tests output directory the module specified, null if corresponding VirtualFile is not valid. If in Paths settings
   *         output directory for tests is not configured explicitly, but the output path is present, the output path will be returned.
   */
  @Nullable
  VirtualFile getModuleOutputDirectoryForTests(Module module);

  /**
   * Checks if the compilation is incremental, i.e. triggered by one of "Make" actions.
   *
   * @return true if compilation is incremental. 
   */
  boolean isMake();

  boolean isAutomake();

  boolean isRebuild();

  @NotNull
  Project getProject();

  boolean isAnnotationProcessorsEnabled();
}
