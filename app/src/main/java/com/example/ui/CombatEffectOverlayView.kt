package com.example.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import kotlin.math.sin

/**
 * ============================================================================
 * COMBAT EFFECT OVERLAY: Projectile Ballistics & Target Crosshairs
 * ============================================================================
 * Renders full-screen combat animations matching the reference screenshot:
 *  - White tactical target crosshairs (+ with circle)
 *  - Spherical naval cannonball projectile with shadow
 *  - High-impact blast flash and smoke ring on arrival
 */
class CombatEffectOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Active projectile state
    private var isProjectileActive = false
    private var projectileX = 0f
    private var projectileY = 0f
    private var projectileRadius = 0f
    private var projectileShadowOffset = 0f

    // Target crosshair state
    private var isCrosshairActive = false
    private var crosshairX = 0f
    private var crosshairY = 0f
    private var crosshairRadius = 0f
    private var crosshairAlpha = 255

    // Blast impact state
    private var isBlastActive = false
    private var blastX = 0f
    private var blastY = 0f
    private var blastRadius = 0f
    private var blastAlpha = 255

    // Paints
    private val cannonballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#222224")
        style = Paint.Style.FILL
    }
    private val cannonballStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0C0C0E")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3).toFloat()
    }
    private val cannonballHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5A5C62")
        style = Paint.Style.FILL
    }
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3).toFloat()
        strokeCap = Paint.Cap.ROUND
    }
    private val blastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw Target Reticle / Crosshair (white + circle)
        if (isCrosshairActive) {
            crosshairPaint.alpha = crosshairAlpha
            canvas.drawCircle(crosshairX, crosshairY, crosshairRadius, crosshairPaint)

            val arm = crosshairRadius * 1.55f
            canvas.drawLine(crosshairX - arm, crosshairY, crosshairX + arm, crosshairY, crosshairPaint)
            canvas.drawLine(crosshairX, crosshairY - arm, crosshairX, crosshairY + arm, crosshairPaint)
        }

        // 2. Draw Flying Cannonball
        if (isProjectileActive) {
            // Dark Projectile Base
            canvas.drawCircle(projectileX, projectileY, projectileRadius, cannonballPaint)
            canvas.drawCircle(projectileX, projectileY, projectileRadius, cannonballStroke)

            // Curved highlight on upper quadrant
            val hlR = projectileRadius * 0.35f
            canvas.drawCircle(projectileX - projectileRadius * 0.35f, projectileY - projectileRadius * 0.35f, hlR, cannonballHighlight)
        }

        // 3. Draw Blast Flash / Smoke Ripple
        if (isBlastActive) {
            blastPaint.color = Color.parseColor("#FFD54F")
            blastPaint.alpha = (blastAlpha * 0.85f).toInt()
            canvas.drawCircle(blastX, blastY, blastRadius, blastPaint)

            blastPaint.color = Color.parseColor("#FF1744")
            blastPaint.alpha = blastAlpha
            canvas.drawCircle(blastX, blastY, blastRadius * 0.65f, blastPaint)
        }
    }

    /**
     * Launches a cannonball from muzzle to target, showing crosshairs and impact burst.
     */
    fun launchCannonball(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        onImpact: () -> Unit
    ) {
        // Activate Crosshair at target
        crosshairX = toX
        crosshairY = toY
        crosshairRadius = dpToPx(18).toFloat()
        crosshairAlpha = 255
        isCrosshairActive = true

        val baseRadius = dpToPx(13).toFloat()
        isProjectileActive = true

        val distance = Math.hypot((toX - fromX).toDouble(), (toY - fromY).toDouble()).toFloat()
        val flightDuration = (distance * 0.65f).toLong().coerceIn(280L, 500L)

        val flightAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = flightDuration
            interpolator = AccelerateInterpolator(1.2f)
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                projectileX = fromX + (toX - fromX) * progress

                // Parabolic ballistic arc
                val arcHeight = dpToPx(38) * sin(progress * Math.PI).toFloat()
                projectileY = (fromY + (toY - fromY) * progress) - arcHeight

                // Subtle scale effect (gets slightly larger at apex of arc)
                projectileRadius = baseRadius + (dpToPx(4) * sin(progress * Math.PI).toFloat())

                invalidate()
            }
        }

        flightAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isProjectileActive = false
                isCrosshairActive = false

                // Trigger callback (board resolves Hit, Miss, or Sunk!)
                onImpact()

                // Trigger Impact Flash animation
                triggerImpactBlast(toX, toY)
            }
        })

        flightAnim.start()
    }

    private fun triggerImpactBlast(x: Float, y: Float) {
        blastX = x
        blastY = y
        blastRadius = dpToPx(8).toFloat()
        blastAlpha = 255
        isBlastActive = true

        val blastAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                blastRadius = dpToPx(8) + (dpToPx(24) * fraction)
                blastAlpha = ((1f - fraction) * 255).toInt().coerceAtLeast(0)
                invalidate()
            }
        }

        blastAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isBlastActive = false
                invalidate()
            }
        })

        blastAnim.start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
