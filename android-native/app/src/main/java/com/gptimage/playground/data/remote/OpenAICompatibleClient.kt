package com.gptimage.playground.data.remote

import com.gptimage.playground.data.model.GenerationMode
import com.gptimage.playground.data.model.GenerationParams
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageQuality
import com.gptimage.playground.data.model.ImageResult
import com.gptimage.playground.data.model.ProviderInstance
import com.gptimage.playground.data.model.ProviderUsage
import com.gptimage.playground.data.remote.dto.ApiErrorResponse
import com.gptimage.playground.data.remote.dto.ImageData
import com.gptimage.playground.data.remote.dto.ImageEditRequest
import com.gptimage.playground.data.remote.dto.ImageGenerationRequest
import com.gptimage.playground.data.remote.dto.ImageGenerationResponse
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/**
 * Result of a network operation: success with data, or failure with a typed
 * error. Mirrors the web app's api-error categorization.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}

sealed class ApiError(val message: String) {
    class Network(message: String) : ApiError(message)
    class Auth(message: String) : ApiError(message)
    class RateLimit(message: String) : ApiError(message)
    class Server(message: String) : ApiError(message)
    class Client(message: String) : ApiError(message)
    class Parse(message: String) : ApiError(message)
    class Unknown(message: String) : ApiError(message)
}

/**
 * OpenAI-compatible image API client. This replaces the web app's
 * `/api/images` Next.js route and the Tauri Rust `proxy/openai.rs` module:
 * the native app calls the provider directly, with no server in between.
 */
class OpenAICompatibleClient(
    private val httpClient: HttpClient,
    private val json: Json
) {
    /** Generate or edit images for the given provider + params. */
    suspend fun request(
        provider: ProviderInstance,
        params: GenerationParams
    ): ApiResult<GenerationResult> {
        if (provider.apiKey.isBlank()) {
            return ApiResult.Failure(ApiError.Auth("Missing API key for ${provider.name}"))
        }
        val started = System.currentTimeMillis()
        val result: ApiResult<ImageGenerationResponse> = if (params.mode == GenerationMode.Edit) {
            requestEdit(provider, params)
        } else {
            requestGenerate(provider, params)
        }
        return when (result) {
            is ApiResult.Success -> {
                val images = result.value.data.orEmpty().mapNotNull { it.toImageResult(params.outputFormat) }
                if (images.isEmpty()) {
                    ApiResult.Failure(ApiError.Parse("Provider returned no images"))
                } else {
                    ApiResult.Success(
                        GenerationResult(
                            images = images,
                            usage = result.value.usage?.toModel(),
                            durationMs = System.currentTimeMillis() - started
                        )
                    )
                }
            }
            is ApiResult.Failure -> ApiResult.Failure(result.error)
        }
    }

    private suspend fun requestGenerate(
        provider: ProviderInstance,
        params: GenerationParams
    ): ApiResult<ImageGenerationResponse> {
        val body = ImageGenerationRequest(
            model = params.model.ifBlank { provider.modelId },
            prompt = params.prompt,
            n = params.count.coerceIn(1, 10),
            size = params.size,
            quality = params.quality.toApiParam(),
            outputFormat = params.outputFormat.toApiParam(),
            background = null,
            moderation = "auto"
        )
        return postJson(provider, "images/generations", body, ImageGenerationRequest.serializer())
    }

    private suspend fun requestEdit(
        provider: ProviderInstance,
        params: GenerationParams
    ): ApiResult<ImageGenerationResponse> {
        val images = params.referenceImages.map { it.toImageElement() }
        if (images.isEmpty()) {
            return ApiResult.Failure(ApiError.Client("Edit mode requires at least one reference image"))
        }
        val body = ImageEditRequest(
            model = params.model.ifBlank { provider.modelId },
            prompt = params.prompt,
            image = if (images.size == 1) images[0] else buildJsonArray { images.forEach { add(it) } },
            n = params.count.coerceIn(1, 10),
            size = params.size,
            quality = params.quality.toApiParam(),
            outputFormat = params.outputFormat.toApiParam(),
            mask = null
        )
        return postJson(provider, "images/edits", body, ImageEditRequest.serializer())
    }

    private suspend fun <T> postJson(
        provider: ProviderInstance,
        pathSegment: String,
        body: T,
        serializer: kotlinx.serialization.KSerializer<T>
    ): ApiResult<ImageGenerationResponse> {
        val url = buildProviderUrl(provider, pathSegment)
        return try {
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                bearerAuth(provider.apiKey)
                setBody(json.encodeToString(serializer, body))
            }
            val text = response.bodyAsText()
            try {
                ApiResult.Success(json.decodeFromString(ImageGenerationResponse.serializer(), text))
            } catch (e: Exception) {
                ApiResult.Failure(ApiError.Parse("Failed to parse response: ${e.message}"))
            }
        } catch (e: ClientRequestException) {
            ApiResult.Failure(mapClientException(e))
        } catch (e: ServerResponseException) {
            ApiResult.Failure(ApiError.Server("Server error: ${extractMessage(e.response.bodyAsText())}"))
        } catch (e: ConnectTimeoutException) {
            ApiResult.Failure(ApiError.Network("Connection timed out"))
        } catch (e: SocketTimeoutException) {
            ApiResult.Failure(ApiError.Network("Socket timed out"))
        } catch (e: ResponseException) {
            ApiResult.Failure(ApiError.Server("HTTP ${e.response.status.value}"))
        } catch (e: Exception) {
            ApiResult.Failure(ApiError.Unknown(e.message ?: "Unknown error"))
        }
    }

    private fun mapClientException(e: ClientRequestException): ApiError {
        val status = e.response.status.value
        val body = runCatching { e.response.bodyAsText() }.getOrDefault("")
        val message = extractMessage(body)
        return when (status) {
            401, 403 -> ApiError.Auth(message)
            429 -> ApiError.RateLimit(message)
            in 400..499 -> ApiError.Client(message)
            else -> ApiError.Server(message)
        }
    }

    private fun extractMessage(body: String): String {
        if (body.isBlank()) return "Unknown error"
        return runCatching {
            json.decodeFromString(ApiErrorResponse.serializer(), body).error?.message
        }.getOrNull() ?: body.take(200)
    }

    private fun buildProviderUrl(provider: ProviderInstance, pathSegment: String): Url {
        val base = provider.effectiveBaseUrl.trimEnd('/')
        return URLBuilder().takeFrom("$base/$pathSegment").build()
    }

    private fun ImageData.toImageResult(format: ImageOutputFormat): ImageResult? =
        when {
            !b64Json.isNullOrBlank() -> ImageResult(base64 = b64Json, outputFormat = format)
            !url.isNullOrBlank() -> ImageResult(url = url, outputFormat = format)
            else -> null
        }

    private fun ImageQuality.toApiParam(): String? = when (this) {
        ImageQuality.Low -> "low"
        ImageQuality.Medium -> "medium"
        ImageQuality.High -> "high"
        ImageQuality.Auto -> "auto"
    }

    private fun ImageOutputFormat.toApiParam(): String? = when (this) {
        ImageOutputFormat.Png -> "png"
        ImageOutputFormat.Jpeg -> "jpeg"
        ImageOutputFormat.Webp -> "webp"
    }

    private fun String.toImageElement(): JsonElement {
        // Accept either a raw base64 string or a data URI; normalize to data URI.
        val dataUri = if (startsWith("data:")) this else "image/png;base64,$this"
        return JsonPrimitive(dataUri)
    }
}
