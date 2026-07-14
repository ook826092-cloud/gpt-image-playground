package com.gptimage.playground.data.repository

import android.graphics.BitmapFactory
import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ImageProviderId
import com.gptimage.playground.data.model.ProviderCredentials
import com.gptimage.playground.data.model.ReferenceImage
import com.gptimage.playground.data.network.ImageProviderService
import com.gptimage.playground.data.network.ProviderException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Base64

/**
 * Combines the [ImageProviderService] (network) with the [HistoryRepository] (persistence):
 * after a successful generation, the returned image bytes are decoded, written to internal storage
 * and a [HistoryItem] row is inserted so the result appears in the album.
 */
class ImageGenerationRepository(
    private val providerService: ImageProviderService,
    private val historyRepository: HistoryRepository
) {

    suspend fun generate(
        request: GenerationRequest,
        credentials: ProviderCredentials,
        provider: ImageProviderId
    ): GenerationOutcome {
        val startedAt = System.currentTimeMillis()
        return try {
            val result = providerService.generate(request, credentials)
            val saved = persistResult(result, request, provider, startedAt, kind = HistoryItem.KIND_GENERATE)
            GenerationOutcome.Success(saved.first(), result)
        } catch (e: ProviderException) {
            GenerationOutcome.Failure(e)
        } catch (e: Throwable) {
            GenerationOutcome.Failure(
                ProviderException(ProviderException.Kind.UNKNOWN, e.message ?: "Unknown error", cause = e)
            )
        }
    }

    suspend fun edit(
        request: EditRequest,
        credentials: ProviderCredentials,
        provider: ImageProviderId
    ): GenerationOutcome {
        val startedAt = System.currentTimeMillis()
        return try {
            val result = providerService.edit(request, credentials)
            val saved = persistResult(result, request.toGenerationRequest(), provider, startedAt, kind = HistoryItem.KIND_EDIT)
            GenerationOutcome.Success(saved.first(), result)
        } catch (e: ProviderException) {
            GenerationOutcome.Failure(e)
        } catch (e: Throwable) {
            GenerationOutcome.Failure(
                ProviderException(ProviderException.Kind.UNKNOWN, e.message ?: "Unknown error", cause = e)
            )
        }
    }

    private suspend fun persistResult(
        result: GenerationResult,
        request: GenerationRequest,
        provider: ImageProviderId,
        startedAt: Long,
        kind: String
    ): List<HistoryItem> = withContext(Dispatchers.IO) {
        if (result.images.isEmpty()) {
            throw ProviderException(
                kind = ProviderException.Kind.PARSE,
                message = "Provider returned no images"
            )
        }
        val now = System.currentTimeMillis()
        val duration = now - startedAt

        result.images.map { image ->
            val (bytes, format, width, height) = decodeImageBytes(image, request)
            val extension = ImageOutputFormats.extension(format)
            val file = historyRepository.newImageFile(extension)
            FileOutputStream(file).use { it.write(bytes) }

            val historyItem = HistoryItem(
                kind = kind,
                provider = provider,
                model = request.model.id,
                modelLabel = request.model.label,
                prompt = request.prompt,
                imagePath = file.absolutePath,
                thumbnailPath = null,
                width = width,
                height = height,
                size = request.size,
                quality = request.quality,
                outputFormat = format,
                createdAt = now,
                durationMs = duration,
                inputTextTokens = result.usage?.inputTextTokens,
                inputImageTokens = result.usage?.inputImageTokens,
                outputTokens = result.usage?.outputTokens,
                errorMessage = null
            )
            val id = historyRepository.insert(historyItem)
            historyItem.copy(id = id)
        }
    }

    private fun decodeImageBytes(
        image: com.gptimage.playground.data.model.GeneratedImage,
        request: GenerationRequest
    ): DecodedImage {
        val bytes = when {
            !image.b64Json.isNullOrBlank() ->
                Base64.getDecoder().decode(image.b64Json)
            !image.url.isNullOrBlank() -> {
                // Synchronous download for remote URLs.
                val connection = java.net.URL(image.url).openConnection()
                connection.connectTimeout = 30_000
                connection.readTimeout = 120_000
                connection.getInputStream().use { it.readBytes() }
            }
            else -> throw ProviderException(
                kind = ProviderException.Kind.PARSE,
                message = "Provider returned image without b64_json or url"
            )
        }

        val format = image.outputFormat.takeIf { ImageOutputFormats.isKnown(it) }
            ?: request.outputFormat
            ?: detectFormatFromBytes(bytes)

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val width = options.outWidth.takeIf { it > 0 }
        val height = options.outHeight.takeIf { it > 0 }

        return DecodedImage(bytes = bytes, format = format, width = width, height = height)
    }

    private fun detectFormatFromBytes(bytes: ByteArray): String {
        if (bytes.size < 4) return ImageOutputFormats.PNG
        return when {
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> ImageOutputFormats.JPEG
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() -> ImageOutputFormats.WEBP
            else -> ImageOutputFormats.PNG
        }
    }

    private data class DecodedImage(
        val bytes: ByteArray,
        val format: String,
        val width: Int?,
        val height: Int?
    )

    private fun EditRequest.toGenerationRequest(): GenerationRequest = GenerationRequest(
        model = model,
        prompt = prompt,
        n = n,
        size = size,
        quality = quality,
        providerOptions = providerOptions
    )
}

sealed interface GenerationOutcome {
    data class Success(val item: HistoryItem, val result: GenerationResult) : GenerationOutcome
    data class Failure(val error: ProviderException) : GenerationOutcome
}

/**
 * Helper to load reference image bytes from a [File] picked by the photo picker.
 */
object ReferenceImages {
    fun fromFile(file: File): ReferenceImage {
        val mimeType = guessMimeType(file)
        return ReferenceImage(name = file.name, mimeType = mimeType, data = file.readBytes())
    }

    private fun guessMimeType(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".png") -> "image/png"
            else -> "image/png"
        }
    }
}
