package com.example.model

import java.io.Serializable

/**
 * ============================================================================
 * DSA CONCEPT: Object Modeling & Set-Based O(1) Health Tracking
 * ============================================================================
 * Custom class representing a discrete naval vessel placed on the board matrix.
 *
 * Data Structure Highlights:
 *  - coordinates: List<Coordinate> representing the contiguous 1D segment of cells
 *    occupied by this ship within the 2D grid.
 *  - hitCoordinates: HashSet<Coordinate> storing coordinates of struck segments.
 *    Using a Hash Set guarantees O(1) time complexity for lookup (contains)
 *    and insertion (add) operations during torpedo strike resolution.
 *
 * Health Metric:
 *  - health = length - hitCoordinates.size
 *  - isSunk is evaluated in O(1) time when hitCoordinates.size == length.
 */
class Ship(
    val type: ShipType,
    val coordinates: List<Coordinate>,
    val isVertical: Boolean
) : Serializable {

    val length: Int = type.length

    // Hash Set for O(1) hit recording and duplicate hit prevention
    private val hitCoordinates: HashSet<Coordinate> = HashSet()

    /**
     * Checks if this ship spans across the provided [coord].
     * Time Complexity: O(L) where L is ship length (L <= 5 -> effectively O(1)).
     */
    fun occupies(coord: Coordinate): Boolean {
        return coordinates.contains(coord)
    }

    /**
     * Records a hit if [coord] belongs to this ship and has not yet been hit.
     * Time Complexity: O(1) average lookup and insertion in HashSet.
     * @return true if this was a new, successful hit on an undamaged segment.
     */
    fun recordHit(coord: Coordinate): Boolean {
        if (occupies(coord)) {
            return hitCoordinates.add(coord)
        }
        return false
    }

    /**
     * Number of remaining undamaged segments.
     */
    val health: Int
        get() = length - hitCoordinates.size

    /**
     * Returns true when all segments of the ship have been hit.
     * Evaluated in O(1) time.
     */
    val isSunk: Boolean
        get() = hitCoordinates.size >= length

    /**
     * Returns set of coordinates that have suffered hits.
     */
    fun getHitCoordinates(): Set<Coordinate> = hitCoordinates.toSet()

    override fun toString(): String {
        return "${type.displayName} (Length: $length, HP: $health/$length, Sunk: $isSunk)"
    }
}
