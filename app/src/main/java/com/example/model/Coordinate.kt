package com.example.model

import java.io.Serializable

/**
 * ============================================================================
 * DSA CONCEPT: 2D Spatial Coordinates & Graph Neighborhood Traversal
 * ============================================================================
 * Represents a discrete coordinate point (row, col) on the Battleship N x N matrix.
 *
 * Space Complexity: O(1) per instance.
 * Time Complexity:
 *  - Equals / HashCode: O(1), allows direct use in HashSets and HashMaps.
 *  - getNeighbors(): O(1) time generating the 4 cardinal adjacency list (North, South, East, West).
 *
 * Used extensively in the AI Queue-based Target Mode (Breadth-First Exploration).
 */
data class Coordinate(
    val row: Int,
    val col: Int
) : Serializable {

    /**
     * Checks if this coordinate falls strictly within the [gridSize] x [gridSize] bounds.
     * Prevents IndexOutOfBoundsException when evaluating edge cell neighbors.
     */
    fun isValid(gridSize: Int): Boolean {
        return row in 0 until gridSize && col in 0 until gridSize
    }

    /**
     * Computes the 4-connected cardinal neighbors (von Neumann neighborhood):
     *  - North: (row - 1, col)
     *  - South: (row + 1, col)
     *  - East:  (row, col + 1)
     *  - West:  (row, col - 1)
     *
     * DSA Note: In Target Mode, after hitting a ship, these adjacent coordinates
     * are pushed onto the AI FIFO Queue to systematically explore nearby cells.
     */
    fun getCardinalNeighbors(gridSize: Int): List<Coordinate> {
        val candidates = listOf(
            Coordinate(row - 1, col), // North
            Coordinate(row + 1, col), // South
            Coordinate(row, col + 1), // East
            Coordinate(row, col - 1)  // West
        )
        return candidates.filter { it.isValid(gridSize) }
    }

    /**
     * Formats into standard naval algebraic notation (e.g. A1, B5, J10).
     */
    fun toNavalNotation(): String {
        val rowLetter = ('A'.code + row).toChar()
        val colNumber = col + 1
        return "$rowLetter$colNumber"
    }

    override fun toString(): String = toNavalNotation()
}
