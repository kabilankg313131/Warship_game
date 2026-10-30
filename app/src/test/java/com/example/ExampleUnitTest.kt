package com.example

import com.example.model.Board
import com.example.model.Coordinate
import com.example.model.GridGraph
import com.example.model.ShipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class ExampleUnitTest {

    @Test
    fun testGridGraph_propertiesAndNeighbors() {
        val graph = GridGraph(size = 8)
        // |V| = N^2 = 64
        assertEquals(64, graph.vertexCount)
        // |E| = 2 * N * (N - 1) = 2 * 8 * 7 = 112
        assertEquals(112, graph.edgeCount)

        // Corner cell (0, 0) has 2 incident edges in the Grid Graph
        val cornerNeighbors = graph.getNeighbors(Coordinate(0, 0))
        assertEquals(2, cornerNeighbors.size)
        assertTrue(cornerNeighbors.contains(Coordinate(0, 1)))
        assertTrue(cornerNeighbors.contains(Coordinate(1, 0)))

        // Center cell (3, 3) has 4 incident edges
        val centerNeighbors = graph.getNeighbors(Coordinate(3, 3))
        assertEquals(4, centerNeighbors.size)
    }

    @Test
    fun testGridGraph_connectedComponentsBFS() {
        val graph = GridGraph(size = 8)
        val hitCells = setOf(
            Coordinate(2, 2),
            Coordinate(2, 3),
            Coordinate(2, 4), // Component 1 (horizontal line of 3)
            Coordinate(5, 5),
            Coordinate(6, 5)  // Component 2 (vertical line of 2)
        )

        val components = graph.findConnectedComponents { hitCells.contains(it) }
        assertEquals(2, components.size)

        val compSizes = components.map { it.size }.sorted()
        assertEquals(2, compSizes[0])
        assertEquals(3, compSizes[1])
    }

    @Test
    fun testLIFOStack_undoPlacement() {
        val board = Board(size = 8)
        val stack = ArrayDeque<com.example.model.BoardPlacementSnapshot>()

        // Initial state: 0 ships
        assertEquals(0, board.getShips().size)

        // Snapshot 0 pushed to LIFO Stack
        stack.push(board.createPlacementSnapshot())

        // Action 1: Place Patrol Boat (DESTROYER)
        board.placeShip(ShipType.DESTROYER, Coordinate(0, 0), isVertical = false)
        assertEquals(1, board.getShips().size)
        stack.push(board.createPlacementSnapshot())

        // Action 2: Place Cruiser
        board.placeShip(ShipType.CRUISER, Coordinate(2, 0), isVertical = true)
        assertEquals(2, board.getShips().size)

        // Undo Action 2: Pop from LIFO Stack
        val snapshotBeforeCruiser = stack.pop()
        board.restoreFromSnapshot(snapshotBeforeCruiser)
        assertEquals(1, board.getShips().size)
        assertEquals(ShipType.DESTROYER, board.getShips().first().type)

        // Undo Action 1: Pop initial snapshot
        val initialSnapshot = stack.pop()
        board.restoreFromSnapshot(initialSnapshot)
        assertEquals(0, board.getShips().size)
    }

    @Test
    fun testFIFOQueue_salvoOrder() {
        val salvoQueue = ArrayDeque<Coordinate>()
        val c1 = Coordinate(1, 1)
        val c2 = Coordinate(2, 2)
        val c3 = Coordinate(3, 3)

        // FIFO Enqueue
        salvoQueue.offer(c1)
        salvoQueue.offer(c2)
        salvoQueue.offer(c3)

        assertEquals(3, salvoQueue.size)

        // FIFO Dequeue: First In, First Out
        assertEquals(c1, salvoQueue.poll())
        assertEquals(c2, salvoQueue.poll())
        assertEquals(c3, salvoQueue.poll())
        assertTrue(salvoQueue.isEmpty())
    }
}
