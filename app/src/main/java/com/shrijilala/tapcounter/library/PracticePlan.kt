package com.shrijilala.tapcounter.library

import android.content.Context
import java.time.LocalDate

/**
 * mode 0 = repeat the SAME pads every day.
 * mode 1 = move on: perDay pads per day, never repeating until the series is finished.
 */
data class PracticePlan(
    val docId: String,
    val series: List<Int>,
    val mode: Int,
    val perDay: Int,
    val startDay: Long
)

object PracticeStore {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("practice", Context.MODE_PRIVATE)

    fun load(ctx: Context): PracticePlan? {
        val s = sp(ctx)
        val doc = s.getString("doc", null) ?: return null
        val series = (s.getString("series", "") ?: "").split(",").mapNotNull { it.trim().toIntOrNull() }
        return PracticePlan(doc, series, s.getInt("mode", 0), s.getInt("perDay", 3), s.getLong("start", today()))
    }

    fun save(ctx: Context, p: PracticePlan) {
        sp(ctx).edit().putString("doc", p.docId).putString("series", p.series.joinToString(","))
            .putInt("mode", p.mode).putInt("perDay", p.perDay).putLong("start", p.startDay).apply()
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    /** null = series finished (mode 1 only). */
    fun todayPads(p: PracticePlan, day: Long = today()): List<Int>? {
        if (p.series.isEmpty()) return emptyList()
        if (p.mode == 0) return p.series
        val per = p.perDay.coerceAtLeast(1)
        val from = (day - p.startDay).coerceAtLeast(0).toInt() * per
        if (from >= p.series.size) return null
        return p.series.subList(from, minOf(from + per, p.series.size))
    }

    fun dayNumber(p: PracticePlan): Int = (today() - p.startDay).coerceAtLeast(0).toInt() + 1
}
