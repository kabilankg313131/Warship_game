package com.example

import com.example.model.AIMode
import com.example.model.AIPlayer
import com.example.model.AttackResult
import com.example.model.Board
import com.example.model.CellState
import com.example.model.Coordinate
import com.example.model.ShipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ============================================================================
 * DSA TEST SUITE: Battleship Data Structures & Algorithms Verification
 * ============================================================================
 */
class BattleshipDsaTest {

    @Test
    fun testMatrixInitializationAndBounds() {
        val board = Board(10)
        assertEquals(10, board.size)

        // Verify all 100 cells initialized to EMPTY
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                assertEquals(CellState.EMPTY, board.getCell(r, c))
            }
        }

        assertTrue(board.isValidCoordinate(0, 0))
        assertTrue(board.isValidCoordinate(9, 9))
        assertFalse(board.isValidCoordinate(-1, 0))
        assertFalse(board.isValidCoordinate(0, 10))
    }

    @Test
    fun testShipPlacementAndOverlapValidation() {
        val board = Board(10)

        // Valid horizontal placement: Carrier at (0, 0)
        val carrier = board.placeShip(ShipType.CARRIER, Coordinate(0, 0), isVertical = false)
        assertNotNull(carrier)
        assertEquals(5, carrier?.length)
        assertEquals(1, board.getShipCount())

        // Verify matrix cells are marked SHIP
        for (c in 0 until 5) {
            assertEquals(CellState.SHIP, board.getCell(0, c))
        }

        // Test Overlap: attempting to place Battleship intersecting at (0, 2) must fail
        assertFalse(board.canPlaceShip(ShipType.BATTLESHIP, Coordinate(0, 2), isVertical = true))
        assertFalse(board.canPlaceShip(ShipType.BATTLESHIP, Coordinate(0, 2), isVertical = false))

        // Test Boundary Overflow: Carrier of length 5 at (0, 7) horizontal extends to col 11 (out of bounds)
        assertFalse(board.canPlaceShip(ShipType.CARRIER, Coordinate(0, 7), isVertical = false))
    }

    @Test
    fun testRecursiveBacktrackingFleetAutoPlacement() {
        val board = Board(10)
        val fleet = ShipType.standardFleet()

        // Execute Backtracking Placement
        val success = board.autoPlaceAllShips(fleet)
        assertTrue("Backtracking algorithm should successfully place all 5 ships", success)
        assertEquals(5, board.getShipCount())

        // Verify total ship cells occupied = 5 + 4 + 3 + 3 + 2 = 17
        var totalShipCells = 0
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                if (board.getCell(r, c) == CellState.SHIP) {
                    totalShipCells++
                }
            }
        }
        assertEquals(17, totalShipCells)
    }

    @Test
    fun testAttackResolutionHitMissSunk() {
        val board = Board(10)
        val destroyer = board.placeShip(ShipType.DESTROYER, Coordinate(2, 2), isVertical = false)
        assertNotNull(destroyer)

        // Attack open water (Miss)
        val missResult = board.receiveAttack(Coordinate(0, 0))
        assertTrue(missResult is AttackResult.Miss)
        assertEquals(CellState.MISS, board.getCell(0, 0))

        // Attack ship segment 1 (Hit)
        val hitResult = board.receiveAttack(Coordinate(2, 2))
        assertTrue(hitResult is AttackResult.Hit)
        assertEquals(CellState.HIT, board.getCell(2, 2))
        assertFalse(board.allShipsSunk())

        // Duplicate attack on already hit cell
        val dupeResult = board.receiveAttack(Coordinate(2, 2))
        assertTrue(dupeResult is AttackResult.AlreadyAttacked)

        // Attack ship segment 2 (Sunk)
        val sunkResult = board.receiveAttack(Coordinate(2, 3))
        assertTrue(sunkResult is AttackResult.Sunk)
        val sunk = sunkResult as AttackResult.Sunk
        assertTrue(sunk.ship.isSunk)
        assertTrue(sunk.isGameOver)
        assertTrue(board.allShipsSunk())
    }

    @Test
    fun testAiPlayerQueueTargetModeAndParityHunt() {
        val ai = AIPlayer(10)
        assertEquals(AIMode.HUNT, ai.currentMode)
        assertEquals(0, ai.targetQueueSize)

        // First hunt move should be chosen from Parity Matrix: (r + c) % 2 == 0
        val huntCoord = ai.decideNextMove()
        assertEquals(0, (huntCoord.row + huntCoord.col) % 2)

        // Simulate a HIT at (4, 4)
        val board = Board(10)
        val ship = board.placeShip(ShipType.DESTROYER, Coordinate(4, 4), isVertical = true)
        val hitResult = AttackResult.Hit(Coordinate(4, 4), ship!!)

        ai.processAttackResult(hitResult)

        // AI must switch to TARGET MODE and enqueue 4 cardinal neighbors
        assertEquals(AIMode.TARGET, ai.currentMode)
        assertEquals(4, ai.targetQueueSize)

        // Verify the next move pops a neighbor from the FIFO Queue
        val nextTarget = ai.decideNextMove()
        val expectedNeighbors = Coordinate(4, 4).getCardinalNeighbors(10)
        assertTrue(expectedNeighbors.contains(nextTarget))
        assertEquals(AIMode.TARGET, ai.currentMode)
        assertEquals(3, ai.targetQueueSize) // Queue decremented by 1 (FIFO pop)
    }
}
