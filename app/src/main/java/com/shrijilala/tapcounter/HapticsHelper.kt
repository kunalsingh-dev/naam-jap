package com.shrijilala.tapcounter

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticsHelper(context: Context) {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    /** Short, quiet tick — used for every normal tap/increment, including in stealth mode. */
    fun tapTick() = oneShot(18, 60)

    /** Distinct double-buzz so the stealth "negative" button is felt, not seen. */
    fun stealthForceOnPattern() = pattern(longArrayOf(0, 40, 60, 40, 60, 90), intArrayOf(0, 255, 0, 255, 0, 255))

    /** Plain toggle feedback for entering/exiting stealth mode by other means. */
    fun stealthTogglePattern() = pattern(longArrayOf(0, 25, 40, 60), intArrayOf(0, 180, 0, 220))

    private fun oneShot(durationMs: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun pattern(timings: LongArray, amplitudes: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}
