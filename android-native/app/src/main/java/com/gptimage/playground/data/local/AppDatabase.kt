package com.gptimage.playground.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gptimage.playground.data.local.dao.HistoryDao
import com.gptimage.playground.data.local.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val DATABASE_NAME = "gpt-image-playground.db"
    }
}
