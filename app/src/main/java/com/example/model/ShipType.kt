package com.example.model

import java.io.Serializable

/**
 * ============================================================================
 * DSA CONCEPT: Heterogeneous Multi-Cell Vessels with Distinct Sizes
 * ============================================================================
 * Represents the 5 distinct warship classes with variable lengths (5, 4, 3, 3, 2):
 *  1. Aircraft Carrier: Length 5
 *  2. Battleship: Length 4
 *  3. Cruiser: Length 3
 *  4. Submarine: Length 3
 *  5. Patrol Boat / Destroyer: Length 2
 */
enum class ShipType(
    val displayName: String,
    val length: Int,
    val symbol: String
) : Serializable {
    CARRIER("Aircraft Carrier", 5, "⚓"),
    BATTLESHIP("Battleship", 4, "🚢"),
    CRUISER("Cruiser", 3, "🚤"),
    SUBMARINE("Submarine", 3, "🐬"),
    DESTROYER("Patrol Boat", 2, "⚡");

    companion object {
        /**
         * Standard 5-ship fleet with different sizes (5, 4, 3, 3, 2).
         */
        fun standardFleet(): List<ShipType> = listOf(
            CARRIER,
            BATTLESHIP,
            CRUISER,
            SUBMARINE,
            DESTROYER
        )

        /**
         * Compact 4-ship fleet for 8x8 boards.
         */
        fun compactFleet(): List<ShipType> = listOf(
            BATTLESHIP,
            CRUISER,
            SUBMARINE,
            DESTROYER
        )
    }
}
