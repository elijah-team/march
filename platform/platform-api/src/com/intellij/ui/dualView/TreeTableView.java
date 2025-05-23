// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ui.dualView;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.HighlightableCellRenderer;
import com.intellij.ui.table.SelectionProvider;
import com.intellij.ui.treeStructure.treetable.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.SortableColumnModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeTableView extends TreeTable implements SelectionProvider {
  private static final Logger LOG = Logger.getInstance(TreeTableView.class);

  public TreeTableView(ListTreeTableModelOnColumns treeTableModel) {
    super(treeTableModel);
    setRootVisible(false);

    setTreeCellRenderer(new TreeCellRenderer() {
      private final TreeCellRenderer myBaseRenderer = new HighlightableCellRenderer();

      @Override
      public Component getTreeCellRendererComponent(JTree tree1,
                                                    Object value,
                                                    boolean selected,
                                                    boolean expanded,
                                                    boolean leaf,
                                                    int row,
                                                    boolean hasFocus) {
        JComponent result = (JComponent)myBaseRenderer.getTreeCellRendererComponent(tree1, value, selected, expanded, leaf, row, hasFocus);
        result.setOpaque(!selected);
        return result;
      }
    });

    setSizes();
  }

  @Override
  public void setTableModel(TreeTableModel treeTableModel) {
    super.setTableModel(treeTableModel);
    LOG.assertTrue(treeTableModel instanceof SortableColumnModel);
  }

  private void setSizes() {
    ColumnInfo[] columns = ((ListTreeTableModelOnColumns)getTableModel()).getColumns();
    for (int i = 0; i < columns.length; i++) {
      ColumnInfo columnInfo = columns[i];
      TableColumn column = getColumnModel().getColumn(i);
      if (columnInfo.getWidth(this) > 0) {
        int width = columnInfo.getWidth(this);
        column.setMaxWidth(width);
        column.setMinWidth(width);
      }
      else {
        final String preferredValue = columnInfo.getPreferredStringValue();
        if (preferredValue != null) {
          int width = getFontMetrics(getFont()).stringWidth(preferredValue) + columnInfo.getAdditionalWidth();
          column.setPreferredWidth(width);
        }
      }
    }
  }

  @Override
  public TableCellEditor getCellEditor(int row, int column) {
    TableCellEditor editor = getColumnInfo(column).getEditor(getRowElement(row));
    return editor == null ? super.getCellEditor(row, column) : editor;
  }

  @Override
  public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
    return new TreeTableCellRenderer(this, getTree()) {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus,
                                                     int row,
                                                     int column) {
        JComponent component = (JComponent)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        component.setOpaque(isSelected);
        return component;
      }
    };
  }


  ListTreeTableModelOnColumns getTreeViewModel() {
    return (ListTreeTableModelOnColumns)getTableModel();
  }

  public @Unmodifiable List<DualTreeElement> getFlattenItems() {
    List<DualTreeElement> items = getTreeViewModel().getItems();
    return ContainerUtil.findAll(items, object -> object.shouldBeInTheFlatView());
  }

  @Override
  public TableCellRenderer getCellRenderer(int row, int column) {
    TableCellRenderer renderer = getColumnInfo(column).getRenderer(getRowElement(row));
    final TableCellRenderer baseRenderer = renderer == null ? super.getCellRenderer(row, column) : renderer;
    return new CellRendererWrapper(baseRenderer);
  }

  protected Object getRowElement(final int row) {
    return getTree().getPathForRow(row).getLastPathComponent();
  }

  protected final ColumnInfo<Object, ?> getColumnInfo(final int column) {
    return getTreeViewModel().getColumnInfos()[convertColumnIndexToModel(column)];
  }

  public List getItems() {
    return getTreeViewModel().getItems();
  }

  @Override
  public List getSelection() {
    final TreeTableTree tree = getTree();
    if (tree == null) return Collections.emptyList();
    final int[] rows = getSelectedRows();
    final ArrayList result = new ArrayList();
    for (int row : rows) {
      final TreePath pathForRow = tree.getPathForRow(row);
      if (pathForRow != null) result.add(pathForRow.getLastPathComponent());
    }
    return result;
  }

  @Override
  public void addSelection(Object item) {
    getTree().setExpandsSelectedPaths(true);
    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)item;
    addSelectedPath(new TreePath(treeNode.getPath()));
  }

  public static class CellRendererWrapper implements TableCellRendererWrapper {
    private final @NotNull TableCellRenderer myBaseRenderer;

    public CellRendererWrapper(@NotNull TableCellRenderer baseRenderer) {
      myBaseRenderer = baseRenderer;
    }

    @Override
    public @NotNull TableCellRenderer getBaseRenderer() {
      return myBaseRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      JComponent rendererComponent = (JComponent)myBaseRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                                                                              row, column);
      if (isSelected) {
        rendererComponent.setBackground(table.getSelectionBackground());
        rendererComponent.setForeground(table.getSelectionForeground());
      }
      else {
        final Color bg = table.getBackground();
        rendererComponent.setBackground(bg);
        rendererComponent.setForeground(table.getForeground());
      }
      rendererComponent.setOpaque(isSelected);
      return rendererComponent;
    }
  }
}
