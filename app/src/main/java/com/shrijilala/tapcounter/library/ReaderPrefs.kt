package com.shrijilala.tapcounter.library

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign

/** Reader look & feel. Every change is saved immediately via save(). */
class ReaderPrefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("reader", Context.MODE_PRIVATE)

    var themeIdx by mutableIntStateOf(sp.getInt("theme", 0))          // -1 = custom colors
    var customBg by mutableIntStateOf(sp.getInt("cbg", 0xFF121212.toInt()))
    var customFg by mutableIntStateOf(sp.getInt("cfg", 0xFFEEEEEE.toInt()))
    var fontSize by mutableFloatStateOf(sp.getFloat("size", 22f))
    var lineSpacing by mutableFloatStateOf(sp.getFloat("spacing", 1.5f))
    var fontIdx by mutableIntStateOf(sp.getInt("font", 0))
    var alignIdx by mutableIntStateOf(sp.getInt("align", 1))          // 0 left 1 center 2 right
    var bold by mutableStateOf(sp.getBoolean("bold", true))
    var scrollMode by mutableIntStateOf(sp.getInt("scroll", 0))       // 0 swipe pages, 1 vertical
    var lang by mutableStateOf(sp.getString("lang", "hi") ?: "hi")
    var showMeaning by mutableStateOf(sp.getBoolean("meaning", true))

    fun save() {
        sp.edit()
            .putInt("theme", themeIdx).putInt("cbg", customBg).putInt("cfg", customFg)
            .putFloat("size", fontSize).putFloat("spacing", lineSpacing)
            .putInt("font", fontIdx).putInt("align", alignIdx).putBoolean("bold", bold)
            .putInt("scroll", scrollMode).putString("lang", lang).putBoolean("meaning", showMeaning)
            .apply()
    }

    fun reset() {
        themeIdx = 0; fontSize = 22f; lineSpacing = 1.5f; fontIdx = 0
        alignIdx = 1; bold = true; scrollMode = 0; showMeaning = true
        save()
    }

    /** background, text, accent */
    fun colors(): Triple<Color, Color, Color> =
        if (themeIdx < 0 || themeIdx >= THEMES.size) Triple(Color(customBg), Color(customFg), Color(0xFFFFB300))
        else THEMES[themeIdx].second

    fun family(): FontFamily = when (fontIdx) {
        1 -> FontFamily.Serif; 2 -> FontFamily.SansSerif; 3 -> FontFamily.Monospace; 4 -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    fun align(): TextAlign = when (alignIdx) { 0 -> TextAlign.Start; 2 -> TextAlign.End; else -> TextAlign.Center }

    companion object {
        val THEMES: List<Pair<String, Triple<Color, Color, Color>>> = listOf(
            "Night" to Triple(Color(0xFF121212), Color(0xFFEEEEEE), Color(0xFFFFB300)),
            "Red-Black" to Triple(Color(0xFF120202), Color(0xFFFFFFFF), Color(0xFFB22222)),
            "Sepia" to Triple(Color(0xFFF4E8D0), Color(0xFF3B2A1A), Color(0xFF8B4513)),
            "White" to Triple(Color(0xFFFFFFFF), Color(0xFF111111), Color(0xFFB22222)),
            "Saffron" to Triple(Color(0xFFFF9933), Color(0xFF1A0A00), Color(0xFF7A0000)),
            "Black" to Triple(Color(0xFF000000), Color(0xFFFFD54F), Color(0xFFFF7043))
        )
        val FONT_NAMES = listOf("Default", "Serif", "Sans", "Mono", "Cursive")
        val PALETTE: List<Long> = listOf(
            0xFF000000, 0xFF121212, 0xFF120202, 0xFF1B2A41, 0xFFF4E8D0, 0xFFFFFFFF,
            0xFFEEEEEE, 0xFFFFD54F, 0xFFFF9933, 0xFFB22222, 0xFF8BC34A, 0xFF4FC3F7
        )
    }
}
