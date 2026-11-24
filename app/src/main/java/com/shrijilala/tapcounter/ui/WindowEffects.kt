package com.shrijilala.tapcounter.ui

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** While [on]: hides status bar/clock + nav bar and dims the screen. Restores on leave. */
@Composable
fun StealthWindow(window: Window, on: Boolean, brightness: Float = 0.01f) {
    val view = LocalView.current
    DisposableEffect(on) {
        val c = WindowInsetsControllerCompat(window, view)
        fun restore() {
            c.show(WindowInsetsCompat.Type.systemBars())
            val p = window.attributes
            p.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = p
        }
        if (on) {
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val p = window.attributes
            p.screenBrightness = brightness.coerceIn(0.01f, 1f)
            window.attributes = p
        } else restore()
        onDispose { restore() }
    }
}
