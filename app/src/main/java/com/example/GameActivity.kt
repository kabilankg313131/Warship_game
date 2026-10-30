package com.example

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.model.AttackResult
import com.example.model.CellState
import com.example.model.Coordinate
import com.example.model.GameMode
import com.example.model.GameSession
import com.example.model.ShipType
import com.example.ui.BoardTheme
import com.example.ui.CannonView
import com.example.ui.CombatEffectOverlayView
import com.example.ui.GridCellView
import com.example.ui.ShipDockView

/**
 * ============================================================================
 * DSA WARSHIP: Battle Phase & Combat Artillery Engine
 * ============================================================================
 * Features:
 *  - Interactive animated Naval Artillery Cannon aiming and firing projectiles
 *  - Ballistic cannonball projectile with crosshair target reticle and impact flash
 *  - "if the ship was founded fully make the ship dead like black": Sunk ships
 *    reveal their complete charred dead-black silhouettes on the enemy radar
 *  - "if not found make it as x": Misses render as bold rounded 'X' marks
 *  - Middle fleet arsenal tracking alive vs sunk ships
 *  - Corner score badges tracking remaining segments to find ("9") and health ("17")
 */
class GameActivity : ComponentActivity() {

    private val gridSize: Int
        get() = GameSession.gridSize

    private val fleet: List<ShipType>
        get() = if (gridSize == 10) ShipType.standardFleet() else ShipType.compactFleet()

    private val totalShipSegments: Int
        get() = fleet.sumOf { it.length }

    private var isPlayer1Turn: Boolean = true
    private var isFiringLocked: Boolean = false
    private var turnCounter: Int = 1

    private var p1Hits: Int = 0
    private var p1Shots: Int = 0
    private var p2Hits: Int = 0
    private var p2Shots: Int = 0

    // Views
    private lateinit var tvEnemyRemainingBadge: TextView
    private lateinit var tvPlayerRemainingBadge: TextView
    private lateinit var tvTurnTitle: TextView
    private lateinit var tvTurnCounter: TextView
    private lateinit var tvTurnMessage: TextView

    private lateinit var tvOpponentBoardLabel: TextView
    private lateinit var gridOpponentFiring: GridLayout
    private lateinit var layoutOpponentColLabels: LinearLayout
    private lateinit var layoutOpponentRowLabels: LinearLayout

    private lateinit var dockCombatFleet: ShipDockView

    private lateinit var tvPlayerBoardLabel: TextView
    private lateinit var gridPlayerDefense: GridLayout
    private lateinit var layoutDefenseColLabels: LinearLayout
    private lateinit var layoutDefenseRowLabels: LinearLayout

    private lateinit var cannonView: CannonView
    private lateinit var combatOverlay: CombatEffectOverlayView
    private lateinit var btnGameCancel: ImageButton
    private lateinit var btnSalvoQueue: Button
    private lateinit var btnGraphScan: Button

    // ========================================================================
    // DSA DATA STRUCTURE: Player Artillery Salvo FIFO Queue
    // ========================================================================
    private val playerSalvoQueue: java.util.ArrayDeque<Coordinate> = java.util.ArrayDeque()
    private var isSalvoMode: Boolean = false

    private lateinit var tvBattleLog: TextView

    private lateinit var layoutPassCurtain: LinearLayout
    private lateinit var tvCurtainTitle: TextView
    private lateinit var btnRevealTurn: Button

    private lateinit var layoutGameOver: LinearLayout
    private lateinit var tvGameOverTitle: TextView
    private lateinit var tvGameOverWinner: TextView
    private lateinit var tvGameOverStats: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnReturnMainMenu: Button

