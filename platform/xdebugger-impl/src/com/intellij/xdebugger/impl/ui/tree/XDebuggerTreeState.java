// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.xdebugger.impl.ui.tree;

import com.intellij.util.concurrency.ThreadingAssertions;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.xdebugger.XNamedTreeNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.RestorableStateNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public final class XDebuggerTreeState {
  private final NodeInfo myRootInfo;
  private Rectangle myLastVisibleNodeRect;

  private XDebuggerTreeState(@NotNull XDebuggerTree tree) {
    ThreadingAssertions.assertEventDispatchThread();
    XDebuggerTreeNode root = tree.getRoot();
    myRootInfo = root != null ? new NodeInfo("", "", tree.isPathSelected(root.getPath())) : null;
    if (root != null) {
      addChildren(tree, myRootInfo, root);
    }
  }

  public XDebuggerTreeRestorer restoreState(@NotNull XDebuggerTree tree) {
    ThreadingAssertions.assertEventDispatchThread();
    XDebuggerTreeRestorer restorer = null;
    if (myRootInfo != null) {
      restorer = new XDebuggerTreeRestorer(tree, myLastVisibleNodeRect);
      tree.setCurrentRestorer(restorer);
      restorer.restore(tree.getRoot(), myRootInfo);
    }
    return restorer;
  }

  public static XDebuggerTreeState saveState(@NotNull XDebuggerTree tree) {
    return new XDebuggerTreeState(tree);
  }

  private void addChildren(final XDebuggerTree tree, final NodeInfo nodeInfo, final XDebuggerTreeNode treeNode) {
    if (tree.isExpanded(treeNode.getPath())) {
      List<? extends XDebuggerTreeNode> children = treeNode.getLoadedChildren();
      nodeInfo.myExpanded = true;
      for (XDebuggerTreeNode child : children) {
        TreePath path = child.getPath();
        Rectangle bounds = tree.getPathBounds(path);
        if (bounds != null) {
          Rectangle treeVisibleRect =
            tree.getParent() instanceof JViewport ? ((JViewport)tree.getParent()).getViewRect() : tree.getVisibleRect();
          if (treeVisibleRect.contains(bounds)) {
            myLastVisibleNodeRect = bounds;
          }
        }
        NodeInfo childInfo = createNode(child, tree.isPathSelected(path));
        if (childInfo != null) {
          nodeInfo.addChild(childInfo);
          addChildren(tree, childInfo, child);
        }
      }
    }
  }

  private static @Nullable NodeInfo createNode(final XDebuggerTreeNode node, boolean selected) {
    if (node instanceof RestorableStateNode valueNode) {
      if (valueNode.isComputed()) {
        return new NodeInfo(valueNode.getName(), valueNode.getRawValue(), selected);
      }
    }
    return null;
  }

  public static class NodeInfo {
    private final String myName;
    private final String myValue;
    private boolean myExpanded;
    private final boolean mySelected;
    private MultiMap<String, NodeInfo> myChildren; // MultiMap to allow several nodes with the same name

    public NodeInfo(final String name, final String value, boolean selected) {
      myName = name;
      myValue = value;
      mySelected = selected;
    }

    public void addChild(@NotNull NodeInfo child) {
      if (myChildren == null) {
        myChildren = new MultiMap<>();
      }
      myChildren.putValue(child.myName, child);
    }

    public boolean isExpanded() {
      return myExpanded;
    }

    public boolean isSelected() {
      return mySelected;
    }

    public String getValue() {
      return myValue;
    }

    public @Nullable NodeInfo getChild(XNamedTreeNode node) {
      String name = node.getName();
      if (myChildren == null) {
        return null;
      }
      List<NodeInfo> infos = (List<NodeInfo>)myChildren.get(name);
      if (infos.size() > 1) {
        TreeNode parent = node.getParent();
        if (parent instanceof XDebuggerTreeNode) {
          int idx = 0;
          for (XDebuggerTreeNode treeNode : ((XDebuggerTreeNode)parent).getLoadedChildren()) {
            if (treeNode == node) {
              break;
            }
            if (treeNode instanceof XNamedTreeNode && Objects.equals(((XNamedTreeNode)treeNode).getName(), name)) {
              idx++;
            }
          }
          if (idx < infos.size()) {
            return infos.get(idx);
          }
        }
      }
      return ContainerUtil.getFirstItem(infos);
    }
  }
}
