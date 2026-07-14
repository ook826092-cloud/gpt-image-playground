package com.gptimage.playground.data.repository

import android.graphics.BitmapFactory
import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.GeneratedImage
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ImageProviderId
import com.gptimage.playground.data.model.ProviderCredentials
import com.gptimage.playground.data.model.ProviderUsage
import com.gptimage.playground.data.model.ReferenceImage
import com.gptimage.playground.data.network.ImageProviderService
import com.gptimage.playground.data.network.ProviderException
import com.gptimage.playground.data.network.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

    /**
     * 流式生成。返回 [GenerationStreamEvent] 流：
     *  - [GenerationStreamEvent.Partial] 直接转发上游部分图像（ViewModel 解码为 Bitmap 用于预览）
     *  - [GenerationStreamEvent.Completed] 每张图像完成后立即写盘 + 写 Room
     *  - [GenerationStreamEvent.Failure] 上游显式错误或本地异常
     *
     * 仅 OpenAI 兼容上游（`gpt-image-*` 系列）支持；其他 provider 会通过 [UnsupportedOperationException]
     * 转成 [GenerationStreamEvent.Failure]，ViewModel 应在调用前先用 [ImageModelDefinition.supportsStreaming] 判断。
     *
     * @param partialImages 部分图像预览数量（1-3），默认 2 与 Web 端一致
     */
    fun generateStream(
        request: GenerationRequest,
        credentials: ProviderCredentials,
        provider: ImageProviderId,
        partialImages: Int = 2
    ): Flow<GenerationStreamEvent> {
        val startedAt = System.currentTimeMillis()
        return flow {
            val upstream = try {
                providerService.generateStream(request, credentials, partialImages)
            } catch (e: ProviderException) {
                emit(GenerationStreamEvent.Failure(e))
                return@flow
            } catch (e: Throwable) {
                emit(GenerationStreamEvent.Failure(
                    ProviderException(ProviderException.Kind.UNKNOWN, e.message ?: "Unknown error", cause = e)
                ))
                return@flow
            }

            upstream.collect { event ->
                when (event) {
                    is StreamEvent.PartialImage -> emit(
                        GenerationStreamEvent.Partial(
                            b64Json = event.b64Json,
                            imageIndex = event.imageIndex,
                            partialImageIndex = event.partialImageIndex
                        )
                    )

                    is StreamEvent.CompletedImage -> {
                        val item = runCatching {
                            persistSingleImage(
                                b64Json = event.b64Json,
                                outputFormat = event.outputFormat,
                                usage = event.usage,
                                request = request,
                                provider = provider,
                                startedAt = startedAt,
                                kind = HistoryItem.KIND_GENERATE
                            )
                        }.getOrElse { e ->
                            emit(GenerationStreamEvent.Failure(
                                ProviderException(
                                    kind = ProviderException.Kind.PARSE,
                                    message = e.message ?: "Failed to persist streaming image",
                                    cause = e
                                )
                            ))
                            return@collect
                        }
                        emit(GenerationStreamEvent.Completed(item))
                    }

                    is StreamEvent.Error -> emit(
                        GenerationStreamEvent.Failure(
                            ProviderException(
                                kind = event.kind,
                                message = event.message
                            )
                        )
                    )
                }
            }
        }.catch { e ->
            // Flow 内部异常（取消异常会自然传播；其他转成 Failure）
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(GenerationStreamEvent.Failure(
                ProviderException(ProviderException.Kind.UNKNOWN, e.message ?: "Stream failed", cause = e)
            ))
        }
    }

    /**
     * 流式编辑。与 [generateStream] 类似，但走 [ImageProviderService.editStream] 路径。
     */
    fun editStream(
        request: EditRequest,
        credentials: ProviderCredentials,
        provider: ImageProviderId,
        partialImages: Int = 2
    ): Flow<GenerationStreamEvent> {
        val startedAt = System.currentTimeMillis()
        val generationRequest = request.toGenerationRequest()
        return flow {
            val upstream = try {
                providerService.editStream(request, credentials, partialImages)
            } catch (e: ProviderException) {
                emit(GenerationStreamEvent.Failure(e))
                return@flow
            } catch (e: Throwable) {
                emit(GenerationStreamEvent.Failure(
                    ProviderException(ProviderException.Kind.UNKNOWN, e.message ?: "Unknown error", cause = e)
                ))
                return@flow
            }

            upstream.collect { event ->
                when (event) {
                    is StreamEvent.PartialImage -> emit(
                        GenerationStreamEvent.Partial(
                            b64Json = event.b64Json,
                            imageIndex = event.imageIndex,
                            partialImageIndex = event.partialImageIndex
                        )
                    )

                    is StreamEvent.CompletedImage -> {
                        val item = runCatching {
                            persistSingleImage(
                                b64Json = event.b64Json,
                                outputFormat = event.outputFormat,
                                usage = event.usage,
                                request = generationRequest,
                                provider = provider,
                                startedAt = startedAt,
                                kind = HistoryItem.KIND_EDIT
                            )
                        }.getOrElse { e ->
                            emit(GenerationStreamEvent.Failure(
                                ProviderException(
                                    kind = ProviderException.Kind.PARSE,
                                    message = e.message ?: "Failed to persist streaming image",
                                    cause = e
                                )
                            ))
                            return@collect
                        }
                        emit(GenerationStreamEvent.Completed(item))
                    }

                    is StreamEvent.Error -> emit(
                        GenerationStreamEvent.Failure(
                            ProviderException(
                                kind = event.kind,
                                message = event.message
                            )
                        )
                    )
                }
            }
        }.catch { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(GenerationStreamEvent.Failure(
                ProviderException(ProviderException.Kind.UNKNOWN, e.message ?: "Stream failed", cause = e)
            ))
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
        result.images.map { image ->
            persistSingleImage(
                b64Json = image.b64Json,
                url = image.url,
                outputFormat = image.outputFormat,
                usage = result.usage,
                request = request,
                provider = provider,
                startedAt = startedAt,
                kind = kind
            )
        }
    }

    /**
     * 把单张图像写盘 + 写 Room。流式与非流式路径共用。
     *
     * 支持 b64_json 与 url 两种来源（流式路径上游只会给 b64_json，但保持兼容）。
     */
    private suspend fun persistSingleImage(
        b64Json: String?,
        url: String?,
        outputFormat: String,
        usage: ProviderUsage?,
        request: GenerationRequest,
        provider: ImageProviderId,
        startedAt: Long,
        kind: String
    ): HistoryItem = withContext(Dispatchers.IO) {
        val image = GeneratedImage(b64Json = b64Json, url = url, outputFormat = outputFormat)
        val (bytes, format, width, height) = decodeImageBytes(image, request)
        val extension = ImageOutputFormats.extension(format)
        val file = historyRepository.newImageFile(extension)
        FileOutputStream(file).use { it.write(bytes) }

        val now = System.currentTimeMillis()
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
            durationMs = now - startedAt,
            inputTextTokens = usage?.inputTextTokens,
            inputImageTokens = usage?.inputImageTokens,
            outputTokens = usage?.outputTokens,
            errorMessage = null
        )
        val id = historyRepository.insert(historyItem)
        historyItem.copy(id = id)
    }

    /** 流式路径专用：上游只给 b64_json，无 url。 */
    private suspend fun persistSingleImage(
        b64Json: String,
        outputFormat: String,
        usage: ProviderUsage?,
        request: GenerationRequest,
        provider: ImageProviderId,
        startedAt: Long,
        kind: String
    ): HistoryItem = persistSingleImage(
        b64Json = b64Json,
        url = null,
        outputFormat = outputFormat,
        usage = usage,
        request = request,
        provider = provider,
        startedAt = startedAt,
        kind = kind
    )

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