    // Cell View Matrices (Both using custom GridCellView for X marks & Dead Black ships)
    private lateinit var opponentCellViews: Array<Array<GridCellView>>
    private lateinit var defenseCellViews: Array<Array<GridCellView>>

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupCoordinateHeaders()
        buildDualGridBoards()
        refreshAllBoardsUi()
        updateTurnDisplay()
    }

    private fun initViews() {
        tvEnemyRemainingBadge = findViewById(R.id.tv_enemy_remaining_badge)
        tvPlayerRemainingBadge = findViewById(R.id.tv_player_remaining_badge)
        tvTurnTitle = findViewById(R.id.tv_active_turn_title)
        tvTurnCounter = findViewById(R.id.tv_turn_counter)
        tvTurnMessage = findViewById(R.id.tv_turn_status_message)

        tvOpponentBoardLabel = findViewById(R.id.tv_opponent_board_label)
        gridOpponentFiring = findViewById(R.id.grid_opponent_firing)
        layoutOpponentColLabels = findViewById(R.id.layout_opponent_col_labels)
        layoutOpponentRowLabels = findViewById(R.id.layout_opponent_row_labels)

        dockCombatFleet = findViewById(R.id.dock_combat_fleet)
        dockCombatFleet.setupDock(fleet)

        tvPlayerBoardLabel = findViewById(R.id.tv_player_board_label)
        gridPlayerDefense = findViewById(R.id.grid_player_defense)
        layoutDefenseColLabels = findViewById(R.id.layout_defense_col_labels)
        layoutDefenseRowLabels = findViewById(R.id.layout_defense_row_labels)

        cannonView = findViewById(R.id.cannon_view)
        combatOverlay = findViewById(R.id.overlay_combat_effects)
        btnGameCancel = findViewById(R.id.btn_game_cancel)

        tvBattleLog = findViewById(R.id.tv_battle_log)

        layoutPassCurtain = findViewById(R.id.layout_pass_curtain)
        tvCurtainTitle = findViewById(R.id.tv_curtain_title)
        btnRevealTurn = findViewById(R.id.btn_reveal_turn)

        layoutGameOver = findViewById(R.id.layout_game_over)
        tvGameOverTitle = findViewById(R.id.tv_game_over_title)
        tvGameOverWinner = findViewById(R.id.tv_game_over_winner)
        tvGameOverStats = findViewById(R.id.tv_game_over_stats)
        btnPlayAgain = findViewById(R.id.btn_play_again)
        btnReturnMainMenu = findViewById(R.id.btn_return_main_menu)

        btnSalvoQueue = findViewById(R.id.btn_salvo_queue)
        btnGraphScan = findViewById(R.id.btn_graph_scan)

        btnSalvoQueue.setOnClickListener {
            if (isFiringLocked) return@setOnClickListener
            if (!isSalvoMode) {
                isSalvoMode = true
                playerSalvoQueue.clear()
                btnSalvoQueue.text = "🎯 QUEUE (0/3): Tap 3 Cells"
                btnSalvoQueue.setTextColor(getColor(R.color.hit_orange))
                Toast.makeText(this, "Salvo Queue Mode: Tap up to 3 coordinates to queue them (FIFO)", Toast.LENGTH_SHORT).show()
            } else {
                if (playerSalvoQueue.isNotEmpty()) {
                    executePlayerSalvoQueue()
                } else {
                    isSalvoMode = false
                    btnSalvoQueue.text = "🎯 SALVO QUEUE (FIFO)"
                    btnSalvoQueue.setTextColor(getColor(R.color.cyan_accent))
                }
            }
        }

        btnGraphScan.setOnClickListener {
            val targetBoard = if (isPlayer1Turn) GameSession.player2Board else GameSession.player1Board
            val hitClusters = targetBoard.getConnectedHitClusters()
            val totalHitVertices = hitClusters.sumOf { it.size }
            val graph = targetBoard.graph

            val info = "Graph BFS Analysis: ${hitClusters.size} connected hit cluster(s) (${totalHitVertices} hit cells). Graph |V|=${graph.vertexCount}, |E|=${graph.edgeCount}."
            Toast.makeText(this, info, Toast.LENGTH_LONG).show()
            appendBattleLog("☍ $info")
        }

        btnGameCancel.setOnClickListener {
            finish()
        }

        btnRevealTurn.setOnClickListener {
            layoutPassCurtain.visibility = View.GONE
            isFiringLocked = false
            refreshAllBoardsUi()
            updateTurnDisplay()
        }

        btnPlayAgain.setOnClickListener {
            GameSession.resetMatch(GameSession.gameMode, GameSession.gridSize)
            val intent = Intent(this, DeploymentActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnReturnMainMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun setupCoordinateHeaders() {
        populateHeaders(layoutOpponentColLabels, layoutOpponentRowLabels)
        populateHeaders(layoutDefenseColLabels, layoutDefenseRowLabels)
    }

    private fun populateHeaders(colLayout: LinearLayout, rowLayout: LinearLayout) {
        colLayout.removeAllViews()
        rowLayout.removeAllViews()

        val cornerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(16))
        }
        colLayout.addView(cornerSpacer)

        for (c in 0 until gridSize) {
            val tvCol = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(16), 1f)
                text = "${c + 1}"
                textSize = 9f
                setTextColor(getColor(R.color.text_subtitle))
                gravity = Gravity.CENTER
            }
            colLayout.addView(tvCol)
        }

        for (r in 0 until gridSize) {
            val tvRow = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(18), 0, 1f)
                text = "${('A'.code + r).toChar()}"
                textSize = 9f
                setTextColor(getColor(R.color.text_subtitle))
                gravity = Gravity.CENTER
            }
            rowLayout.addView(tvRow)
        }
    }

    private fun buildDualGridBoards() {
        // 1. Build Opponent Firing Grid (Blue Theme with X marks and dead black ships)
        gridOpponentFiring.removeAllViews()
        gridOpponentFiring.rowCount = gridSize
        gridOpponentFiring.columnCount = gridSize

        opponentCellViews = Array(gridSize) { r ->
            Array(gridSize) { c ->
                val cell = GridCellView(this).apply {
                    boardTheme = BoardTheme.OCEAN_BLUE
                    val params = GridLayout.LayoutParams().apply {
                        rowSpec = GridLayout.spec(r, 1f)
                        columnSpec = GridLayout.spec(c, 1f)
                        width = 0
                        height = dpToPx(32)
                        setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1))
                    }
                    layoutParams = params

                    setOnClickListener {
                        handlePlayerTorpedoFire(r, c, this)
                    }
                }
                gridOpponentFiring.addView(cell)
                cell
            }
        }

        // 2. Build Defense Fleet Grid (Coral Theme)
        gridPlayerDefense.removeAllViews()
        gridPlayerDefense.rowCount = gridSize
        gridPlayerDefense.columnCount = gridSize

        defenseCellViews = Array(gridSize) { r ->
            Array(gridSize) { c ->
                val cell = GridCellView(this).apply {
                    boardTheme = BoardTheme.CORAL_PEACH
                    val params = GridLayout.LayoutParams().apply {
                        rowSpec = GridLayout.spec(r, 1f)
                        columnSpec = GridLayout.spec(c, 1f)
                        width = 0
                        height = dpToPx(24) // Compact defense overview
                        setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1))
                    }
                    layoutParams = params
                }
                gridPlayerDefense.addView(cell)
                cell
            }
        }
    }

    /**
     * Handles player tapping a cell to fire the Cannon!
     */
    private fun handlePlayerTorpedoFire(row: Int, col: Int, targetCellView: GridCellView) {
        if (isFiringLocked) return

        val coord = Coordinate(row, col)
        val targetBoard = if (isPlayer1Turn) GameSession.player2Board else GameSession.player1Board

        // Check if cell was already targeted
        val currentState = targetBoard.getCell(coord)
        if (currentState == CellState.HIT || currentState == CellState.MISS) {
            Toast.makeText(this, "Coordinate ${coord.toNavalNotation()} already targeted!", Toast.LENGTH_SHORT).show()
            return
        }

        // ====================================================================
        // DSA QUEUE FEATURE: Enqueue targets into player's Salvo Queue (FIFO)
        // ====================================================================
        if (isSalvoMode) {
            if (playerSalvoQueue.contains(coord)) {
                playerSalvoQueue.remove(coord)
                targetCellView.setSelectedState(false)
                btnSalvoQueue.text = "🎯 QUEUE (${playerSalvoQueue.size}/3): Tap cells"
                return
            }
            if (playerSalvoQueue.size < 3) {
                playerSalvoQueue.offer(coord) // FIFO Enqueue O(1)
                targetCellView.setSelectedState(true)
                btnSalvoQueue.text = "🎯 FIRE SALVO (${playerSalvoQueue.size}/3) ➔"
                Toast.makeText(this, "Queued #${playerSalvoQueue.size}: ${coord.toNavalNotation()} in FIFO Queue", Toast.LENGTH_SHORT).show()
                if (playerSalvoQueue.size == 3) {
                    btnSalvoQueue.setTextColor(getColor(R.color.radar_green))
                }
            } else {
                Toast.makeText(this, "Queue full (3 max). Tap 'FIRE SALVO' button to launch!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        isFiringLocked = true

        // 1. Calculate Target Screen Coordinates
        val loc = IntArray(2)
        targetCellView.getLocationOnScreen(loc)
        val targetX = loc[0] + targetCellView.width / 2f
        val targetY = loc[1] + targetCellView.height / 2f

        // 2. Aim and Fire the Naval Cannon!
        cannonView.aimAndFire(targetX, targetY) { muzzleX, muzzleY ->
            // Launch Cannonball through the air!
            combatOverlay.launchCannonball(muzzleX, muzzleY, targetX, targetY) {
                // Cannonball Impact: Resolve Attack on 2D Matrix!
                resolveShotImpact(row, col, coord, targetBoard)
            }
        }
    }

    /**
     * Executes shots buffered in the player's Salvo FIFO Queue sequentially.
     */
    private fun executePlayerSalvoQueue() {
        if (playerSalvoQueue.isEmpty() || isFiringLocked) return
        isFiringLocked = true
        btnSalvoQueue.isEnabled = false

        fun fireNextFromQueue() {
            if (playerSalvoQueue.isEmpty()) {
                isFiringLocked = false
                isSalvoMode = false
                btnSalvoQueue.isEnabled = true
                btnSalvoQueue.text = "🎯 SALVO QUEUE (FIFO)"
                btnSalvoQueue.setTextColor(getColor(R.color.cyan_accent))

                val targetBoard = if (isPlayer1Turn) GameSession.player2Board else GameSession.player1Board
                if (!targetBoard.allShipsSunk()) {
                    if (GameSession.gameMode == GameMode.VS_AI) {
                        tvTurnTitle.text = "AI COMMANDER IS AIMING ARTILLERY..."
                        tvTurnTitle.setTextColor(getColor(R.color.dsa_purple))
                        mainHandler.postDelayed({ executeAiTurn() }, 650)
                    } else {
                        isPlayer1Turn = !isPlayer1Turn
                        turnCounter++
                        showPassAndPlayCurtain()
                    }
                }
                return
            }

            // Dequeue candidate in FIFO order!
            val coord = playerSalvoQueue.poll() ?: return
            val targetBoard = if (isPlayer1Turn) GameSession.player2Board else GameSession.player1Board
            val targetCellView = opponentCellViews[coord.row][coord.col]
            targetCellView.setSelectedState(false)

            val loc = IntArray(2)
            targetCellView.getLocationOnScreen(loc)
            val targetX = loc[0] + targetCellView.width / 2f
            val targetY = loc[1] + targetCellView.height / 2f

            cannonView.aimAndFire(targetX, targetY) { muzzleX, muzzleY ->
                combatOverlay.launchCannonball(muzzleX, muzzleY, targetX, targetY) {
                    if (isPlayer1Turn) p1Shots++ else p2Shots++
                    val result = targetBoard.receiveAttack(coord)
                    when (result) {
                        is AttackResult.Hit -> {
                            if (isPlayer1Turn) p1Hits++ else p2Hits++
                            tvTurnMessage.text = "💥 FIFO Salvo HIT at ${coord.toNavalNotation()}!"
                            appendBattleLog("FIFO Salvo struck enemy vessel at ${coord.toNavalNotation()}!")
                        }
                        is AttackResult.Sunk -> {
                            if (isPlayer1Turn) p1Hits++ else p2Hits++
                            tvTurnMessage.text = "🔥 FIFO Salvo SUNK ${result.ship.type.displayName}!"
                            appendBattleLog("FIFO Salvo DESTROYED enemy ${result.ship.type.displayName}!")
                            if (result.isGameOver) {
                                refreshAllBoardsUi()
                                handleGameOver(if (isPlayer1Turn) GameSession.player1Name else GameSession.player2Name)
                                return@launchCannonball
                            }
                        }
                        is AttackResult.Miss -> {
                            tvTurnMessage.text = "✕ FIFO Salvo MISS at ${coord.toNavalNotation()}."
                            appendBattleLog("FIFO Salvo missed at ${coord.toNavalNotation()}.")
                        }
                        else -> {}
                    }
                    refreshAllBoardsUi()
                    mainHandler.postDelayed({ fireNextFromQueue() }, 450)
                }
            }
        }

        fireNextFromQueue()
    }

    private fun resolveShotImpact(row: Int, col: Int, coord: Coordinate, targetBoard: com.example.model.Board) {
        if (isPlayer1Turn) p1Shots++ else p2Shots++
        val result = targetBoard.receiveAttack(coord)
        val activeShooterName = if (isPlayer1Turn) GameSession.player1Name else GameSession.player2Name
        val opponentName = if (isPlayer1Turn) GameSession.player2Name else GameSession.player1Name

        when (result) {
            is AttackResult.Hit -> {
                if (isPlayer1Turn) p1Hits++ else p2Hits++
                tvTurnMessage.text = "💥 DIRECT HIT at ${coord.toNavalNotation()}!"
                tvTurnMessage.setTextColor(getColor(R.color.hit_red))
                appendBattleLog("$activeShooterName struck enemy vessel at ${coord.toNavalNotation()}!")
            }
            is AttackResult.Sunk -> {
                if (isPlayer1Turn) p1Hits++ else p2Hits++
                tvTurnMessage.text = "🔥 ${result.ship.type.displayName} SUNK (DEAD BLACK)!"
                tvTurnMessage.setTextColor(getColor(R.color.hit_orange))
                appendBattleLog("$activeShooterName DESTROYED $opponentName's ${result.ship.type.displayName}!")

                if (result.isGameOver) {
                    refreshAllBoardsUi()
                    handleGameOver(winnerName = activeShooterName)
                    return
                }
            }
            is AttackResult.Miss -> {
                tvTurnMessage.text = "✕ MISS at ${coord.toNavalNotation()}."
                tvTurnMessage.setTextColor(getColor(R.color.cyan_accent))
                appendBattleLog("$activeShooterName fired at ${coord.toNavalNotation()} (Miss).")
            }
            else -> { return }
        }

        refreshAllBoardsUi()

        // Turn Management
        if (GameSession.gameMode == GameMode.VS_AI) {
            tvTurnTitle.text = "AI COMMANDER IS AIMING ARTILLERY..."
            tvTurnTitle.setTextColor(getColor(R.color.dsa_purple))

            mainHandler.postDelayed({
                executeAiTurn()
            }, 650)
        } else {
            // 2-Player Pass & Play
            if (!targetBoard.allShipsSunk()) {
                isPlayer1Turn = !isPlayer1Turn
                turnCounter++
                showPassAndPlayCurtain()
            } else {
                isFiringLocked = false
            }
        }
    }

    /**
     * Executes AI turn with artillery ballistics against the player's defense board.
     */
    private fun executeAiTurn() {
        val ai = GameSession.aiPlayer ?: return
        p2Shots++

        // 1. AI Decision Cycle
        val aiTargetCoord = ai.decideNextMove()

        // 2. Animate incoming strike onto player defense grid
        val defCell = defenseCellViews[aiTargetCoord.row][aiTargetCoord.col]
        val loc = IntArray(2)
        defCell.getLocationOnScreen(loc)
        val targetX = loc[0] + defCell.width / 2f
        val targetY = loc[1] + defCell.height / 2f

        combatOverlay.launchCannonball(targetX - dpToPx(60), targetY - dpToPx(140), targetX, targetY) {
            // Resolve strike against Player 1's 2D Matrix
            val attackResult = GameSession.player1Board.receiveAttack(aiTargetCoord)
            ai.processAttackResult(attackResult)

            when (attackResult) {
                is AttackResult.Hit -> {
                    p2Hits++
                    appendBattleLog("AI struck your ${attackResult.ship.type.displayName} at ${aiTargetCoord.toNavalNotation()}!")
                }
                is AttackResult.Sunk -> {
                    p2Hits++
                    appendBattleLog("AI SUNK your ${attackResult.ship.type.displayName} (DEAD BLACK)!")
                    if (attackResult.isGameOver) {
                        refreshAllBoardsUi()
                        handleGameOver(winnerName = ai.name)
                        return@launchCannonball
                    }
                }
                is AttackResult.Miss -> {
                    appendBattleLog("AI fired at ${aiTargetCoord.toNavalNotation()} (Miss).")
                }
                else -> {}
            }

            isFiringLocked = false
            turnCounter++
            refreshAllBoardsUi()
            updateTurnDisplay()
        }
    }

    private fun refreshAllBoardsUi() {
        val activeTargetBoard = if (isPlayer1Turn) GameSession.player2Board else GameSession.player1Board
        val activeDefenseBoard = if (isPlayer1Turn) GameSession.player1Board else GameSession.player2Board

        // 1. Update Opponent Radar (Masked Firing Grid)
        // "if not found make it as x" & "if the ship was founded fully make the ship dead like black"
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val coord = Coordinate(r, c)
                val state = activeTargetBoard.getCell(r, c)
                val cell = opponentCellViews[r][c]
                val ship = activeTargetBoard.getShips().firstOrNull { it.occupies(coord) }

                when (state) {
                    CellState.HIT -> {
                        if (ship != null && ship.isSunk) {
                            // "if the ship was founded fully make the ship dead like black"
                            val segIndex = ship.coordinates.indexOf(coord)
                            cell.setShipSegment(
                                type = ship.type,
                                index = segIndex,
                                length = ship.length,
                                vertical = ship.isVertical,
                                state = CellState.HIT,
                                selected = false,
                                sunk = true // Renders in DEAD BLACK!
                            )
                        } else {
                            // Hit on unsunk ship (fog of war hit spark)
                            cell.shipType = null
                            cell.cellState = CellState.HIT
                            cell.isSunk = false
                            cell.invalidate()
                        }
                    }
                    CellState.MISS -> {
                        // "if not found make it as x"
                        cell.shipType = null
                        cell.cellState = CellState.MISS
                        cell.isSunk = false
                        cell.invalidate()
                    }
                    else -> {
                        cell.clearCell()
                    }
                }
            }
        }

        // 2. Update Defense Fleet Grid
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val coord = Coordinate(r, c)
                val state = activeDefenseBoard.getCell(r, c)
                val cell = defenseCellViews[r][c]
                val ship = activeDefenseBoard.getShips().firstOrNull { it.occupies(coord) }

                if (ship != null) {
                    val segIndex = ship.coordinates.indexOf(coord)
                    cell.setShipSegment(
                        type = ship.type,
                        index = segIndex,
                        length = ship.length,
                        vertical = ship.isVertical,
                        state = state,
                        selected = false,
                        sunk = ship.isSunk // Sunk friendly ships also turn DEAD BLACK!
                    )
                } else {
                    if (state == CellState.MISS) {
                        cell.shipType = null
                        cell.cellState = CellState.MISS
                        cell.isSunk = false
                        cell.invalidate()
                    } else {
                        cell.clearCell()
                    }
                }
            }
        }

        // 3. Update Middle Fleet Status Bar (Mini Silhouettes)
        val sunkEnemyShipTypes = activeTargetBoard.getShips().filter { it.isSunk }.map { it.type }.toSet()
        dockCombatFleet.updateFleetCombatState(sunkEnemyShipTypes)

        // 4. Update Score Badges matching reference screenshot ("9" remaining target cells, "17" friendly health)
        val enemyHitsReceived = activeTargetBoard.getShips().sumOf { it.length - it.health }
        val enemyCellsRemaining = (totalShipSegments - enemyHitsReceived).coerceAtLeast(0)
        tvEnemyRemainingBadge.text = "$enemyCellsRemaining"

        val myHitsReceived = activeDefenseBoard.getShips().sumOf { it.length - it.health }
        val myHealthRemaining = (totalShipSegments - myHitsReceived).coerceAtLeast(0)
        tvPlayerRemainingBadge.text = "$myHealthRemaining"
    }

    private fun updateTurnDisplay() {
        val activePlayer = if (isPlayer1Turn) GameSession.player1Name else GameSession.player2Name
        tvTurnTitle.text = "$activePlayer's TURN: FIRE CANNON"
        tvTurnTitle.setTextColor(if (isPlayer1Turn) getColor(R.color.white) else getColor(R.color.dsa_purple))
        tvTurnCounter.text = "T$turnCounter"
    }

    private fun showPassAndPlayCurtain() {
        isFiringLocked = true
        val nextPlayer = if (isPlayer1Turn) GameSession.player1Name else GameSession.player2Name
        tvCurtainTitle.text = "PASS DEVICE TO $nextPlayer"
        layoutPassCurtain.visibility = View.VISIBLE
    }

    private fun handleGameOver(winnerName: String) {
        isFiringLocked = true
        layoutGameOver.visibility = View.VISIBLE

        tvGameOverWinner.text = "$winnerName EMERGES VICTORIOUS!"

        val p1Acc = if (p1Shots == 0) 0 else ((p1Hits.toFloat() / p1Shots) * 100).toInt()
        val p2Acc = if (p2Shots == 0) 0 else ((p2Hits.toFloat() / p2Shots) * 100).toInt()

        val stats = StringBuilder()
            .append("Match Duration: $turnCounter Turns\n\n")
            .append("${GameSession.player1Name}: $p1Hits Hits / $p1Shots Shots ($p1Acc% Accuracy)\n")
            .append("${GameSession.player2Name}: $p2Hits Hits / $p2Shots Shots ($p2Acc% Accuracy)")
            .toString()

        tvGameOverStats.text = stats
    }

    private fun appendBattleLog(message: String) {
        val current = tvBattleLog.text.toString()
        val lines = current.split("\n").takeLast(2).toMutableList()
        lines.add("• $message")
        tvBattleLog.text = lines.joinToString("\n")
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
