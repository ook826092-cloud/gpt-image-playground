package com.gptimage.playground.data.local

import android.content.Context
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageResult
import java.io.File
import java.util.Base64

/**
 * Persists generated images to the app's internal files storage under `images/`.
 * Mirrors the web app's `imageStorageMode: 'fs'` path used by the Tauri desktop
 * client. Base64 payloads are decoded to binary; remote URLs are downloaded.
 */
class ImageStorage(private val context: Context) {

    private val rootDir: File by lazy {
        File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
    }

    /** Persist a single image result and return its absolute file path. */
    fun saveImage(image: ImageResult, namePrefix: String): String? {
        val format = image.outputFormat
        val ext = when (format) {
            ImageOutputFormat.Png -> "png"
            ImageOutputFormat.Jpeg -> "jpg"
            ImageOutputFormat.Webp -> "webp"
        }
        val file = File(rootDir, "$namePrefix.$ext")
        return when {
            !image.base64.isNullOrBlank() -> runCatching {
                val bytes = Base64.getDecoder().decode(image.base64)
                file.writeBytes(bytes)
                file.absolutePath
            }.getOrNull()
            !image.url.isNullOrBlank() -> {
                // Remote URLs are referenced, not re-hosted in the first batch.
                // Future: download into file for offline history.
                image.url
            }
            else -> null
        }
    }

    /** Resolve the local file path for a stored image filename. */
    fun resolvePath(filename: String): String {
        val file = File(rootDir, filename)
        return if (file.exists()) file.absolutePath else ""
    }

    fun deleteByFilename(filename: String): Boolean = runCatching {
        File(rootDir, filename).delete()
    }.getOrDefault(false)

    fun clearAll(): Boolean = runCatching {
        rootDir.listFiles()?.forEach { it.delete() }
        true
    }.getOrDefault(false)
}
