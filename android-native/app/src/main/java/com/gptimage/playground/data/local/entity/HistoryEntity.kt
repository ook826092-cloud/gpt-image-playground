package com.gptimage.playground.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a generation history record. The full HistoryMetadata is
 * stored as JSON in [metadataJson] for forward compatibility, while the most
 * queried fields are promoted to indexed columns.
 */
@Entity(
    tableName = "history",
    indices = [Index("timestamp"), Index("prompt")]
)
data class HistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "prompt")
    val prompt: String,
    @ColumnInfo(name = "mode")
    val mode: String,
    @ColumnInfo(name = "model")
    val model: String,
    @ColumnInfo(name = "provider_name")
    val providerName: String,
    @ColumnInfo(name = "image_count")
    val imageCount: Int,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String
)
