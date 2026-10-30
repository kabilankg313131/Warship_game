package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.model.Board
import com.example.model.CellState
import com.example.model.Coordinate
import com.example.model.GameMode
import com.example.model.GameSession
import com.example.model.Ship
import com.example.model.ShipType
import com.example.ui.GridCellView
import com.example.ui.ShipDockView

/**
 * ============================================================================
 * DSA WARSHIP: Fleet Deployment Phase with Distinct Warships
 * ============================================================================
 * Features:
 *  - 5 distinct visual warships with different sizes:
 *    Aircraft Carrier (5), Battleship (4), Cruiser (3), Submarine (3), Patrol Boat (2)
 *  - Interactive Ships Dock showing active and placed/ghosted silhouettes
 *  - Tap a placed ship on the grid to rotate it (with visual rotation indicators)
 *  - Prominent "RANDOM" auto-placement powered by Recursive Backtracking
 */
class DeploymentActivity : ComponentActivity() {

    private val gridSize: Int
        get() = GameSession.gridSize

    private val fleet: List<ShipType>
        get() = if (gridSize == 10) ShipType.standardFleet() else ShipType.compactFleet()

    private var activePlayerIndex: Int = 1
    private lateinit var currentBoard: Board

    private var selectedShipType: ShipType = ShipType.CARRIER
    private var isVertical: Boolean = false
    private var selectedShipOnGrid: Ship? = null

    // Views
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnRandom: Button
    private lateinit var btnUndoStack: Button
    private lateinit var dockShipsView: ShipDockView
    private lateinit var gridDeployment: GridLayout
    private lateinit var btnResetCircle: ImageButton
    private lateinit var btnRotateToggle: Button
    private lateinit var btnConfirmFleet: Button
    private lateinit var tvStatusHint: TextView

    // ========================================================================
    // DSA DATA STRUCTURE: Stack (LIFO) for Fleet Placement Undo
    // ========================================================================
    private val undoStack: java.util.ArrayDeque<com.example.model.BoardPlacementSnapshot> = java.util.ArrayDeque()

