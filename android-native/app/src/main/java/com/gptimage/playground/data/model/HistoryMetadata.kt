package com.gptimage.playground.data.model

import kotlinx.serialization.Serializable

/** A reference to a stored image file on disk, mirrors HistoryImage from the web app. */
@Serializable
data class HistoryImage(
    val filename: String,
    val size: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
)

/**
 * Persisted generation record, mirrors the web app's HistoryMetadata.
 * Stored as a Room entity (see HistoryEntity) and serialized for backup.
 */
@Serializable
data class HistoryMetadata(
    val id: String,
    val timestamp: Long,
    val images: List<HistoryImage>,
    val durationMs: Long,
    val prompt: String,
    val mode: String, // "generate" | "edit"
    val model: String,
    val providerInstanceId: String,
    val providerName: String,
    val quality: String,
    val outputFormat: String,
    val size: String,
    val count: Int,
    val totalTokens: Long = 0L,
    val errorMessage: String? = null
) {
    val isGenerate: Boolean get() = mode == "generate"
    val isEdit: Boolean get() = mode == "edit"
}

/** UI-facing history item that pairs metadata with a resolved local thumbnail path. */
data class HistoryItem(
    val metadata: HistoryMetadata,
    val thumbnailPath: String?
)
