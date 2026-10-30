package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.ShipType

/**
 * ============================================================================
 * WARSHIP GRAPHICS ENGINE: Custom Vector Renderer for Distinct Warship Sizes
 * ============================================================================
 * Renders distinct visual silhouettes, deck details, and armament for the 5 warship types:
 *  1. Aircraft Carrier (Length 5): Wide flight deck with 3 jet fighter silhouettes, island tower, runway
 *  2. Battleship (Length 4): Armored hull with dual fore & aft artillery gun turrets and cooling grilles
 *  3. Cruiser (Length 3): Sleek naval cruiser with forward artillery cannon, bridge, and pointed bow
 *  4. Submarine (Length 3): Torpedo submarine with hydroplane dive fins, conning tower sail, and rudder
 *  5. Patrol Boat (Length 2): Compact high-speed boat with twin stern exhaust pods and cabin grilles
 */
object WarshipRenderer {

    // Palette matching the reference design
    private val HULL_COLOR = Color.parseColor("#E85248")
    private val HULL_SHADOW = Color.parseColor("#C63D34")
    private val OUTLINE_COLOR = Color.parseColor("#261A18")
    private val DETAIL_COLOR = Color.parseColor("#261A18")
    private val HIT_OVERLAY_COLOR = Color.parseColor("#B71C1C")
    private val SUNK_OVERLAY_COLOR = Color.parseColor("#37474F")
    private val ROTATION_ARROW_COLOR = Color.WHITE

    /**
     * Renders a complete standalone warship graphic for the Ships Dock.
     */
    fun createDockShipBitmap(
        shipType: ShipType,
        widthPx: Int,
        heightPx: Int,
        isPlaced: Boolean,
        isSelected: Boolean,
        isSunk: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(10), heightPx.coerceAtLeast(10), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val alpha = if (isPlaced && !isSunk) 70 else 255
        val bounds = RectF(6f, 6f, widthPx - 6f, heightPx - 6f)

        drawFullShip(canvas, bounds, shipType, isVertical = false, alpha = alpha, isSelected = isSelected, isSunk = isSunk)
        return bitmap
    }

