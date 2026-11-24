package com.shrijilala.tapcounter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CountHistoryDao {
    @Insert
    suspend fun insert(session: CountSession): Long

    @Query("SELECT * FROM count_sessions ORDER BY savedAtEpochMillis DESC")
    fun getAll(): Flow<List<CountSession>>

    @Query("DELETE FROM count_sessions")
    suspend fun clearAll()
}
