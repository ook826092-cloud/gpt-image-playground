package com.gptimage.playground.data.repository

import android.content.Context
import com.gptimage.playground.data.db.AppDatabase
import com.gptimage.playground.data.db.HistoryDao
import com.gptimage.playground.data.model.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Owns the persistent history of generated images. The image bytes themselves are stored in the
 * app's internal storage under `generated/<id>.<ext>` and referenced from the [HistoryItem] row.
 */
class HistoryRepository(
    private val dao: HistoryDao,
    private val rootDir: File
) {

    val all: Flow<List<HistoryItem>> = dao.observeAll()

    suspend fun byId(id: Long): HistoryItem? = dao.findById(id)

    suspend fun count(): Int = dao.count()

    suspend fun insert(item: HistoryItem): Long = withContext(Dispatchers.IO) {
        dao.insert(item)
    }

    suspend fun delete(item: HistoryItem) = withContext(Dispatchers.IO) {
        dao.deleteById(item.id)
        runCatching { File(item.imagePath).delete() }
        item.thumbnailPath?.let { path -> runCatching { File(path).delete() } }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
        runCatching { rootDir.deleteRecursively() }
        runCatching { rootDir.mkdirs() }
    }

    /**
     * Allocates a new image file under the app's internal `generated/` directory.
     */
    fun newImageFile(extension: String = "png"): File {
        if (!rootDir.exists()) rootDir.mkdirs()
        val baseName = "img_${System.currentTimeMillis()}"
        return File(rootDir, "$baseName.$extension")
    }

    fun imageFileFor(item: HistoryItem): File = File(item.imagePath)
}
