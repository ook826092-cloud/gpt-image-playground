package com.gptimage.playground.data.repository

import com.gptimage.playground.data.local.ImageStorage
import com.gptimage.playground.data.local.dao.HistoryDao
import com.gptimage.playground.data.local.entity.HistoryEntity
import com.gptimage.playground.data.model.HistoryImage
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.model.HistoryMetadata
import com.gptimage.playground.data.model.ImageResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Manages generation history: persists metadata to Room, image binaries to
 * [ImageStorage]. Mirrors the web app's history pipeline (`src/lib/image-history.ts`
 * + IndexedDB) but uses Room + the app's internal files dir instead.
 */
class HistoryRepository(
    private val dao: HistoryDao,
    private val storage: ImageStorage,
    private val json: Json
) {
    val history: Flow<List<HistoryItem>> = dao.observeAll().map { entities ->
        entities.map { it.toItem() }
    }

    fun search(query: String): Flow<List<HistoryItem>> =
        dao.search(query).map { entities -> entities.map { it.toItem() } }

    suspend fun findById(id: String): HistoryMetadata? = dao.findById(id)?.toMetadata()

    /**
     * Persist a generation result. Each image is saved to disk; the resulting
     * filenames are recorded in the history metadata.
     */
    suspend fun record(
        prompt: String,
        mode: String,
        model: String,
        providerInstanceId: String,
        providerName: String,
        quality: String,
        outputFormat: String,
        size: String,
        count: Int,
        images: List<ImageResult>,
        durationMs: Long,
        totalTokens: Long = 0L,
        errorMessage: String? = null
    ): HistoryMetadata {
        val recordId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val saved = images.mapIndexed { index, image ->
            val namePrefix = "${recordId}_${timestamp}_$index"
            val savedPath = storage.saveImage(image, namePrefix)
            val filename = savedPath?.substringAfterLast('/') ?: ""
            HistoryImage(
                filename = filename,
                size = 0L,
                width = 0,
                height = 0
            )
        }
        val metadata = HistoryMetadata(
            id = recordId,
            timestamp = timestamp,
            images = saved,
            durationMs = durationMs,
            prompt = prompt,
            mode = mode,
            model = model,
            providerInstanceId = providerInstanceId,
            providerName = providerName,
            quality = quality,
            outputFormat = outputFormat,
            size = size,
            count = count,
            totalTokens = totalTokens,
            errorMessage = errorMessage
        )
        dao.upsert(metadata.toEntity())
        return metadata
    }

    suspend fun delete(id: String) {
        dao.findById(id)?.toMetadata()?.images?.forEach { image ->
            if (image.filename.isNotBlank()) storage.deleteByFilename(image.filename)
        }
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        storage.clearAll()
        dao.clearAll()
    }

    private fun HistoryMetadata.toEntity(): HistoryEntity = HistoryEntity(
        id = id,
        timestamp = timestamp,
        prompt = prompt,
        mode = mode,
        model = model,
        providerName = providerName,
        imageCount = images.size,
        metadataJson = json.encodeToString(HistoryMetadata.serializer(), this)
    )

    private fun HistoryEntity.toMetadata(): HistoryMetadata =
        runCatching { json.decodeFromString(HistoryMetadata.serializer(), metadataJson) }
            .getOrNull()
            ?: HistoryMetadata(
                id = id,
                timestamp = timestamp,
                images = emptyList(),
                durationMs = 0L,
                prompt = prompt,
                mode = mode,
                model = model,
                providerInstanceId = "",
                providerName = providerName,
                quality = "auto",
                outputFormat = "png",
                size = "1024x1024",
                count = imageCount
            )

    private fun HistoryEntity.toItem(): HistoryItem {
        val metadata = toMetadata()
        val thumbnailPath = metadata.images.firstOrNull()?.filename?.let { storage.resolvePath(it) }
            ?.takeIf { it.isNotBlank() }
        return HistoryItem(metadata, thumbnailPath?.takeIf { it.isNotBlank() })
    }
}
