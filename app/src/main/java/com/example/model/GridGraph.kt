package com.example.model

import java.io.Serializable
import java.util.ArrayDeque

/**
 * ============================================================================
 * DSA CONCEPT: Graph Data Structure (Undirected Grid Graph & Adjacency List)
 * ============================================================================
 * Models the N x N tactical sea grid as an Undirected Graph G = (V, E) where:
 *   - V (Vertices): All coordinates (row, col) within board boundaries.
 *   - E (Edges): Orthogonal connections between 4-cardinal neighbors (N, S, E, W).
 *
 * Graph Representation:
 *   - Adjacency List: Map<Coordinate, List<Coordinate>> (O(1) neighbor retrieval).
 *   - Space Complexity: |V| = N^2, |E| <= 4 * N^2 -> O(N^2) vertices and edges.
 *
 * Core Graph Algorithms Implemented:
 *   1. Connected Components (BFS / DFS):
 *      Partitions any subgraph of cells (e.g. all HIT cells, or all unattacked water cells)
 *      into connected components in O(V + E) linear graph traversal time.
 *
 *   2. Graph Island Pruning (Tactical AI):
 *      Checks if an unattacked water component can geometrically fit a vessel of
 *      length >= L. If not, the component is pruned from the AI search space.
 *
 *   3. Neighbor Vertex Discovery:
 *      Used by both AI and Sonar algorithms to traverse incident edges.
 */
class GridGraph(val size: Int = 10) : Serializable {

    // Adjacency List representation: maps every vertex Coordinate to its incident neighbor vertices
    private val adjacencyList: HashMap<Coordinate, List<Coordinate>> = HashMap()

    init {
        buildAdjacencyList()
    }

    private fun buildAdjacencyList() {
        val deltas = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1) // North, South, West, East

        for (r in 0 until size) {
            for (c in 0 until size) {
                val vertex = Coordinate(r, c)
                val neighbors = ArrayList<Coordinate>(4)

                for ((dr, dc) in deltas) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until size && nc in 0 until size) {
                        neighbors.add(Coordinate(nr, nc))
                    }
                }
                adjacencyList[vertex] = neighbors
            }
        }
    }

    /**
     * O(1) retrieval of incident adjacent vertices in the Graph.
     */
    fun getNeighbors(coord: Coordinate): List<Coordinate> {
        return adjacencyList[coord] ?: emptyList()
    }

    /**
     * Graph Connected Components Algorithm using Breadth-First Search (BFS).
     * Partitions the subgraph of vertices satisfying [predicate] into distinct connected components.
     *
     * Time Complexity: O(V + E) linear graph traversal.
     * Space Complexity: O(V) for visited set and BFS queue.
     *
     * @param predicate Filter specifying which vertices belong to the subgraph.
     * @return List of sets, each containing vertices forming a connected component.
     */
    fun findConnectedComponents(predicate: (Coordinate) -> Boolean): List<Set<Coordinate>> {
        val visited = HashSet<Coordinate>()
        val components = ArrayList<Set<Coordinate>>()

        for (r in 0 until size) {
            for (c in 0 until size) {
                val vertex = Coordinate(r, c)
                if (predicate(vertex) && !visited.contains(vertex)) {
                    // BFS exploration of a new connected component in the graph
                    val component = HashSet<Coordinate>()
                    val queue = ArrayDeque<Coordinate>()

                    queue.offer(vertex)
                    visited.add(vertex)

                    while (!queue.isEmpty()) {
                        val current = queue.poll() ?: continue
                        component.add(current)

                        for (neighbor in getNeighbors(current)) {
                            if (predicate(neighbor) && !visited.contains(neighbor)) {
                                visited.add(neighbor)
                                queue.offer(neighbor)
                            }
                        }
                    }
                    components.add(component)
                }
            }
        }
        return components
    }

    /**
     * Graph Geometry Verification:
     * Evaluates whether a connected water component contains at least one contiguous
     * straight-line path of length >= [minLength] (horizontal or vertical) that could
     * legally house an undiscovered warship.
     */
    fun canFitShip(component: Set<Coordinate>, minLength: Int): Boolean {
        if (component.size < minLength) return false

        for (coord in component) {
            // Check horizontal alignment
            var horizontalFit = true
            for (dc in 0 until minLength) {
                if (!component.contains(Coordinate(coord.row, coord.col + dc))) {
                    horizontalFit = false
                    break
                }
            }
            if (horizontalFit) return true

            // Check vertical alignment
            var verticalFit = true
            for (dr in 0 until minLength) {
                if (!component.contains(Coordinate(coord.row + dr, coord.col))) {
                    verticalFit = false
                    break
                }
            }
            if (verticalFit) return true
        }

        return false
    }

    /**
     * Total number of vertices in this grid graph: |V| = N^2.
     */
    val vertexCount: Int
        get() = size * size

    /**
     * Total number of undirected edges: |E| = 2 * N * (N - 1).
     */
    val edgeCount: Int
        get() = 2 * size * (size - 1)
}
