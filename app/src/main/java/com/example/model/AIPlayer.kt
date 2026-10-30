package com.example.model

import java.io.Serializable
import java.util.ArrayDeque
import kotlin.random.Random

/**
 * AI Operating Mode
 */
enum class AIMode {
    HUNT,   // Searching across the board matrix using Checkerboard Parity heuristic
    TARGET  // Systematic Breadth-First exploration via FIFO Queue after a confirmed hit
}

/**
 * ============================================================================
 * DSA CONCEPT: Queue / Deque Data Structure for State-Machine AI Opponent
 * ============================================================================
 * Implements a tactical Battleship AI exhibiting two primary states:
 *
 * 1. HUNT MODE (Checkerboard Parity Heuristic):
 *    - Invariant: Every ship in the fleet has length L >= 2.
 *    - Theorem: Any segment of length >= 2 on a 2D grid MUST cross at least
 *      one cell where (row + col) % 2 == 0 (checkerboard parity).
 *    - By restricting initial hunt shots to parity cells, the search space
 *      is halved from N^2 to (N^2)/2 (e.g. 100 cells -> 50 cells).
 *    - Time Complexity per Hunt Decision: O(1) random selection from unattacked list.
 *
 * 2. TARGET MODE (FIFO Queue - Breadth-First Neighbor Search):
 *    - Data Structure: [ArrayDeque<Coordinate>] operated as a FIFO Queue.
 *    - Rationale: Once a torpedo connects (HIT), the ship's adjacent hull segments
 *      must reside in the 4 cardinal directions (North, South, East, West).
 *    - Algorithm:
 *      1. Enqueue valid, unattacked cardinal neighbors into the FIFO Queue.
 *      2. Dequeue the front element (poll) for the subsequent firing action.
 *      3. If subsequent shots yield hits along a line, prioritize collinear
 *         extension points.
 *      4. When the ship is declared SUNK, drain exhausted targets; if the Queue
 *         empties, transition back to HUNT MODE.
 *    - Time Complexity per Target Decision: O(1) dequeue operation.
 *
 * Space Complexity:
 *  - Queue size is at most 4 * (number of active hits) -> O(N).
 *  - HashSet of attacked coordinates: O(N^2) worst case for 100 cells.
 */
