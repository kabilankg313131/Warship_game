package com.example.model

import java.io.Serializable
import kotlin.random.Random

/**
 * ============================================================================
 * DSA CONCEPT: 2D Matrix (Adjacency / Spatial Grid) & Recursive Backtracking
 * ============================================================================
 * Represents an N x N Battleship game board backed by a 2D Array of [CellState].
 *
 * Core Data Structures:
 *  1. 2D Array / Matrix: Array<Array<CellState>>
 *     - Space Complexity: O(N^2) where N is grid dimension (e.g. 10x10 = 100 cells).
 *     - Direct random access: O(1) time complexity for reading/updating any cell:
 *       grid[row][col].
 *
 *  2. Linear Ship Registry: MutableList<Ship>
 *     - Stores all placed naval vessels.
 *     - Ship count S is small (typically S = 5), so linear scans are effectively O(1).
 *
 * Key Algorithms:
 *  1. Overlap & Boundary Validation: O(L) where L is ship length (L <= 5).
 *     Verifies spatial containment within the matrix boundaries and checks that
 *     every candidate cell is currently [CellState.EMPTY].
 *
 *  2. Randomized Recursive Backtracking (autoPlaceAllShips):
 *     Solves the exact fleet placement constraint satisfaction problem (CSP).
 *     For each ship in the list, candidates are generated and shuffled. If a placement
 *     leads to a deadlock for subsequent ships, the algorithm unwinds (backtracks),
 *     restores matrix cells to EMPTY, and tries alternative spatial placements.
 */
