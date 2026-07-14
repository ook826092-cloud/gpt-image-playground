package com.gptimage.playground.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gptimage.playground.data.model.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): HistoryItem?

    @Query("SELECT COUNT(*) FROM history_items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem): Long

    @Query("DELETE FROM history_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history_items")
    suspend fun deleteAll()

    @Query("DELETE FROM history_items WHERE createdAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int
}