class AIPlayer(
    val gridSize: Int = 10,
    val name: String = "AI Tactical Commander"
) : Serializable {

    // ========================================================================
    // DSA DATA STRUCTURE: FIFO Queue for Target Mode
    // ========================================================================
    private val targetQueue: ArrayDeque<Coordinate> = ArrayDeque()

    // ========================================================================
    // DSA DATA STRUCTURE: Undirected Grid Graph for Topology & Adjacency
    // ========================================================================
    val gridGraph: GridGraph = GridGraph(gridSize)

    // O(1) membership lookup set of all coordinates fired upon
    private val attackedCoordinates: HashSet<Coordinate> = HashSet()

    // Cache of unvisited parity cells: (r + c) % 2 == 0
    private val parityHuntPool: MutableList<Coordinate> = mutableListOf()

    // Cache of remaining non-parity cells (fallback if all parity cells checked)
    private val secondaryHuntPool: MutableList<Coordinate> = mutableListOf()

    // Hit streak tracking for directional orientation
    private val activeHitStreak: MutableList<Coordinate> = mutableListOf()

    // AI Telemetry & Statistics
    var currentMode: AIMode = AIMode.HUNT
        private set
    var totalShotsFired: Int = 0
        private set
    var totalHits: Int = 0
        private set
    var totalMisses: Int = 0
        private set
    var lastFiredCoordinate: Coordinate? = null
        private set
    var lastAlgorithmAction: String = "Initialized AI in HUNT Mode (Parity Grid)"
        private set

    init {
        initializeHuntPools()
    }

    /**
     * Initializes the parity and secondary pools for the N x N grid.
     */
    private fun initializeHuntPools() {
        parityHuntPool.clear()
        secondaryHuntPool.clear()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val coord = Coordinate(r, c)
                if ((r + c) % 2 == 0) {
                    parityHuntPool.add(coord)
                } else {
                    secondaryHuntPool.add(coord)
                }
            }
        }
        // Shuffle pools for non-deterministic hunt sequences
        parityHuntPool.shuffle(Random.Default)
        secondaryHuntPool.shuffle(Random.Default)
    }

    /**
     * Selects the next target coordinate based on the current AI state.
     *
     * State Machine:
     *  - If targetQueue is NOT empty: Pop next candidate from FIFO Queue -> TARGET MODE.
     *  - If targetQueue IS empty: Sample from parityHuntPool -> HUNT MODE.
     *
     * @return The chosen [Coordinate] to attack.
     */
    fun decideNextMove(): Coordinate {
        // --------------------------------------------------------------------
        // BRANCH 1: TARGET MODE (Queue-driven BFS)
        // --------------------------------------------------------------------
        while (!targetQueue.isEmpty()) {
            currentMode = AIMode.TARGET
            val candidate = targetQueue.removeFirst() // FIFO Dequeue O(1)

            // Ensure candidate has not been attacked previously
            if (!attackedCoordinates.contains(candidate)) {
                lastFiredCoordinate = candidate
                lastAlgorithmAction = "TARGET MODE: Dequeued ${candidate.toNavalNotation()} from FIFO Queue (Queue size: ${targetQueue.size})"
                return candidate
            }
        }

        // --------------------------------------------------------------------
        // BRANCH 2: HUNT MODE (Checkerboard Parity Heuristic)
        // --------------------------------------------------------------------
        currentMode = AIMode.HUNT

        // Pick from unattacked parity cells first
        while (parityHuntPool.isNotEmpty()) {
            val candidate = parityHuntPool.removeAt(parityHuntPool.size - 1) // O(1) removal from end
            if (!attackedCoordinates.contains(candidate)) {
                lastFiredCoordinate = candidate
                lastAlgorithmAction = "HUNT MODE: Selected parity cell ${candidate.toNavalNotation()} (Parity Pool: ${parityHuntPool.size} left)"
                return candidate
            }
        }

        // Fallback: Pick from secondary unattacked cells if all parity cells exhausted
        while (secondaryHuntPool.isNotEmpty()) {
            val candidate = secondaryHuntPool.removeAt(secondaryHuntPool.size - 1)
            if (!attackedCoordinates.contains(candidate)) {
                lastFiredCoordinate = candidate
                lastAlgorithmAction = "HUNT MODE (Fallback): Selected secondary cell ${candidate.toNavalNotation()}"
                return candidate
            }
        }

        // Ultimate fail-safe (scan all cells)
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val coord = Coordinate(r, c)
                if (!attackedCoordinates.contains(coord)) {
                    lastFiredCoordinate = coord
                    lastAlgorithmAction = "HUNT MODE (Exhaustive Scan): Selected ${coord.toNavalNotation()}"
                    return coord
                }
            }
        }

        throw IllegalStateException("AI Error: No remaining valid coordinates on the board to attack!")
    }

    /**
     * Feedback cycle: Invoked after the attack is executed against the opponent's board.
     * Updates the AI state machine, statistics, and Queue targets.
     *
     * DSA Queue Operations:
     *  - On HIT: Enqueues cardinal neighbors (North, South, East, West) into [targetQueue].
     *  - On SUNK: Purges streak, checks if queue has remaining targets or returns to HUNT.
     */
    fun processAttackResult(result: AttackResult) {
        val coord = result.coordinate
        attackedCoordinates.add(coord)
        totalShotsFired++

        when (result) {
            is AttackResult.Hit -> {
                totalHits++
                activeHitStreak.add(coord)
                currentMode = AIMode.TARGET

                // Graph traversal: Query incident neighbor vertices in the Grid Graph
                val neighbors = gridGraph.getNeighbors(coord)
                var enqueuedCount = 0

                // If we have at least 2 collinear hits, prioritize inline neighbors
                val inlineNeighbors = getCollinearPriorityNeighbors(coord, neighbors)
                val orderedNeighbors = if (inlineNeighbors.isNotEmpty()) {
                    inlineNeighbors + (neighbors - inlineNeighbors.toSet())
                } else {
                    neighbors
                }

                for (neighbor in orderedNeighbors) {
                    if (!attackedCoordinates.contains(neighbor) && !targetQueue.contains(neighbor)) {
                        targetQueue.addLast(neighbor) // FIFO Enqueue O(1)
                        enqueuedCount++
                    }
                }

                lastAlgorithmAction = "HIT confirmed at ${coord.toNavalNotation()}! Enqueued $enqueuedCount incident Graph vertices to FIFO Queue (Queue size: ${targetQueue.size})."
            }

            is AttackResult.Sunk -> {
                totalHits++
                activeHitStreak.add(coord)

                // Ship is sunk! Clear hit streak for this ship
                activeHitStreak.clear()

                // Filter out any queued targets that are no longer sensible,
                // or keep them if they belong to an adjacent struck ship
                if (targetQueue.isEmpty()) {
                    currentMode = AIMode.HUNT
                    lastAlgorithmAction = "SUNK ${result.ship.type.displayName}! FIFO Queue empty -> Switched to HUNT Mode."
                } else {
                    currentMode = AIMode.TARGET
                    lastAlgorithmAction = "SUNK ${result.ship.type.displayName}! ${targetQueue.size} targets remaining in FIFO Queue."
                }
            }

            is AttackResult.Miss -> {
                totalMisses++
                if (targetQueue.isEmpty()) {
                    currentMode = AIMode.HUNT
                }
            }

            else -> { /* No state transition for duplicate/invalid */ }
        }
    }

    /**
     * Heuristic Optimization: If multiple hits are registered along an axis,
     * prioritize neighbors along the same line (horizontal vs vertical).
     */
    private fun getCollinearPriorityNeighbors(
        currentHit: Coordinate,
        neighbors: List<Coordinate>
    ): List<Coordinate> {
        if (activeHitStreak.size < 2) return emptyList()

        val previousHit = activeHitStreak[activeHitStreak.size - 2]
        val isVertical = previousHit.col == currentHit.col
        val isHorizontal = previousHit.row == currentHit.row

        return neighbors.filter { n ->
            if (isVertical) n.col == currentHit.col
            else if (isHorizontal) n.row == currentHit.row
            else false
        }
    }

    /**
     * Live Queue inspection for UI debugging and DSA demonstration.
     */
    fun getTargetQueueContents(): List<Coordinate> = targetQueue.toList()

    val targetQueueSize: Int
        get() = targetQueue.size

    val accuracyPercentage: Float
        get() = if (totalShotsFired == 0) 0f else (totalHits.toFloat() / totalShotsFired) * 100f
}
