package com.example.ui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.atan2

/**
 * ============================================================================
 * NAVAL ARTILLERY CANNON: Interactive Animated Firing Cannon
 * ============================================================================
 * Renders the iconic naval mortar cannon matching the reference screenshot:
 *  - Red circular swivel ring base with dark border
 *  - Heavy dark steel mortar barrel with highlights and circular muzzle aperture
 *  - Dynamic rotation / aiming toward target grid coordinates
 *  - Realistic mechanical recoil kickback animation on firing
 */
class CannonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Visual Palette matching reference screenshot
    private val BASE_RING_COLOR = Color.parseColor("#D32F2F")
    private val BASE_OUTLINE_COLOR = Color.parseColor("#181210")
    private val BASE_CENTER_COLOR = Color.parseColor("#262628")

    private val BARREL_BODY_COLOR = Color.parseColor("#44464A")
    private val BARREL_HIGHLIGHT_COLOR = Color.parseColor("#64666C")
    private val BARREL_OUTLINE_COLOR = Color.parseColor("#121214")
    private val BARREL_MUZZLE_COLOR = Color.parseColor("#141416")

    // Paints
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Animation & State
    var aimAngleDegrees: Float = -20f
        set(value) {
            field = value
            invalidate()
        }

    var recoilOffsetPx: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var isFiring: Boolean = false
        private set

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f + dpToPx(8)
        val baseRadius = (width.coerceAtMost(height) / 2f) * 0.70f

        // 1. Draw Swivel Base Mount
        // Outer Shadow / Border Ring
        strokePaint.color = BASE_OUTLINE_COLOR
        strokePaint.strokeWidth = dpToPx(5).toFloat()
        canvas.drawCircle(cx, cy, baseRadius, strokePaint)

        // Red Accent Ring
        fillPaint.color = BASE_RING_COLOR
        canvas.drawCircle(cx, cy, baseRadius - dpToPx(2), fillPaint)

        // Inner Dark Well
        fillPaint.color = BASE_CENTER_COLOR
        canvas.drawCircle(cx, cy, baseRadius * 0.72f, fillPaint)
        strokePaint.strokeWidth = dpToPx(3).toFloat()
        canvas.drawCircle(cx, cy, baseRadius * 0.72f, strokePaint)

        // 2. Draw Cannon Barrel (Rotated around base center)
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(aimAngleDegrees)

        // Apply recoil translation along barrel axis
        canvas.translate(0f, recoilOffsetPx)

        drawMortarBarrel(canvas, baseRadius)

        canvas.restore()
    }

    private fun drawMortarBarrel(canvas: Canvas, baseRadius: Float) {
        val barrelWidth = baseRadius * 0.85f
        val barrelHeight = baseRadius * 1.55f
        val halfW = barrelWidth / 2f

        val barrelRect = RectF(-halfW, -barrelHeight, halfW, -baseRadius * 0.2f)

        // Main Barrel Tube
        val tubePath = Path().apply {
            moveTo(-halfW, -barrelHeight + dpToPx(16))
            lineTo(-halfW * 0.92f, 0f)
            quadTo(0f, dpToPx(10).toFloat(), halfW * 0.92f, 0f)
            lineTo(halfW, -barrelHeight + dpToPx(16))
            close()
        }

        fillPaint.color = BARREL_BODY_COLOR
        canvas.drawPath(tubePath, fillPaint)

        // Highlight stripe on side of barrel
        val highlightPath = Path().apply {
            moveTo(-halfW * 0.65f, -barrelHeight + dpToPx(24))
            lineTo(-halfW * 0.40f, -barrelHeight + dpToPx(24))
            lineTo(-halfW * 0.45f, -dpToPx(10).toFloat())
            lineTo(-halfW * 0.70f, -dpToPx(10).toFloat())
            close()
        }
        fillPaint.color = BARREL_HIGHLIGHT_COLOR
        canvas.drawPath(highlightPath, fillPaint)

        // Barrel Outer Stroke
        strokePaint.color = BARREL_OUTLINE_COLOR
        strokePaint.strokeWidth = dpToPx(5).toFloat()
        canvas.drawPath(tubePath, strokePaint)

        // Barrel Muzzle Rim (Oval at top)
        val muzzleOval = RectF(-halfW * 1.08f, -barrelHeight - dpToPx(6), halfW * 1.08f, -barrelHeight + dpToPx(18))
        fillPaint.color = BARREL_MUZZLE_COLOR
        canvas.drawOval(muzzleOval, fillPaint)

        strokePaint.color = BARREL_OUTLINE_COLOR
        strokePaint.strokeWidth = dpToPx(6).toFloat()
        canvas.drawOval(muzzleOval, strokePaint)

        // Deep Muzzle Aperture (inner black void)
        fillPaint.color = Color.BLACK
        val innerVoid = RectF(-halfW * 0.82f, -barrelHeight, halfW * 0.82f, -barrelHeight + dpToPx(12))
        canvas.drawOval(innerVoid, fillPaint)
    }

    /**
     * Animates the cannon aiming towards target coordinates and fires with recoil kickback.
     */
    fun aimAndFire(
        targetX: Float,
        targetY: Float,
        onMuzzleDischarge: (muzzleX: Float, muzzleY: Float) -> Unit
    ) {
        if (isFiring) return
        isFiring = true

        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val cannonCenterX = loc[0] + width / 2f
        val cannonCenterY = loc[1] + height / 2f

        val dx = targetX - cannonCenterX
        val dy = targetY - cannonCenterY
        val targetAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0).toFloat()

        // 1. Aim Rotation Animator
        val rotateAnim = ValueAnimator.ofFloat(aimAngleDegrees, targetAngle).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                aimAngleDegrees = it.animatedValue as Float
            }
        }

        // 2. Recoil Kickback Animator
        val recoilAnim = ValueAnimator.ofFloat(0f, dpToPx(14).toFloat(), 0f).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.8f)
            addUpdateListener {
                recoilOffsetPx = it.animatedValue as Float
            }
        }

        val animSet = AnimatorSet().apply {
            playSequentially(rotateAnim, recoilAnim)
        }

        rotateAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Calculate muzzle discharge screen coordinate
                val rad = Math.toRadians((targetAngle - 90.0))
                val barrelLength = (width / 2f) * 1.2f
                val muzzleScreenX = cannonCenterX + (Math.cos(rad) * barrelLength).toFloat()
                val muzzleScreenY = cannonCenterY + (Math.sin(rad) * barrelLength).toFloat()
                onMuzzleDischarge(muzzleScreenX, muzzleScreenY)
            }
        })

        recoilAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isFiring = false
            }
        })

        animSet.start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