class Board(
    val size: Int = 10
) : Serializable {

    // 2D Matrix tracking discrete cell states
    private val grid: Array<Array<CellState>> = Array(size) {
        Array(size) { CellState.EMPTY }
    }

    // List of active ships deployed on this board
    private val ships: MutableList<Ship> = mutableListOf()

    // Counter for attacks received
    var totalAttacksReceived: Int = 0
        private set

    /**
     * O(1) matrix cell inspection.
     */
    fun getCell(row: Int, col: Int): CellState {
        require(isValidCoordinate(row, col)) { "Coordinate ($row, $col) is out of bounds for size $size" }
        return grid[row][col]
    }

    fun getCell(coord: Coordinate): CellState = getCell(coord.row, coord.col)

    fun isValidCoordinate(row: Int, col: Int): Boolean {
        return row in 0 until size && col in 0 until size
    }

    fun isValidCoordinate(coord: Coordinate): Boolean = isValidCoordinate(coord.row, coord.col)

    /**
     * Validates whether a ship of [shipType] can be placed starting at [startCoord]
     * with the given [isVertical] orientation.
     *
     * DSA Validation Steps:
     *  1. Boundary Check: Ensure (startRow + L <= size) or (startCol + L <= size).
     *  2. Collision Check: Ensure none of the traversed cells are occupied (grid != EMPTY).
     * Time Complexity: O(L) where L is ship length (L <= 5).
     */
    fun canPlaceShip(
        shipType: ShipType,
        startCoord: Coordinate,
        isVertical: Boolean
    ): Boolean {
        val length = shipType.length
        val endRow = if (isVertical) startCoord.row + length - 1 else startCoord.row
        val endCol = if (isVertical) startCoord.col else startCoord.col + length - 1

        // 1. Boundary Check
        if (!isValidCoordinate(startCoord.row, startCoord.col) || !isValidCoordinate(endRow, endCol)) {
            return false
        }

        // 2. Collision / Overlap Check across 2D Matrix
        for (i in 0 until length) {
            val r = if (isVertical) startCoord.row + i else startCoord.row
            val c = if (isVertical) startCoord.col else startCoord.col + i
            if (grid[r][c] != CellState.EMPTY) {
                return false // Collision detected
            }
        }
        return true
    }

    /**
     * Computes the list of coordinates for a ship candidate.
     */
    fun computeShipCoordinates(
        startCoord: Coordinate,
        length: Int,
        isVertical: Boolean
    ): List<Coordinate> {
        val list = ArrayList<Coordinate>(length)
        for (i in 0 until length) {
            val r = if (isVertical) startCoord.row + i else startCoord.row
            val c = if (isVertical) startCoord.col else startCoord.col + i
            list.add(Coordinate(r, c))
        }
        return list
    }

    /**
     * Places a ship onto the board matrix and records it in [ships].
     * Time Complexity: O(L) matrix update.
     * @return The created [Ship] if successful, or null if placement is invalid.
     */
    fun placeShip(
        shipType: ShipType,
        startCoord: Coordinate,
        isVertical: Boolean
    ): Ship? {
        if (!canPlaceShip(shipType, startCoord, isVertical)) {
            return null
        }

        val coords = computeShipCoordinates(startCoord, shipType.length, isVertical)
        val ship = Ship(shipType, coords, isVertical)

        // Commit placement to 2D Matrix
        for (coord in coords) {
            grid[coord.row][coord.col] = CellState.SHIP
        }
        ships.add(ship)
        return ship
    }

    /**
     * Unplaces a ship, reverting its cells in the matrix back to EMPTY.
     * Essential subroutine for Backtracking algorithm.
     */
    fun removeShip(ship: Ship): Boolean {
        if (!ships.remove(ship)) return false
        for (coord in ship.coordinates) {
            grid[coord.row][coord.col] = CellState.EMPTY
        }
        return true
    }

    /**
     * Clears all placed ships and resets matrix cells to EMPTY.
     */
    fun clear() {
        ships.clear()
        for (r in 0 until size) {
            for (c in 0 until size) {
                grid[r][c] = CellState.EMPTY
            }
        }
        totalAttacksReceived = 0
    }

    /**
     * ========================================================================
     * DSA ALGORITHM: Randomized Recursive Backtracking for Fleet Placement
     * ========================================================================
     * Places all ships in [shipTypes] onto the matrix automatically.
     *
     * How Backtracking Operates:
     *  - Function placeFleetRecursive(shipIndex):
     *    1. Base Case: If shipIndex == shipTypes.size, all ships placed -> return true.
     *    2. Permutation Generation: Create a list of all potential (row, col, orientation)
     *       configurations and randomly shuffle them for non-deterministic layout.
     *    3. Recursive Trial:
     *       - If a candidate position passes boundary and overlap validation:
     *         Place the ship on the matrix.
     *         Recurse to place the next ship (shipIndex + 1).
     *         If the recursive branch returns true, propagate success upward.
     *       - If the recursive branch returns false (deadlock reached):
     *         BACKTRACK! Undo the placement (reset matrix cells to EMPTY).
     *         Proceed to the next candidate configuration.
     *    4. Failure: If all candidate configurations are exhausted without success,
     *       return false.
     */
    fun autoPlaceAllShips(shipTypes: List<ShipType> = ShipType.standardFleet()): Boolean {
        clear()
        return backtrackPlace(shipTypes, 0)
    }

    private fun backtrackPlace(shipTypes: List<ShipType>, index: Int): Boolean {
        // Base case: all ships successfully positioned
        if (index >= shipTypes.size) {
            return true
        }

        val shipType = shipTypes[index]

        // Candidate state: (row, col, isVertical)
        data class Candidate(val coord: Coordinate, val isVertical: Boolean)

        val candidateList = mutableListOf<Candidate>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                candidateList.add(Candidate(Coordinate(r, c), isVertical = false))
                candidateList.add(Candidate(Coordinate(r, c), isVertical = true))
            }
        }
        // Shuffle candidate positions for varied random fleet layouts
        candidateList.shuffle(Random.Default)

        for (candidate in candidateList) {
            if (canPlaceShip(shipType, candidate.coord, candidate.isVertical)) {
                // Choose: Place ship
                val placedShip = placeShip(shipType, candidate.coord, candidate.isVertical)
                if (placedShip != null) {
                    // Explore: Recurse on remaining ships
                    if (backtrackPlace(shipTypes, index + 1)) {
                        return true
                    }
                    // Backtrack / Undo: Revert choice and explore alternative branches
                    removeShip(placedShip)
                }
            }
        }

        return false
    }

    /**
     * Resolves incoming torpedo attack against the board matrix.
     *
     * Time Complexity:
     *  - Coordinate boundary check: O(1)
     *  - State lookup in matrix: O(1)
     *  - Ship search & hit record: O(S) where S is ship count (S <= 5 -> O(1))
     *  - Fleet sunk check: O(S) -> O(1)
     */
    fun receiveAttack(coord: Coordinate): AttackResult {
        if (!isValidCoordinate(coord)) {
            return AttackResult.Invalid(coord)
        }

        val currentState = grid[coord.row][coord.col]

        // Check for redundant attack
        if (currentState == CellState.HIT || currentState == CellState.MISS) {
            return AttackResult.AlreadyAttacked(coord)
        }

        totalAttacksReceived++

        return when (currentState) {
            CellState.EMPTY -> {
                // Water splash - Miss
                grid[coord.row][coord.col] = CellState.MISS
                AttackResult.Miss(coord)
            }
            CellState.SHIP -> {
                // Direct strike - Hit
                grid[coord.row][coord.col] = CellState.HIT
                val hitShip = ships.firstOrNull { it.occupies(coord) }
                requireNotNull(hitShip) { "Integrity Error: Matrix marked SHIP at $coord but no Ship owns it!" }

                hitShip.recordHit(coord)

                if (hitShip.isSunk) {
                    val gameOver = allShipsSunk()
                    AttackResult.Sunk(coord, hitShip, isGameOver = gameOver)
                } else {
                    AttackResult.Hit(coord, hitShip)
                }
            }
            else -> AttackResult.AlreadyAttacked(coord)
        }
    }

    /**
     * Checks if all ships on the board have been completely sunk.
     * Evaluated in O(S) time (S <= 5).
     */
    fun allShipsSunk(): Boolean {
        return ships.isNotEmpty() && ships.all { it.isSunk }
    }

    fun getShips(): List<Ship> = ships.toList()

    fun getShipCount(): Int = ships.size

    /**
     * Produces a fog-of-war copy of the matrix for opponent display.
     * SHIP cells are masked as EMPTY, while HIT and MISS remain visible.
     */
    fun getMaskedMatrix(): Array<Array<CellState>> {
        return Array(size) { r ->
            Array(size) { c ->
                when (val state = grid[r][c]) {
                    CellState.SHIP -> CellState.EMPTY // Mask ship from enemy radar
                    else -> state
                }
            }
        }
    }

    /**
     * Returns the full unmasked matrix view (for defensive player fleet overview).
     */
    fun getUnmaskedMatrix(): Array<Array<CellState>> {
        return Array(size) { r ->
            Array(size) { c ->
                grid[r][c]
            }
        }
    }

    /**
     * ========================================================================
     * DSA FEATURE: Tactical Sonar Reconnaissance Scan (Sector Matrix Query)
     * ========================================================================
     * Performs an O(K) spatial matrix probe over the 3x3 subgrid centered at
     * [centerCoord].
     *
     * @return true if at least one unhit [CellState.SHIP] segment is detected
     * within the 3x3 sector; false if the sector contains no active warships.
     */
    fun sonarScan(centerCoord: Coordinate): Boolean {
        for (r in (centerCoord.row - 1)..(centerCoord.row + 1)) {
            for (c in (centerCoord.col - 1)..(centerCoord.col + 1)) {
                if (isValidCoordinate(r, c) && grid[r][c] == CellState.SHIP) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns all valid coordinates in the 3x3 sector for visual highlighting.
     */
    fun getSectorCoordinates(centerCoord: Coordinate): List<Coordinate> {
        val list = mutableListOf<Coordinate>()
        for (r in (centerCoord.row - 1)..(centerCoord.row + 1)) {
            for (c in (centerCoord.col - 1)..(centerCoord.col + 1)) {
                if (isValidCoordinate(r, c)) {
                    list.add(Coordinate(r, c))
                }
            }
        }
        return list
    }

    // ========================================================================
    // DSA CONCEPT: Grid Graph Analysis & Connected Components
    // ========================================================================
    val graph: GridGraph by lazy { GridGraph(size) }

    /**
     * Identifies connected clusters of HIT cells using Graph Connected Components (BFS).
     */
    fun getConnectedHitClusters(): List<Set<Coordinate>> {
        return graph.findConnectedComponents { coord ->
            getCell(coord) == CellState.HIT
        }
    }

    // ========================================================================
    // DSA CONCEPT: State Snapshot for LIFO Undo Stack (Command Pattern)
    // ========================================================================
    fun createPlacementSnapshot(): BoardPlacementSnapshot {
        val records = ships.map { ship ->
            BoardPlacementSnapshot.ShipRecord(
                type = ship.type,
                origin = ship.coordinates.first(),
                isVertical = ship.isVertical
            )
        }
        return BoardPlacementSnapshot(records)
    }

    fun restoreFromSnapshot(snapshot: BoardPlacementSnapshot) {
        clear()
        for (record in snapshot.shipRecords) {
            placeShip(record.type, record.origin, record.isVertical)
        }
    }
}

/**
 * Snapshot object pushed onto the LIFO Stack in DeploymentActivity.
 */
data class BoardPlacementSnapshot(
    val shipRecords: List<ShipRecord>
) : Serializable {
    data class ShipRecord(
        val type: ShipType,
        val origin: Coordinate,
        val isVertical: Boolean
    ) : Serializable
}

