package com.gptimage.playground.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gptimage.playground.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): HistoryEntity?

    @Query("SELECT * FROM history WHERE prompt LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}
