package com.shrijilala.tapcounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A snapshot saved automatically every time a reset is CONFIRMED.
 * This is what makes reset "accident-proof" — the count and date are
 * written here before the on-screen counter ever goes back to 0.
 */
@Entity(tableName = "count_sessions")
data class CountSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finalCount: Int,
    val savedAtEpochMillis: Long,
    val label: String? = null
)
