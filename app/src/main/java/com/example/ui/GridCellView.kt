package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.model.CellState
import com.example.model.ShipType

enum class BoardTheme {
    OCEAN_BLUE,   // Top enemy radar grid (Blue theme from screenshot)
    CORAL_PEACH   // Bottom player fleet defense grid (Warm terracotta/coral theme)
}

/**
 * ============================================================================
 * GRID CELL VIEW: Visual Matrix Cell with Dead-Black Warships and 'X' Miss Marks
 * ============================================================================
 * Matches the reference screenshot:
 *  - "if not found make it as x": Misses render as bold rounded 'X' marks
 *  - "if the ship was founded fully make the ship dead like black": Fully sunk vessels
 *    render as charred dead black warships
 *  - Unhit shots render in tactical hit sparks
 */
class GridCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var cellState: CellState = CellState.EMPTY
    var shipType: ShipType? = null
    var segmentIndex: Int = 0
    var totalShipLength: Int = 0
    var isVertical: Boolean = false
    var isSelectedForRotation: Boolean = false
    var isSunk: Boolean = false

    var boardTheme: BoardTheme = BoardTheme.OCEAN_BLUE
        set(value) {
            field = value
            invalidate()
        }

    private val cellBounds = RectF()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val hitSparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellBounds.set(2f, 2f, w - 2f, h - 2f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw base cell background
        val (bgColor, borderColor) = when (boardTheme) {
            BoardTheme.OCEAN_BLUE -> Pair(Color.parseColor("#35729C"), Color.parseColor("#28587A"))
            BoardTheme.CORAL_PEACH -> Pair(Color.parseColor("#5A4238"), Color.parseColor("#443028"))
        }

        bgPaint.color = bgColor
        borderPaint.color = borderColor
        val cornerRadius = 8f
        canvas.drawRoundRect(cellBounds, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(cellBounds, cornerRadius, cornerRadius, borderPaint)

        // 2. Draw warship segment if a ship occupies this cell (or if ship is revealed)
        val type = shipType
        if (type != null && totalShipLength > 0 && (cellState == CellState.SHIP || cellState == CellState.HIT)) {
            val isHit = (cellState == CellState.HIT)
            WarshipRenderer.drawShipSegment(
                canvas = canvas,
                bounds = cellBounds,
                shipType = type,
                segmentIndex = segmentIndex,
                totalLength = totalShipLength,
                isVertical = isVertical,
                isHit = isHit,
                isSunk = isSunk,
                showRotationHint = isSelectedForRotation
            )
        } else if (cellState == CellState.HIT && type == null) {
            // Hit on opponent grid before ship is fully sunk (fog of war hit spark)
            hitSparkPaint.color = Color.parseColor("#FFD600")
            canvas.drawCircle(cellBounds.centerX(), cellBounds.centerY(), cellBounds.width() * 0.24f, hitSparkPaint)
            hitSparkPaint.color = Color.parseColor("#FF1744")
            canvas.drawCircle(cellBounds.centerX(), cellBounds.centerY(), cellBounds.width() * 0.15f, hitSparkPaint)
        } else if (cellState == CellState.MISS) {
            // "if not found make it as x" -> Draw bold rounded 'X' mark!
            val xColor = when (boardTheme) {
                BoardTheme.OCEAN_BLUE -> Color.parseColor("#6EBEE2")
                BoardTheme.CORAL_PEACH -> Color.parseColor("#FFAF96")
            }
            xPaint.color = xColor
            xPaint.strokeWidth = cellBounds.width() * 0.20f

            val pad = cellBounds.width() * 0.28f
            canvas.drawLine(cellBounds.left + pad, cellBounds.top + pad, cellBounds.right - pad, cellBounds.bottom - pad, xPaint)
            canvas.drawLine(cellBounds.right - pad, cellBounds.top + pad, cellBounds.left + pad, cellBounds.bottom - pad, xPaint)
        }

        // 3. Highlight cell if queued in the player's Salvo FIFO Queue
        if (isQueuedForSalvo) {
            borderPaint.color = Color.parseColor("#00E5FF")
            borderPaint.strokeWidth = 4f
            canvas.drawRoundRect(cellBounds, 8f, 8f, borderPaint)
            borderPaint.strokeWidth = 2f
        }
    }

    var isQueuedForSalvo: Boolean = false
        private set

    fun setSelectedState(selected: Boolean) {
        this.isQueuedForSalvo = selected
        invalidate()
    }

    fun setShipSegment(
        type: ShipType?,
        index: Int,
        length: Int,
        vertical: Boolean,
        state: CellState,
        selected: Boolean = false,
        sunk: Boolean = false
    ) {
        this.shipType = type
        this.segmentIndex = index
        this.totalShipLength = length
        this.isVertical = vertical
        this.cellState = state
        this.isSelectedForRotation = selected
        this.isSunk = sunk
        invalidate()
    }

    fun clearCell() {
        this.shipType = null
        this.segmentIndex = 0
        this.totalShipLength = 0
        this.cellState = CellState.EMPTY
        this.isSelectedForRotation = false
        this.isSunk = false
        this.isQueuedForSalvo = false
        invalidate()
    }
}
