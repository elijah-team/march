// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.vcs.log.graph.impl.print

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.SLRUMap
import com.intellij.vcs.log.graph.EdgePrintElement
import com.intellij.vcs.log.graph.VcsLogVisibleGraphIndex
import com.intellij.vcs.log.graph.api.EdgeFilter
import com.intellij.vcs.log.graph.api.LinearGraph
import com.intellij.vcs.log.graph.api.elements.GraphEdge
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType
import com.intellij.vcs.log.graph.api.elements.GraphElement
import com.intellij.vcs.log.graph.api.elements.GraphNode
import com.intellij.vcs.log.graph.api.printer.GraphPrintElement
import com.intellij.vcs.log.graph.api.printer.PrintElementGenerator
import com.intellij.vcs.log.graph.api.printer.PrintElementPresentationManager
import com.intellij.vcs.log.graph.impl.print.elements.EdgePrintElementImpl
import com.intellij.vcs.log.graph.impl.print.elements.SimplePrintElementImpl
import com.intellij.vcs.log.graph.impl.print.elements.TerminalEdgePrintElement
import com.intellij.vcs.log.graph.utils.LinearGraphUtils.*
import com.intellij.vcs.log.graph.utils.NormalEdge
import org.jetbrains.annotations.VisibleForTesting
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal class PrintElementGeneratorImpl @VisibleForTesting constructor(private val linearGraph: LinearGraph,
                                                                        private val presentationManager: PrintElementPresentationManager,
                                                                        private val elementComparator: Comparator<GraphElement>,
                                                                        private val longEdgeSize: Int,
                                                                        private val visiblePartSize: Int,
                                                                        private val edgeWithArrowSize: Int) : PrintElementGenerator {
  private val cache = SLRUMap<Int, List<GraphElement>>(CACHE_SIZE, CACHE_SIZE * 2)
  private val edgesInRowGenerator = EdgesInRowGenerator(linearGraph)

  val recommendedWidth by lazy { calculateRecommendedWidth() }

  constructor(graph: LinearGraph,
              presentationManager: PrintElementPresentationManager,
              showLongEdges: Boolean,
              elementComparator: Comparator<GraphElement>) :
    this(graph, presentationManager, elementComparator,
         if (showLongEdges) VERY_LONG_EDGE_SIZE else LONG_EDGE_SIZE,
         if (showLongEdges) VERY_LONG_EDGE_PART_SIZE else LONG_EDGE_PART_SIZE,
         if (showLongEdges) LONG_EDGE_SIZE else Integer.MAX_VALUE)

  private fun calculateRecommendedWidth(): Int {
    val nodesCount = linearGraph.nodesCount()
    if (nodesCount == 0) return 0
    if (nodesCount == 1) return 1

    val n = min(SAMPLE_SIZE, nodesCount)

    var sum = 0.0
    var sumSquares = 0.0
    var edgesCount = 0
    val currentNormalEdges = CollectionFactory.createSmallMemoryFootprintSet<NormalEdge>()

    for (i in 0 until n) {
      val adjacentEdges = linearGraph.getAdjacentEdges(i, EdgeFilter.ALL)
      var upArrows = 0
      var downArrows = 0
      for (e in adjacentEdges) {
        val normalEdge = asNormalEdge(e)
        if (normalEdge != null) {
          if (isEdgeUp(e, i)) {
            currentNormalEdges.remove(normalEdge)
          }
          else {
            currentNormalEdges.add(normalEdge)
          }
        }
        else {
          if (e.type == GraphEdgeType.DOTTED_ARROW_UP) {
            upArrows++
          }
          else {
            downArrows++
          }
        }
      }

      var newEdgesCount = 0
      for (e in currentNormalEdges) {
        if (isEdgeVisibleInRow(e, i)) {
          newEdgesCount++
        }
        else {
          val arrow = getArrowType(e, i)
          if (arrow === EdgePrintElement.Type.DOWN) {
            downArrows++
          }
          else if (arrow === EdgePrintElement.Type.UP) {
            upArrows++
          }
        }
      }

      /*
         * 0 <= K < 1; weight is an arithmetic progression, starting at 2 / ( n * (k + 1)) ending at k * 2 / ( n * (k + 1))
         * this formula ensures that sum of all weights is 1
         */
      val width = max(edgesCount + upArrows, newEdgesCount + downArrows)
      val weight = 2 / (n * (K + 1)) * (1 + (K - 1) * i / (n - 1))
      sum += width * weight
      sumSquares += width.toDouble() * width.toDouble() * weight

      edgesCount = newEdgesCount
    }

    /*
      weighted variance calculation described here:
      http://stackoverflow.com/questions/30383270/how-do-i-calculate-the-standard-deviation-between-weighted-measurements
       s*/
    val average = sum
    val deviation = sqrt(sumSquares - average * average)
    return Math.round(average + deviation).toInt()
  }

  override fun getPrintElements(rowIndex: VcsLogVisibleGraphIndex): Collection<GraphPrintElement> {
    val result = mutableListOf<GraphPrintElement>()
    val nodes = mutableListOf<GraphPrintElement>() // nodes at the end, to be drawn over the edges

    val visibleElements = getSortedVisibleElementsInRow(rowIndex)
    val upPosition = createEndPositionFunction(rowIndex - 1, true)
    val downPosition = createEndPositionFunction(rowIndex + 1, false)

    visibleElements.forEachIndexed { position, element ->
      when (element) {
        is GraphNode -> {
          val nodeIndex = element.nodeIndex
          nodes.add(SimplePrintElementImpl(rowIndex, position, element, presentationManager))
          linearGraph.getAdjacentEdges(nodeIndex, EdgeFilter.ALL).forEach { edge ->
            val arrowType = getArrowType(edge, rowIndex)
            val down = downPosition(edge)
            val up = upPosition(edge)
            if (down != null) {
              result.add(EdgePrintElementImpl(rowIndex, position, down, EdgePrintElement.Type.DOWN, edge, arrowType === EdgePrintElement.Type.DOWN, presentationManager))
            }
            if (up != null) {
              result.add(EdgePrintElementImpl(rowIndex, position, up, EdgePrintElement.Type.UP, edge, arrowType === EdgePrintElement.Type.UP, presentationManager))
            }
          }
        }
        is GraphEdge -> {
          val down = downPosition(element)
          val up = upPosition(element)
          val arrowType = getArrowType(element, rowIndex)

          if (down != null) {
            result.add(EdgePrintElementImpl(rowIndex, position, down, EdgePrintElement.Type.DOWN, element, arrowType === EdgePrintElement.Type.DOWN, presentationManager))
          }
          else if (arrowType === EdgePrintElement.Type.DOWN) {
            result.add(TerminalEdgePrintElement(rowIndex, position, EdgePrintElement.Type.DOWN, element, presentationManager))
          }
          if (up != null) {
            result.add(EdgePrintElementImpl(rowIndex, position, up, EdgePrintElement.Type.UP, element, arrowType === EdgePrintElement.Type.UP, presentationManager))
          }
          else if (arrowType === EdgePrintElement.Type.UP) {
            result.add(TerminalEdgePrintElement(rowIndex, position, EdgePrintElement.Type.UP, element, presentationManager))
          }
        }
      }
    }
    result.addAll(nodes)
    return result
  }

  private fun createEndPositionFunction(visibleRowIndex: VcsLogVisibleGraphIndex, up: Boolean): (GraphEdge) -> Int? {
    if (visibleRowIndex < 0 || visibleRowIndex >= linearGraph.nodesCount()) return { null }

    val visibleElementsInNextRow = getSortedVisibleElementsInRow(visibleRowIndex)

    val toPosition = HashMap<GraphElement, Int>(visibleElementsInNextRow.size)
    visibleElementsInNextRow.forEachIndexed { position, element -> toPosition[element] = position }

    return { edge ->
      toPosition[edge] ?: run {
        val nodeIndex = if (up) edge.upNodeIndex else edge.downNodeIndex
        if (nodeIndex != null) toPosition[linearGraph.getGraphNode(nodeIndex)]
        else null
      }
    }
  }

  private fun getArrowType(edge: GraphEdge, rowIndex: VcsLogVisibleGraphIndex): EdgePrintElement.Type? {
    val normalEdge = asNormalEdge(edge)
    if (normalEdge != null) {
      return getArrowType(normalEdge, rowIndex)
    }
    else { // special edges
      when (edge.type) {
        GraphEdgeType.DOTTED_ARROW_DOWN, GraphEdgeType.NOT_LOAD_COMMIT ->
          if (intEqual(edge.upNodeIndex, rowIndex - 1)) {
            return EdgePrintElement.Type.DOWN
          }
        GraphEdgeType.DOTTED_ARROW_UP ->
          // todo case 0-row arrow
          if (intEqual(edge.downNodeIndex, rowIndex + 1)) {
            return EdgePrintElement.Type.UP
          }
        else -> LOG.error("Unknown special edge type " + edge.type + " at row " + rowIndex)
      }
    }
    return null
  }

  private fun getArrowType(normalEdge: NormalEdge, rowIndex: VcsLogVisibleGraphIndex): EdgePrintElement.Type? {
    val edgeSize = normalEdge.down - normalEdge.up
    val upOffset = rowIndex - normalEdge.up
    val downOffset = normalEdge.down - rowIndex

    if (edgeSize >= longEdgeSize) {
      if (upOffset == visiblePartSize) {
        LOG.assertTrue(downOffset != visiblePartSize,
                       "Both up and down arrow at row $rowIndex") // this can not happen due to how constants are picked out, but just in case
        return EdgePrintElement.Type.DOWN
      }
      if (downOffset == visiblePartSize) return EdgePrintElement.Type.UP
    }
    if (edgeSize >= edgeWithArrowSize) {
      if (upOffset == 1) {
        LOG.assertTrue(downOffset != 1, "Both up and down arrow at row $rowIndex")
        return EdgePrintElement.Type.DOWN
      }
      if (downOffset == 1) return EdgePrintElement.Type.UP
    }
    return null
  }

  private fun isEdgeVisibleInRow(edge: GraphEdge, visibleRowIndex: VcsLogVisibleGraphIndex): Boolean {
    val normalEdge = asNormalEdge(edge) ?: return false // e.d. edge is special. See addSpecialEdges
    return isEdgeVisibleInRow(normalEdge, visibleRowIndex)
  }

  private fun isEdgeVisibleInRow(normalEdge: NormalEdge, visibleRowIndex: VcsLogVisibleGraphIndex): Boolean {
    return normalEdge.down - normalEdge.up < longEdgeSize || getAttachmentDistance(normalEdge, visibleRowIndex) <= visiblePartSize
  }

  private fun getSortedVisibleElementsInRow(rowIndex: VcsLogVisibleGraphIndex): List<GraphElement> {
    val graphElements = cache.get(rowIndex)
    if (graphElements != null) {
      return graphElements
    }

    val result = ArrayList<GraphElement>()
    result.add(linearGraph.getGraphNode(rowIndex))

    edgesInRowGenerator.getEdgesInRow(rowIndex).filterTo(result) { isEdgeVisibleInRow(it, rowIndex) }
    if (rowIndex > 0) {
      linearGraph.getAdjacentEdges(rowIndex - 1, EdgeFilter.SPECIAL)
        .filterTo(result) { isEdgeDown(it, rowIndex - 1) }
    }
    if (rowIndex < linearGraph.nodesCount() - 1) {
      linearGraph.getAdjacentEdges(rowIndex + 1, EdgeFilter.SPECIAL)
        .filterTo(result) { isEdgeUp(it, rowIndex + 1) }
    }

    Collections.sort(result, elementComparator)
    cache.put(rowIndex, result)
    return result
  }

  private fun getAttachmentDistance(e1: NormalEdge, rowIndex: VcsLogVisibleGraphIndex): Int {
    return min(rowIndex - e1.up, e1.down - rowIndex)
  }

  companion object {
    private val LOG = Logger.getInstance(PrintElementGeneratorImpl::class.java)

    private const val VERY_LONG_EDGE_SIZE = 1000
    const val LONG_EDGE_SIZE: Int = 30
    private const val VERY_LONG_EDGE_PART_SIZE = 250
    private const val LONG_EDGE_PART_SIZE = 1

    private const val CACHE_SIZE = 100
    private const val SAMPLE_SIZE = 20000
    private const val K = 0.1
  }
}
