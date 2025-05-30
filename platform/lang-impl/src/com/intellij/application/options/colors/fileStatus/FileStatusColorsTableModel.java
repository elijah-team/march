// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.application.options.colors.fileStatus;

import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme;
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@ApiStatus.Internal
public final class FileStatusColorsTableModel extends AbstractTableModel {
  private final EditorColorsScheme myScheme;
  private final List<FileStatusColorDescriptor> myDescriptors;

  private static final ColumnInfo[] COLUMNS_INFO = {
    new ColumnInfo(FileStatusColorDescriptor.class, descriptor -> descriptor),
    new ColumnInfo(String.class, descriptor -> descriptor.getStatus().getText())
  };

  private record ColumnInfo(Class<?> columnClass, Function<? super FileStatusColorDescriptor, Object> dataFunction) {
  }

  public FileStatusColorsTableModel(FileStatus @NotNull [] fileStatuses, @NotNull EditorColorsScheme scheme) {
    myScheme = scheme;
    myDescriptors = createDescriptors(fileStatuses, myScheme);
  }

  private static List<FileStatusColorDescriptor> createDescriptors(FileStatus @NotNull [] fileStatuses, @NotNull EditorColorsScheme scheme) {
    EditorColorsScheme baseScheme = scheme instanceof AbstractColorsScheme ? ((AbstractColorsScheme)scheme).getParentScheme() : null;
    List<FileStatusColorDescriptor> descriptors = new ArrayList<>();
    for (FileStatus fileStatus : fileStatuses) {
      Color color = scheme.getColor(fileStatus.getColorKey());
      Color uiThemeColor = UIManager.getColor(FileStatusFactory.getFilestatusUiThemePrefix() + fileStatus.getId());
      Color originalColor = baseScheme != null ? baseScheme.getColor(fileStatus.getColorKey()) : null;
      descriptors.add(new FileStatusColorDescriptor(fileStatus, color, originalColor, uiThemeColor));
    }
    descriptors.sort(Comparator.comparing(d -> d.getStatus().getText()));
    return descriptors;
  }

  @Override
  public int getRowCount() {
    return myDescriptors.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMNS_INFO.length;
  }

  @Override
  public String getColumnName(int columnIndex) {
    return columnIndex == 0 ? "" : ApplicationBundle.message("file.status.colors.header.status");
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return COLUMNS_INFO[columnIndex].columnClass;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    FileStatusColorDescriptor descriptor = myDescriptors.get(rowIndex);
    return COLUMNS_INFO[columnIndex].dataFunction.apply(descriptor);
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    myDescriptors.get(rowIndex).setColor((Color)aValue);
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  public void resetToDefault(@NotNull FileStatusColorDescriptor descriptor) {
    int row = myDescriptors.indexOf(descriptor);
    if (row == -1) return;

    descriptor.resetToDefault();
    fireTableCellUpdated(row, 1);
  }

  public void setColorFor(@NotNull FileStatusColorDescriptor descriptor, @Nullable Color color) {
    int row = myDescriptors.indexOf(descriptor);
    if (row == -1) return;

    descriptor.setColor(color);
    fireTableCellUpdated(row, 1);
  }

  @Nullable FileStatusColorDescriptor getDescriptorByName(String statusName) {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      if (statusName.equals(descriptor.getStatus().getText())) {
        return descriptor;
      }
    }
    return null;
  }

  public boolean isModified() {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      ColorKey key = descriptor.getStatus().getColorKey();
      Color original = myScheme.getColor(key);
      Color current =  descriptor.getColor();
      if (!Comparing.equal(original, current)) return true;
    }
    return false;
  }

  public void reset() {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      descriptor.setColor(myScheme.getColor(descriptor.getStatus().getColorKey()));
    }
    fireTableDataChanged();
  }

  public void apply() {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      myScheme.setColor(descriptor.getStatus().getColorKey(), descriptor.getColor());
    }
    if (myScheme instanceof AbstractColorsScheme) {
      ((AbstractColorsScheme)myScheme).setSaveNeeded(true);
    }
    if (EditorColorsManagerImpl.Companion.isTempScheme(myScheme)) {
      ColorAndFontOptions.writeTempScheme(myScheme);
    }
  }

  @Nullable FileStatusColorDescriptor getDescriptorAt(int index) {
    if (index >= 0 && index < myDescriptors.size()) {
      return myDescriptors.get(index);
    }
    return null;
  }
}