    /**
     * Draws the complete warship across a single continuous bounding box.
     */
    fun drawFullShip(
        canvas: Canvas,
        bounds: RectF,
        shipType: ShipType,
        isVertical: Boolean,
        alpha: Int = 255,
        isSelected: Boolean = false,
        isSunk: Boolean = false
    ) {
        canvas.save()

        if (isVertical) {
            canvas.rotate(90f, bounds.centerX(), bounds.centerY())
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isSunk) Color.parseColor("#1C1C1E") else HULL_COLOR
            this.alpha = alpha
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isSunk) Color.parseColor("#08080A") else OUTLINE_COLOR
            strokeWidth = if (isSunk) 6f else 5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.alpha = alpha
        }

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isSunk) Color.parseColor("#2E2E32") else DETAIL_COLOR
            this.alpha = alpha
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isSunk) Color.parseColor("#2A2A2E") else DETAIL_COLOR
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            this.alpha = alpha
        }

        // Selection highlight ring if active
        if (isSelected) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.parseColor("#00E5FF")
                strokeWidth = 6f
            }
            canvas.drawRoundRect(RectF(bounds.left - 4, bounds.top - 4, bounds.right + 4, bounds.bottom + 4), 14f, 14f, haloPaint)
        }

        when (shipType) {
            ShipType.CARRIER -> drawCarrier(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.BATTLESHIP -> drawBattleship(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.CRUISER -> drawCruiser(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.SUBMARINE -> drawSubmarine(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.DESTROYER -> drawPatrolBoat(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
        }

        canvas.restore()
    }

    /**
     * Draws an individual cell segment of a ship on the board grid.
     */
    fun drawShipSegment(
        canvas: Canvas,
        bounds: RectF,
        shipType: ShipType,
        segmentIndex: Int,
        totalLength: Int,
        isVertical: Boolean,
        isHit: Boolean,
        isSunk: Boolean,
        showRotationHint: Boolean
    ) {
        val w = bounds.width()
        val h = bounds.height()

        canvas.save()

        // Clip to cell bounds
        canvas.clipRect(bounds)

        // Calculate the virtual total length of the ship in pixels
        val shipTotalLength = if (isVertical) h * totalLength else w * totalLength
        val shipWidth = if (isVertical) w else h

        // Compute virtual full-ship bounds such that the current segment aligns with `bounds`
        val virtualBounds = if (!isVertical) {
            val leftOffset = bounds.left - (segmentIndex * w)
            RectF(leftOffset, bounds.top, leftOffset + shipTotalLength, bounds.bottom)
        } else {
            val topOffset = bounds.top - (segmentIndex * h)
            RectF(bounds.left, topOffset, bounds.right, topOffset + shipTotalLength)
        }

        // Hull paint with hit / sunk styling
        val baseHullColor = when {
            isSunk -> Color.parseColor("#1C1C1E") // DEAD BLACK when fully found
            isHit -> HIT_OVERLAY_COLOR
            else -> HULL_COLOR
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = baseHullColor
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isSunk) Color.parseColor("#08080A") else OUTLINE_COLOR
            strokeWidth = if (isSunk) 6f else 5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isSunk) Color.parseColor("#2E2E32") else DETAIL_COLOR
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isSunk) Color.parseColor("#2A2A2E") else DETAIL_COLOR
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }

        // Draw the full ship, which gets clipped to this cell segment!
        if (isVertical) {
            // In vertical orientation, rotate around the center of the virtual bounds
            canvas.save()
            // Map vertical alignment:
            val vLeft = bounds.left
            val vTop = bounds.top - (segmentIndex * h)
            val hBounds = RectF(vTop, vLeft, vTop + (totalLength * h), vLeft + w)

            // Translate and rotate so vertical drawing matches
            canvas.translate(bounds.centerX(), bounds.centerY())
            canvas.rotate(90f)
            canvas.translate(-bounds.centerY(), -bounds.centerX())

            val rotatedVirtualBounds = RectF(
                bounds.centerX() - (shipTotalLength / 2f) + ((totalLength / 2f - segmentIndex - 0.5f) * h),
                bounds.centerY() - (w * 0.42f),
                bounds.centerX() + (shipTotalLength / 2f) + ((totalLength / 2f - segmentIndex - 0.5f) * h),
                bounds.centerY() + (w * 0.42f)
            )

            drawShipByType(canvas, rotatedVirtualBounds, shipType, fillPaint, strokePaint, detailPaint, linePaint)
            canvas.restore()
        } else {
            val adjustedBounds = RectF(virtualBounds.left, virtualBounds.top + (h * 0.08f), virtualBounds.right, virtualBounds.bottom - (h * 0.08f))
            drawShipByType(canvas, adjustedBounds, shipType, fillPaint, strokePaint, detailPaint, linePaint)
        }

        // If this ship is currently active/selected, draw rotation arrows on the middle segment
        if (showRotationHint && segmentIndex == totalLength / 2) {
            drawRotationArrows(canvas, bounds)
        }

        // If hit and ship is still alive, draw hit sparks. Once sunk, the ship renders purely as dead black.
        if (isHit && !isSunk) {
            drawHitSparks(canvas, bounds)
        }

        canvas.restore()
    }

    private fun drawShipByType(
        canvas: Canvas,
        bounds: RectF,
        shipType: ShipType,
        fillPaint: Paint,
        strokePaint: Paint,
        detailPaint: Paint,
        linePaint: Paint
    ) {
        when (shipType) {
            ShipType.CARRIER -> drawCarrier(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.BATTLESHIP -> drawBattleship(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.CRUISER -> drawCruiser(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.SUBMARINE -> drawSubmarine(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
            ShipType.DESTROYER -> drawPatrolBoat(canvas, bounds, fillPaint, strokePaint, detailPaint, linePaint)
        }
    }

    // ========================================================================
    // 1. AIRCRAFT CARRIER (SIZE 5)
    // Distinct wide flight deck with runway, island bridge tower, and 3 jet fighter silhouettes
    // ========================================================================
    private fun drawCarrier(
        canvas: Canvas,
        b: RectF,
        fillPaint: Paint,
        strokePaint: Paint,
        detailPaint: Paint,
        linePaint: Paint
    ) {
        val path = Path()
        val h = b.height()
        val w = b.width()

        // Asymmetric flight deck: angled bow, island tower cutout on top side
        path.moveTo(b.left + 8f, b.top + h * 0.25f)
        path.lineTo(b.left + w * 0.35f, b.top + h * 0.25f)
        // Island tower protruding slightly on top
        path.lineTo(b.left + w * 0.38f, b.top)
        path.lineTo(b.left + w * 0.58f, b.top)
        path.lineTo(b.left + w * 0.60f, b.top + h * 0.25f)
        // Flight deck bow (angled)
        path.lineTo(b.right - w * 0.12f, b.top + h * 0.25f)
        path.lineTo(b.right, b.top + h * 0.50f)
        path.lineTo(b.right - w * 0.08f, b.bottom - h * 0.20f)
        // Bottom hull line
        path.lineTo(b.left + 8f, b.bottom - h * 0.20f)
        path.quadTo(b.left, b.centerY(), b.left + 8f, b.top + h * 0.25f)
        path.close()

        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)

        // 3 Jet Fighter Silhouettes (deltas) on the flight deck
        val jetY = b.bottom - h * 0.40f
        val jetStartX = b.left + w * 0.10f
        val jetSpacing = w * 0.06f
        for (i in 0 until 3) {
            val jx = jetStartX + (i * jetSpacing)
            drawJetSilhouette(canvas, jx, jetY, h * 0.22f, detailPaint)
        }

        // Center cooling grilles
        val ventX = b.left + w * 0.45f
        for (i in 0 until 3) {
            val vy = b.centerY() - 6f + (i * 6f)
            canvas.drawLine(ventX, vy, ventX + w * 0.08f, vy, linePaint)
        }

        // Forward catapult markings
        canvas.drawLine(b.right - w * 0.28f, b.centerY(), b.right - w * 0.10f, b.centerY(), linePaint)
    }

    private fun drawJetSilhouette(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val p = Path()
        p.moveTo(cx + size * 0.5f, cy) // Nose pointing right
        p.lineTo(cx - size * 0.4f, cy - size * 0.4f) // Left wing
        p.lineTo(cx - size * 0.15f, cy)
        p.lineTo(cx - size * 0.4f, cy + size * 0.4f) // Right wing
        p.close()
        canvas.drawPath(p, paint)
    }

    // ========================================================================
    // 2. BATTLESHIP (SIZE 4)
    // Long armored hull with fore and aft artillery gun turrets and cooling vents
    // ========================================================================
    private fun drawBattleship(
        canvas: Canvas,
        b: RectF,
        fillPaint: Paint,
        strokePaint: Paint,
        detailPaint: Paint,
        linePaint: Paint
    ) {
        val path = Path()
        val h = b.height()
        val r = h * 0.35f

        // Hull: rounded flat stern, pointed sharp bow
        path.moveTo(b.left + r, b.top + h * 0.18f)
        path.lineTo(b.right - h * 0.8f, b.top + h * 0.18f)
        path.lineTo(b.right, b.centerY()) // Pointed bow
        path.lineTo(b.right - h * 0.8f, b.bottom - h * 0.18f)
        path.lineTo(b.left + r, b.bottom - h * 0.18f)
        path.quadTo(b.left, b.centerY(), b.left + r, b.top + h * 0.18f)
        path.close()

        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)

        // Rear Gun Turret
        val rearTurretX = b.left + b.width() * 0.15f
        drawTurret(canvas, rearTurretX, b.centerY(), h * 0.16f, isPointingRight = false, detailPaint, strokePaint)

        // Central Cooling Grilles
        val ventX = b.left + b.width() * 0.42f
        for (i in 0 until 3) {
            val vy = b.centerY() - 6f + (i * 6f)
            canvas.drawLine(ventX, vy, ventX + b.width() * 0.16f, vy, linePaint)
        }

        // Forward Heavy Gun Turret
        val frontTurretX = b.right - b.width() * 0.22f
        drawTurret(canvas, frontTurretX, b.centerY(), h * 0.16f, isPointingRight = true, detailPaint, strokePaint)
    }

    // ========================================================================
    // 3. CRUISER / DESTROYER (SIZE 3)
    // Sleek warship with forward cannon turret, midship vents, and tapered hull
    // ========================================================================
    private fun drawCruiser(
        canvas: Canvas,
        b: RectF,
        fillPaint: Paint,
        strokePaint: Paint,
        detailPaint: Paint,
        linePaint: Paint
    ) {
        val path = Path()
        val h = b.height()
        val r = h * 0.35f

        path.moveTo(b.left + r, b.top + h * 0.18f)
        path.lineTo(b.right - h * 0.7f, b.top + h * 0.18f)
        path.lineTo(b.right, b.centerY()) // Pointed prow
        path.lineTo(b.right - h * 0.7f, b.bottom - h * 0.18f)
        path.lineTo(b.left + r, b.bottom - h * 0.18f)
        path.quadTo(b.left, b.centerY(), b.left + r, b.top + h * 0.18f)
        path.close()

        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)

        // Central Cooling Grilles
        val ventX = b.left + b.width() * 0.38f
        for (i in 0 until 3) {
            val vy = b.centerY() - 5f + (i * 5f)
            canvas.drawLine(ventX, vy, ventX + b.width() * 0.22f, vy, linePaint)
        }

        // Forward Artillery Turret
        val turretX = b.right - b.width() * 0.22f
        drawTurret(canvas, turretX, b.centerY(), h * 0.15f, isPointingRight = true, detailPaint, strokePaint)
    }

    // ========================================================================
    // 4. SUBMARINE (SIZE 3)
    // Rounded sonar nose, conning tower sail, and side dive plane fins
    // ========================================================================
    private fun drawSubmarine(
        canvas: Canvas,
        b: RectF,
        fillPaint: Paint,
        strokePaint: Paint,
        detailPaint: Paint,
        linePaint: Paint
    ) {
        val path = Path()
        val h = b.height()
        val w = b.width()

        // Torpedo profile with protruding side dive planes
        path.moveTo(b.left + w * 0.12f, b.top + h * 0.28f)

        // Tail rudder notch
        path.lineTo(b.left + 4f, b.top + h * 0.22f)
        path.lineTo(b.left + 4f, b.bottom - h * 0.22f)
        path.lineTo(b.left + w * 0.12f, b.bottom - h * 0.28f)

        // Lower dive plane fin
        path.lineTo(b.left + w * 0.45f, b.bottom - h * 0.28f)
        path.lineTo(b.left + w * 0.48f, b.bottom)
        path.lineTo(b.left + w * 0.58f, b.bottom)
        path.lineTo(b.left + w * 0.61f, b.bottom - h * 0.28f)

        // Rounded sonar nose
        path.lineTo(b.right - h * 0.35f, b.bottom - h * 0.28f)
        path.quadTo(b.right, b.centerY(), b.right - h * 0.35f, b.top + h * 0.28f)

        // Upper dive plane fin
        path.lineTo(b.left + w * 0.61f, b.top + h * 0.28f)
        path.lineTo(b.left + w * 0.58f, b.top)
        path.lineTo(b.left + w * 0.48f, b.top)
        path.lineTo(b.left + w * 0.45f, b.top + h * 0.28f)
        path.close()

        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)

        // Central Conning Tower Sail & Grilles
        val ventX = b.left + w * 0.42f
        for (i in 0 until 3) {
            val vy = b.centerY() - 5f + (i * 5f)
            canvas.drawLine(ventX, vy, ventX + w * 0.20f, vy, linePaint)
        }
    }

    // ========================================================================
    // 5. PATROL BOAT / GUNBOAT (SIZE 2)
    // Rounded compact hull with twin stern exhaust ports and cabin grilles
    // ========================================================================
    private fun drawPatrolBoat(
        canvas: Canvas,
        b: RectF,
        fillPaint: Paint,
        strokePaint: Paint,
        detailPaint: Paint,
        linePaint: Paint
    ) {
        val r = b.height() * 0.30f
        val rect = RectF(b.left + 4f, b.top + b.height() * 0.15f, b.right - 4f, b.bottom - b.height() * 0.15f)
        canvas.drawRoundRect(rect, r, r, fillPaint)
        canvas.drawRoundRect(rect, r, r, strokePaint)

        // Twin Stern Exhaust Ports
        val exhaustRadius = b.height() * 0.10f
        val exhaustX = b.left + b.width() * 0.20f
        canvas.drawCircle(exhaustX, b.centerY() - b.height() * 0.14f, exhaustRadius, detailPaint)
        canvas.drawCircle(exhaustX, b.centerY() + b.height() * 0.14f, exhaustRadius, detailPaint)

        // Central Cabin Cooling Grilles
        val ventX = b.left + b.width() * 0.42f
        for (i in 0 until 3) {
            val vy = b.centerY() - 5f + (i * 5f)
            canvas.drawLine(ventX, vy, ventX + b.width() * 0.26f, vy, linePaint)
        }

        // Bow Forward Light
        val bowLightX = b.right - b.width() * 0.18f
        canvas.drawCircle(bowLightX, b.centerY(), exhaustRadius * 0.8f, detailPaint)
    }

    private fun drawTurret(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        isPointingRight: Boolean,
        detailPaint: Paint,
        strokePaint: Paint
    ) {
        // Turret Base Circle
        canvas.drawCircle(cx, cy, radius, detailPaint)

        // Gun Barrel
        val barrelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = OUTLINE_COLOR
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }
        val barrelLen = radius * 1.5f
        val endX = if (isPointingRight) cx + barrelLen else cx - barrelLen
        canvas.drawLine(cx, cy, endX, cy, barrelPaint)
    }

    /**
     * Draws rotation arrows indicator (matching the reference photo `↻`).
     */
    private fun drawRotationArrows(canvas: Canvas, bounds: RectF) {
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = ROTATION_ARROW_COLOR
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
        }

        val cx = bounds.centerX()
        val cyTop = bounds.top - 8f
        val cyBottom = bounds.bottom + 8f
        val r = 14f

        // Top arc
        val topRect = RectF(cx - r, cyTop - r, cx + r, cyTop + r)
        canvas.drawArc(topRect, 180f, 180f, false, shadowPaint)
        canvas.drawArc(topRect, 180f, 180f, false, arrowPaint)

        // Bottom arc
        val btmRect = RectF(cx - r, cyBottom - r, cx + r, cyBottom + r)
        canvas.drawArc(btmRect, 0f, 180f, false, shadowPaint)
        canvas.drawArc(btmRect, 0f, 180f, false, arrowPaint)
    }

    private fun drawHitSparks(canvas: Canvas, bounds: RectF) {
        val hitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD600")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() * 0.22f, hitPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() * 0.14f, corePaint)
    }

    private fun drawSunkCross(canvas: Canvas, bounds: RectF) {
        val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }
        val pad = bounds.width() * 0.25f
        canvas.drawLine(bounds.left + pad, bounds.top + pad, bounds.right - pad, bounds.bottom - pad, crossPaint)
        canvas.drawLine(bounds.right - pad, bounds.top + pad, bounds.left + pad, bounds.bottom - pad, crossPaint)
    }
}
