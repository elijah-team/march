// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.java.decompiler.modules.decompiler.sforms;

import org.jetbrains.java.decompiler.main.CancellationManager;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.sforms.FlattenStatementsHelper.FinallyPathWrapper;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


public class DirectGraph {

  public final VBStyleCollection<DirectNode, String> nodes = new VBStyleCollection<>();

  public DirectNode first;

  // exit, [source, destination]
  public final HashMap<String, List<FinallyPathWrapper>> mapShortRangeFinallyPaths = new HashMap<>();

  // exit, [source, destination]
  public final HashMap<String, List<FinallyPathWrapper>> mapLongRangeFinallyPaths = new HashMap<>();

  // negative if branches (recorded for handling of && and ||)
  public final HashMap<String, String> mapNegIfBranch = new HashMap<>();

  // nodes, that are exception exits of a finally block with monitor variable
  public final HashMap<String, String> mapFinallyMonitorExceptionPathExits = new HashMap<>();

  public void sortReversePostOrder() {
    LinkedList<DirectNode> res = new LinkedList<>();
    addToReversePostOrderListIterative(first, res);

    nodes.clear();
    for (DirectNode node : res) {
      nodes.addWithKey(node, node.id);
    }
  }

  private static void addToReversePostOrderListIterative(DirectNode root, List<? super DirectNode> lst) {

    LinkedList<DirectNode> stackNode = new LinkedList<>();
    LinkedList<Integer> stackIndex = new LinkedList<>();

    HashSet<DirectNode> setVisited = new HashSet<>();

    stackNode.add(root);
    stackIndex.add(0);

    while (!stackNode.isEmpty()) {

      DirectNode node = stackNode.getLast();
      int index = stackIndex.removeLast();

      setVisited.add(node);

      for (; index < node.successors.size(); index++) {
        DirectNode succ = node.successors.get(index);

        if (!setVisited.contains(succ)) {
          stackIndex.add(index + 1);

          stackNode.add(succ);
          stackIndex.add(0);

          break;
        }
      }

      if (index == node.successors.size()) {
        lst.add(0, node);

        stackNode.removeLast();
      }
    }
  }


  public boolean iterateExprents(ExprentIterator iter) {
    CancellationManager cancellationManager = DecompilerContext.getCancellationManager();

    LinkedList<DirectNode> stack = new LinkedList<>();
    stack.add(first);

    HashSet<DirectNode> setVisited = new HashSet<>();

    while (!stack.isEmpty()) {

      DirectNode node = stack.removeFirst();

      if (setVisited.contains(node)) {
        continue;
      }
      setVisited.add(node);

      for (int i = 0; i < node.exprents.size(); i++) {
        cancellationManager.checkCanceled();
        int res = iter.processExprent(node.exprents.get(i));

        if (res == 1) {
          return false;
        }

        if (res == 2) {
          node.exprents.remove(i);
          i--;
        }
      }

      stack.addAll(node.successors);
    }

    return true;
  }

  /**
   * Used to iterate over all exprents, including nested ones.
   *
   * @param itr the {@link ExprentIterator} used to process each expression.
   * @return {@code true} if the iteration was successful within the provided constraints (itr doesn't return 1),
   *         {@code false} otherwise.
   */
  public boolean iterateExprentsDeep(ExprentIterator itr) {
    return iterateExprents(exprent -> {
      List<Exprent> lst = exprent.getAllExprents(true);
      lst.add(exprent);

      for (Exprent expr : lst) {
        int res = itr.processExprent(expr);
        if (res == 1 || res == 2) {
          return res;
        }
      }
      return 0;
    });
  }

  public interface ExprentIterator {
    // 0 - success, do nothing
    // 1 - cancel iteration
    // 2 - success, delete exprent
    int processExprent(Exprent exprent);
  }
}
