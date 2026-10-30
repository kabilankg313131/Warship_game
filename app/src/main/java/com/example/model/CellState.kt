package com.example.model

import java.io.Serializable

/**
 * ============================================================================
 * DSA CONCEPT: Finite State Representation in 2D Matrix Cells
 * ============================================================================
 * Enumeration representing the discrete state of each cell in the board's
 * 2D Matrix (Array<Array<CellState>>).
 *
 * State Transitions:
 *  - EMPTY -> SHIP (during fleet deployment)
 *  - EMPTY -> MISS (upon water bombardment)
 *  - SHIP  -> HIT  (upon direct torpedo hit)
 *
 * In fog-of-war (opponent's view), SHIP cells are masked as EMPTY until struck.
 */
enum class CellState : Serializable {
    EMPTY, // Open water, untouched
    SHIP,  // Warship hull occupies this cell, unhit
    HIT,   // Torpedo hit confirmed on a warship section
    MISS   // Torpedo struck open water, missed
}
