package com.gptimage.playground.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Generation history entry. Image bytes are persisted to the app's internal storage
 * and referenced here by [imagePath].
 */
@Entity(
    tableName = "history_items",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["provider"]),
        Index(value = ["kind"])
    ]
)
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** "generate" or "edit". */
    val kind: String,
    val provider: String,
    val model: String,
    val modelLabel: String,
    val prompt: String,
    /** Local file path (under app filesDir) of the saved image. */
    val imagePath: String,
    /** Optional local path of a smaller thumbnail. */
    val thumbnailPath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val size: String? = null,
    val quality: String? = null,
    val outputFormat: String? = null,
    val createdAt: Long,
    val durationMs: Long? = null,
    val inputTextTokens: Int? = null,
    val inputImageTokens: Int? = null,
    val outputTokens: Int? = null,
    val errorMessage: String? = null,
    /** JSON array of local file paths for the reference images used in an edit request. */
    val referenceImagePathsJson: String? = null
) {
    val isGenerate: Boolean get() = kind == KIND_GENERATE
    val isEdit: Boolean get() = kind == KIND_EDIT

    companion object {
        const val KIND_GENERATE = "generate"
        const val KIND_EDIT = "edit"
    }
}