    // Grid Cell Views
    private lateinit var cellViews: Array<Array<GridCellView>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_deployment)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        activePlayerIndex = 1
        currentBoard = GameSession.player1Board
        selectedShipType = fleet.first()

        initViews()
        setupDock()
        setupGrid()
        refreshUi()
    }

    private fun pushUndoSnapshot() {
        undoStack.push(currentBoard.createPlacementSnapshot()) // LIFO Push O(1)
        updateUndoButtonState()
    }

    private fun updateUndoButtonState() {
        btnUndoStack.isEnabled = undoStack.isNotEmpty()
        btnUndoStack.text = if (undoStack.isEmpty()) "UNDO ↶ (0)" else "UNDO ↶ (${undoStack.size})"
        btnUndoStack.alpha = if (undoStack.isEmpty()) 0.45f else 1.0f
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tv_deploy_title)
        tvSubtitle = findViewById(R.id.tv_deploy_subtitle)
        btnRandom = findViewById(R.id.btn_random_placement)
        btnUndoStack = findViewById(R.id.btn_undo_stack)
        dockShipsView = findViewById(R.id.dock_ships_view)
        gridDeployment = findViewById(R.id.grid_deployment)
        btnResetCircle = findViewById(R.id.btn_reset_circle)
        btnRotateToggle = findViewById(R.id.btn_rotate_toggle)
        btnConfirmFleet = findViewById(R.id.btn_confirm_fleet)
        tvStatusHint = findViewById(R.id.tv_deploy_status_hint)

        btnRandom.setOnClickListener {
            pushUndoSnapshot()
            // Execute Recursive Backtracking algorithm
            val success = currentBoard.autoPlaceAllShips(fleet)
            if (success) {
                selectedShipOnGrid = null
                val nextUnplaced = getNextUnplacedShip()
                if (nextUnplaced != null) selectedShipType = nextUnplaced
                Toast.makeText(this, "Fleet auto-deployed via Backtracking Algorithm!", Toast.LENGTH_SHORT).show()
                refreshUi()
            } else {
                Toast.makeText(this, "Failed to auto-deploy fleet", Toast.LENGTH_SHORT).show()
            }
        }

        btnUndoStack.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                val previousSnapshot = undoStack.pop() // LIFO Pop O(1)
                currentBoard.restoreFromSnapshot(previousSnapshot)
                selectedShipOnGrid = null
                val nextUnplaced = getNextUnplacedShip()
                if (nextUnplaced != null) selectedShipType = nextUnplaced
                updateUndoButtonState()
                refreshUi()
                Toast.makeText(this, "Undid last action via LIFO Stack (${undoStack.size} left)", Toast.LENGTH_SHORT).show()
            }
        }

        btnResetCircle.setOnClickListener {
            pushUndoSnapshot()
            currentBoard.clear()
            selectedShipOnGrid = null
            selectedShipType = fleet.first()
            refreshUi()
        }

        btnRotateToggle.setOnClickListener {
            isVertical = !isVertical
            val orientationText = if (isVertical) "Orientation: Vertical ↻" else "Orientation: Horizontal ↻"
            btnRotateToggle.text = orientationText

            // If a ship on grid is selected, try rotating it directly
            val ship = selectedShipOnGrid
            if (ship != null) {
                rotateShipOnGrid(ship)
            } else {
                updateHint()
            }
        }

        btnConfirmFleet.setOnClickListener {
            handleConfirmFleet()
        }
    }

    private fun setupDock() {
        dockShipsView.setupDock(fleet)
        dockShipsView.setOnShipSelectedListener { ship ->
            val placedShip = currentBoard.getShips().firstOrNull { it.type == ship }
            if (placedShip != null) {
                // If ship is already deployed on the grid, clicking it in the dock rotates it!
                selectedShipOnGrid = placedShip
                selectedShipType = ship
                rotateShipOnGrid(placedShip)
            } else {
                // If ship is unplaced, clicking it again toggles its placement orientation
                if (selectedShipType == ship) {
                    isVertical = !isVertical
                    btnRotateToggle.text = if (isVertical) "Orientation: Vertical ↻" else "Orientation: Horizontal ↻"
                    Toast.makeText(this, "Orientation: ${if (isVertical) "Vertical" else "Horizontal"}", Toast.LENGTH_SHORT).show()
                }
                selectedShipType = ship
                selectedShipOnGrid = null
                refreshUi()
            }
        }
    }

    private fun setupGrid() {
        gridDeployment.removeAllViews()
        gridDeployment.rowCount = gridSize
        gridDeployment.columnCount = gridSize

        cellViews = Array(gridSize) { r ->
            Array(gridSize) { c ->
                val cell = GridCellView(this).apply {
                    val params = GridLayout.LayoutParams().apply {
                        rowSpec = GridLayout.spec(r, 1f)
                        columnSpec = GridLayout.spec(c, 1f)
                        width = 0
                        height = dpToPx(34)
                        setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1))
                    }
                    layoutParams = params

                    setOnClickListener {
                        handleCellClick(r, c)
                    }
                }
                gridDeployment.addView(cell)
                cell
            }
        }
    }

    private fun handleCellClick(row: Int, col: Int) {
        val coord = Coordinate(row, col)

        // 1. Check if a ship is already placed at this cell -> TAP TO ROTATE!
        val existingShipAtCell = currentBoard.getShips().firstOrNull { it.occupies(coord) }
        if (existingShipAtCell != null) {
            selectedShipOnGrid = existingShipAtCell
            selectedShipType = existingShipAtCell.type
            rotateShipOnGrid(existingShipAtCell)
            return
        }

        // 2. Cell is empty -> Place active ship
        val previousPlacement = currentBoard.getShips().firstOrNull { it.type == selectedShipType }
        if (previousPlacement != null) {
            currentBoard.removeShip(previousPlacement)
        }

        if (currentBoard.canPlaceShip(selectedShipType, coord, isVertical)) {
            pushUndoSnapshot()
            val newShip = currentBoard.placeShip(selectedShipType, coord, isVertical)
            selectedShipOnGrid = newShip

            // Auto-advance to next unplaced ship
            val nextUnplaced = getNextUnplacedShip()
            if (nextUnplaced != null) {
                selectedShipType = nextUnplaced
            }

            refreshUi()
        } else {
            // Restore previous placement if new position was invalid
            if (previousPlacement != null) {
                currentBoard.placeShip(previousPlacement.type, previousPlacement.coordinates.first(), previousPlacement.isVertical)
            }
            Toast.makeText(this, "Cannot place ${selectedShipType.displayName} here: boundary or collision conflict", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Taps a placed ship on the grid to rotate it (90 degrees).
     * Uses smart bounds clamping and local sliding window to ensure rotation succeeds smoothly.
     */
    private fun rotateShipOnGrid(ship: Ship, pivotCoord: Coordinate? = null) {
        val targetVertical = !ship.isVertical
        val length = ship.length

        // Temporarily remove to evaluate target orientation
        currentBoard.removeShip(ship)

        val candidateStarts = mutableListOf<Coordinate>()
        val originalStart = ship.coordinates.first()
        val pivot = pivotCoord ?: originalStart

        if (targetVertical) {
            // Horizontal -> Vertical
            val maxRow = (gridSize - length).coerceAtLeast(0)
            val idealRow = (pivot.row - (length / 2)).coerceIn(0, maxRow)
            candidateStarts.add(Coordinate(idealRow, pivot.col))
            candidateStarts.add(Coordinate(originalStart.row.coerceIn(0, maxRow), originalStart.col))
            for (r in 0..maxRow) {
                candidateStarts.add(Coordinate(r, pivot.col))
            }
        } else {
            // Vertical -> Horizontal
            val maxCol = (gridSize - length).coerceAtLeast(0)
            val idealCol = (pivot.col - (length / 2)).coerceIn(0, maxCol)
            candidateStarts.add(Coordinate(pivot.row, idealCol))
            candidateStarts.add(Coordinate(originalStart.row, originalStart.col.coerceIn(0, maxCol)))
            for (c in 0..maxCol) {
                candidateStarts.add(Coordinate(pivot.row, c))
            }
        }

        val validStart = candidateStarts.distinct().firstOrNull { candidate ->
            currentBoard.canPlaceShip(ship.type, candidate, targetVertical)
        }

        if (validStart != null) {
            pushUndoSnapshot()
            val rotatedShip = currentBoard.placeShip(ship.type, validStart, targetVertical)
            selectedShipOnGrid = rotatedShip
            isVertical = targetVertical
            btnRotateToggle.text = if (isVertical) "Orientation: Vertical ↻" else "Orientation: Horizontal ↻"
            Toast.makeText(this, "Rotated ${ship.type.displayName}!", Toast.LENGTH_SHORT).show()
            refreshUi()
        } else {
            // Revert back
            currentBoard.placeShip(ship.type, originalStart, ship.isVertical)
            Toast.makeText(this, "Cannot rotate ${ship.type.displayName}: blocked by edge or other ships", Toast.LENGTH_SHORT).show()
            refreshUi()
        }
    }

    private fun getNextUnplacedShip(): ShipType? {
        val placedTypes = currentBoard.getShips().map { it.type }.toSet()
        return fleet.firstOrNull { !placedTypes.contains(it) }
    }

    private fun refreshUi() {
        updateUndoButtonState()
        val placedShips = currentBoard.getShips()
        val placedTypes = placedShips.map { it.type }.toSet()

        // 1. Update Grid Cells
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val coord = Coordinate(r, c)
                val cell = cellViews[r][c]
                val ship = placedShips.firstOrNull { it.occupies(coord) }

                if (ship != null) {
                    val segIndex = ship.coordinates.indexOf(coord)
                    val isSelected = (ship == selectedShipOnGrid)
                    cell.setShipSegment(
                        type = ship.type,
                        index = segIndex,
                        length = ship.length,
                        vertical = ship.isVertical,
                        state = CellState.SHIP,
                        selected = isSelected
                    )
                } else {
                    cell.clearCell()
                }
            }
        }

        // 2. Update Dock
        dockShipsView.updateState(placedTypes, selectedShipType)

        // 3. Update Controls & Confirmation
        val placedCount = placedShips.size
        val totalCount = fleet.size
        val allPlaced = (placedCount == totalCount)

        btnConfirmFleet.isEnabled = allPlaced
        if (allPlaced) {
            btnConfirmFleet.setBackgroundResource(R.drawable.button_primary_bg)
            btnConfirmFleet.setTextColor(getColor(R.color.navy_950))
            tvStatusHint.text = "✓ All ${totalCount} warships deployed! Tap READY to begin battle."
            tvStatusHint.setTextColor(getColor(R.color.radar_green))
        } else {
            btnConfirmFleet.setBackgroundResource(R.drawable.button_secondary_bg)
            btnConfirmFleet.setTextColor(getColor(R.color.text_muted))
            updateHint()
        }
    }

    private fun updateHint() {
        val orientation = if (isVertical) "Vertical" else "Horizontal"
        tvStatusHint.text = "Selected: ${selectedShipType.displayName} (${selectedShipType.length} cells) | $orientation | Tap cell to place or tap ship to rotate"
        tvStatusHint.setTextColor(getColor(R.color.text_secondary))
    }

    private fun handleConfirmFleet() {
        if (GameSession.gameMode == GameMode.VS_AI) {
            // Player 1 ready. Auto-generate AI board with Backtracking Algorithm!
            GameSession.player2Board.autoPlaceAllShips(fleet)

            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 2-Player Pass & Play mode
            if (activePlayerIndex == 1) {
                activePlayerIndex = 2
                currentBoard = GameSession.player2Board
                undoStack.clear()
                selectedShipType = fleet.first()
                selectedShipOnGrid = null

                tvTitle.text = "Player 2: Deploy ships"
                tvSubtitle.text = "Pass device to Player 2 to deploy their fleet"

                Toast.makeText(this, "Player 1 fleet confirmed! Player 2, deploy your ships.", Toast.LENGTH_LONG).show()
                refreshUi()
            } else {
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
