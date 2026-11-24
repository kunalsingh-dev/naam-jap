package com.shrijilala.tapcounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CountSession::class], version = 1, exportSchema = false)
abstract class CountHistoryDatabase : RoomDatabase() {
    abstract fun countHistoryDao(): CountHistoryDao

    companion object {
        @Volatile private var INSTANCE: CountHistoryDatabase? = null

        fun getInstance(context: Context): CountHistoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CountHistoryDatabase::class.java,
                    "count_history.db"
                ).build().also { INSTANCE = it }
            }
    }
}
