package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Tactical audio and haptic feedback system using built-in Android ToneGenerator
 * and Vibrator services (zero external asset overhead).
 */
object SoundEffects {

    var soundEnabled: Boolean = true

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    /**
     * Crisp blast tone for direct hit on enemy warship.
     */
    fun playHit(context: Context) {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 180)
            vibrate(context, 120)
        } catch (_: Exception) {}
    }

    /**
     * Soft water splash tone for miss.
     */
    fun playMiss() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
        } catch (_: Exception) {}
    }

    /**
     * Deep double-pulse for warship sunk.
     */
    fun playSunk(context: Context) {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 350)
            vibrate(context, 300)
        } catch (_: Exception) {}
    }

    /**
     * High-pitch radar pulse for Sonar reconnaissance scan.
     */
    fun playSonar() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
        } catch (_: Exception) {}
    }

    /**
     * Victory chime.
     */
    fun playVictory() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 400)
        } catch (_: Exception) {}
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
