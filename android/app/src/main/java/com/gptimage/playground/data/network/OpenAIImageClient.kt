package com.gptimage.playground.data.network

import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.GeneratedImage
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Base64

/**
 * OpenAI-compatible image generation client.
 *
 * Supports the standard OpenAI `/images/generations` (JSON) and `/images/edits` (multipart) endpoints,
 * as well as the JSON-mode edit request that providers like Seedream use (where the edit is sent as a
 * regular `/images/generations` call with an `image` field set to a data URI or array of data URIs).
 */
class OpenAIImageClient(
    private val httpClient: OkHttpClient = HttpFactory.client,
    private val json: Json = defaultJson
) {

    suspend fun generate(request: GenerationRequest, apiKey: String, baseUrl: String): GenerationResult {
        val url = normalizedBaseUrl(baseUrl).newBuilder()
            .addPathSegments("images/generations")
            .build()

        val body = buildGenerationsBody(request)
        val req = buildJsonRequest(url.toString(), apiKey, body)
        val response = execute(req)
        return parseOpenAIResponse(response, request.outputFormat ?: ImageOutputFormats.PNG)
    }

    /**
     * Edit using either multipart (default OpenAI behaviour) or JSON mode (Seedream-like).
     */
    suspend fun edit(request: EditRequest, apiKey: String, baseUrl: String): GenerationResult {
        val useJsonMode = request.providerOptions["editRequestMode"] == "generations-json" ||
            request.model.provider == ImageProviders.SEEDREAM

        return if (useJsonMode) editJson(request, apiKey, baseUrl) else editMultipart(request, apiKey, baseUrl)
    }

    /**
     * 流式生成（OpenAI `gpt-image-*` 系列支持）。返回 [StreamEvent] 流，消费端可通过取消 Flow 来中止。
     *
     * 上游使用 `stream: true` + `partial_images: [partialImages]`，响应为 `text/event-stream`：
     *  - `event: image_generation.partial_image` / `event: image_generation.completed`
     *  - 或 data-only 模式：`data: {"type": "...", ...}` 直接带 type
     */
    fun generateStream(
        request: GenerationRequest,
        apiKey: String,
        baseUrl: String,
        partialImages: Int = 2
    ): Flow<StreamEvent> {
        val url = normalizedBaseUrl(baseUrl).newBuilder()
            .addPathSegments("images/generations")
            .build()
        val body = buildStreamBody(request, partialImages)
        val req = buildStreamRequest(url.toString(), apiKey, body)
        val outputFormat = request.outputFormat ?: ImageOutputFormats.PNG
        return sseFlow(req, outputFormat)
    }

    /**
     * 流式编辑（仅 OpenAI multipart 编辑路径支持流式）。
     * 上游事件类型为 `image_edit.partial_image` / `image_edit.completed`，统一映射到 [StreamEvent]。
     */
    fun editStream(
        request: EditRequest,
        apiKey: String,
        baseUrl: String,
        partialImages: Int = 2
    ): Flow<StreamEvent> {
        val url = normalizedBaseUrl(baseUrl).newBuilder()
            .addPathSegments("images/edits")
            .build()

        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("model", request.model.id)
            .addFormDataPart("prompt", request.prompt)
            .addFormDataPart("n", request.n.toString())
            .addFormDataPart("stream", "true")
            .addFormDataPart("partial_images", partialImages.coerceIn(1, 3).toString())

        request.size?.let { builder.addFormDataPart("size", it) }
        request.quality?.let { builder.addFormDataPart("quality", it) }

        request.referenceImages.forEachIndexed { index, image ->
            val filename = if (request.referenceImages.size > 1) {
                "${image.name.ifBlank { "image" }}_$index.png"
            } else {
                image.name.ifBlank { "image.png" }
            }
            val mediaType = image.mimeType.toMediaType()
            val partBody = image.data.toRequestBody(mediaType)
            val partName = if (request.referenceImages.size > 1) "image[]" else "image"
            builder.addFormDataPart(partName, filename, partBody)
        }

        request.mask?.let { mask ->
            val mediaType = mask.mimeType.toMediaType()
            builder.addFormDataPart("mask", mask.name.ifBlank { "mask.png" }, mask.data.toRequestBody(mediaType))
        }

        request.providerOptions
            .filterKeys { it !in ignoredMultipartProviderOptions }
            .forEach { (k, v) -> builder.addFormDataPart(k, v) }

        val req = Request.Builder()
            .url(url.toString())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(builder.build())
            .build()

        // 编辑路径上游总是返回 PNG（与 Web 端约定一致）
        return sseFlow(req, ImageOutputFormats.PNG)
    }

    /**
     * 共用的 SSE 读取 + 解析 Flow。使用 [callbackFlow] + [awaitClose] 让消费端可以随时取消：
     * 取消时通过 `call.cancel()` 主动中断阻塞的 socket read。
     */
    private fun sseFlow(request: Request, fallbackFormat: String): Flow<StreamEvent> = callbackFlow {
        val call = httpClient.newCall(request)

        launch(Dispatchers.IO) {
            var response: Response? = null
            try {
                response = call.execute()
                if (!response.isSuccessful) {
                    val raw = response.body?.string().orEmpty()
                    val ex = ProviderException.fromHttp(response.code, raw)
                    trySend(StreamEvent.Error(ex.message ?: "HTTP ${response.code}", ex.kind))
                    close()
                    return@launch
                }
                val source = response.body?.source()
                if (source == null) {
                    trySend(StreamEvent.Error("Empty response body", ProviderException.Kind.PARSE))
                    close()
                    return@launch
                }

                var currentEvent: String? = null
                val dataBuffer = StringBuilder()

                while (true) {
                    val line = try {
                        source.readUtf8Line() ?: break
                    } catch (e: IOException) {
                        // socket 被取消或网络中断
                        return@launch
                    }
                    when {
                        line.isEmpty() -> {
                            // event boundary
                            if (dataBuffer.isNotEmpty()) {
                                val parsed = parseSseBlock(currentEvent, dataBuffer.toString(), fallbackFormat)
                                if (parsed != null) {
                                    trySend(parsed)
                                    if (parsed is StreamEvent.Error) {
                                        close()
                                        return@launch
                                    }
                                }
                                dataBuffer.setLength(0)
                                currentEvent = null
                            }
                        }
                        line.startsWith(":") -> {
                            // SSE comment / heartbeat, ignore
                        }
                        line.startsWith("event:") -> {
                            currentEvent = line.substringAfter(":").trim()
                        }
                        line.startsWith("data:") -> {
                            val data = line.substringAfter(":").trim()
                            if (dataBuffer.isNotEmpty()) dataBuffer.append('\n')
                            dataBuffer.append(data)
                        }
                        else -> {
                            // 未知行，忽略
                        }
                    }
                }

                // flush remaining buffer
                if (dataBuffer.isNotEmpty()) {
                    val parsed = parseSseBlock(currentEvent, dataBuffer.toString(), fallbackFormat)
                    if (parsed != null) {
                        trySend(parsed)
                    }
                }
                close()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: IOException) {
                trySend(StreamEvent.Error(e.message ?: "Network error", ProviderException.Kind.NETWORK))
                close()
            } catch (e: Throwable) {
                trySend(StreamEvent.Error(e.message ?: "Unknown error", ProviderException.Kind.UNKNOWN))
                close()
            } finally {
                response?.close()
            }
        }

        awaitClose {
            // 消费端取消 Flow 时主动 cancel OkHttp call，让阻塞的 readUtf8Line 抛 IOException
            call.cancel()
        }
    }

    /**
     * 解析单个 SSE 事件块。兼容三种事件命名：
     *  - 标准 OpenAI 上游：`image_generation.partial_image` / `image_generation.completed`
     *  - 编辑路径：`image_edit.partial_image` / `image_edit.completed`
     *  - data-only 模式：data JSON 里直接带 `type`
     */
    private fun parseSseBlock(
        event: String?,
        data: String,
        fallbackFormat: String
    ): StreamEvent? {
        if (data.isBlank() || data == "[DONE]") return null
        val obj = try {
            json.parseToJsonElement(data).jsonObject
        } catch (e: Exception) {
            return null
        }

        // 上游直接返回 {"error": "..."} 或 {"error": {"message": "..."}}
        obj["error"]?.let { errEl ->
            val msg = try {
                errEl.jsonObject["message"]?.jsonPrimitive?.content
                    ?: errEl.jsonPrimitive?.content
                    ?: "Stream error"
            } catch (e: Exception) {
                "Stream error"
            }
            return StreamEvent.Error(msg, ProviderException.Kind.SERVER)
        }

        val type = event ?: obj["type"]?.jsonPrimitive?.content
        val b64 = obj["b64_json"]?.jsonPrimitive?.content
        val imageIndex = obj["index"]?.jsonPrimitive?.intOrNull ?: 0
        val partialIdx = obj["partial_image_index"]?.jsonPrimitive?.intOrNull ?: 0
        val outputFormat = obj["output_format"]?.jsonPrimitive?.content ?: fallbackFormat
        val usage = obj["usage"]?.jsonObject?.let { u ->
            val inputDetails = u["input_tokens_details"]?.jsonObject
            ProviderUsage(
                inputTextTokens = inputDetails?.get("text_tokens")?.jsonPrimitive?.intOrNull,
                inputImageTokens = inputDetails?.get("image_tokens")?.jsonPrimitive?.intOrNull,
                outputTokens = u["output_tokens"]?.jsonPrimitive?.intOrNull
            )
        }

        return when (type) {
            "image_generation.partial_image",
            "image_edit.partial_image" ->
                if (b64 != null) StreamEvent.PartialImage(b64, imageIndex, partialIdx) else null

            "image_generation.completed",
            "image_edit.completed" ->
                if (b64 != null) StreamEvent.CompletedImage(b64, imageIndex, outputFormat, usage) else null

            "error" -> {
                val msg = obj["message"]?.jsonPrimitive?.content ?: "Stream error"
                StreamEvent.Error(msg, ProviderException.Kind.SERVER)
            }

            else -> null
        }
    }

    private fun buildStreamBody(request: GenerationRequest, partialImages: Int): String {
        val obj = buildJsonObject {
            put("model", request.model.id)
            put("prompt", request.prompt)
            put("n", request.n.coerceAtLeast(1))
            put("stream", true)
            put("partial_images", partialImages.coerceIn(1, 3))
            request.size?.let { put("size", it) }
            request.quality?.let { put("quality", it) }
            request.outputFormat?.let { put("output_format", it) }
            request.outputCompression?.let { put("output_compression", it) }
            request.background?.let { put("background", it) }
            request.moderation?.let { put("moderation", it) }
            request.providerOptions.forEach { (k, v) -> put(k, v) }
        }
        return obj.toString()
    }

    private fun buildStreamRequest(url: String, apiKey: String, body: String): Request {
        val mediaType = "application/json".toMediaType()
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .post(body.toRequestBody(mediaType))
            .build()
    }

    private suspend fun editMultipart(request: EditRequest, apiKey: String, baseUrl: String): GenerationResult {
        val url = normalizedBaseUrl(baseUrl).newBuilder()
            .addPathSegments("images/edits")
            .build()

        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("model", request.model.id)
            .addFormDataPart("prompt", request.prompt)
            .addFormDataPart("n", request.n.toString())

        request.size?.let { builder.addFormDataPart("size", it) }
        request.quality?.let { builder.addFormDataPart("quality", it) }

        request.referenceImages.forEachIndexed { index, image ->
            val filename = if (request.referenceImages.size > 1) {
                "${image.name.ifBlank { "image" }}_$index.png"
            } else {
                image.name.ifBlank { "image.png" }
            }
            val mediaType = image.mimeType.toMediaType()
            val body = image.data.toRequestBody(mediaType)
            val partName = if (request.referenceImages.size > 1) "image[]" else "image"
            builder.addFormDataPart(partName, filename, body)
        }

        request.mask?.let { mask ->
            val mediaType = mask.mimeType.toMediaType()
            builder.addFormDataPart("mask", mask.name.ifBlank { "mask.png" }, mask.data.toRequestBody(mediaType))
        }

        request.providerOptions
            .filterKeys { it !in ignoredMultipartProviderOptions }
            .forEach { (k, v) -> builder.addFormDataPart(k, v) }

        val req = Request.Builder()
            .url(url.toString())
            .header("Authorization", "Bearer $apiKey")
            .post(builder.build())
            .build()

        val response = execute(req)
        return parseOpenAIResponse(response, ImageOutputFormats.PNG)
    }

    private suspend fun editJson(request: EditRequest, apiKey: String, baseUrl: String): GenerationResult {
        val url = normalizedBaseUrl(baseUrl).newBuilder()
            .addPathSegments("images/generations")
            .build()

        val imageInputs = buildJsonArray {
            request.referenceImages.forEach { image ->
                val dataUri = "data:${image.mimeType};base64,${Base64.getEncoder().encodeToString(image.data)}"
                add(JsonPrimitive(dataUri))
            }
        }
        val imageFieldValue: Any = if (imageInputs.size == 1) imageInputs.first() else imageInputs

        val body = buildJsonObject {
            put("model", request.model.id)
            put("prompt", request.prompt)
            put("n", request.n)
            request.size?.let { put("size", it) }
            request.quality?.let { put("quality", it) }
            request.providerOptions
                .filterKeys { it !in ignoredJsonProviderOptions }
                .forEach { (k, v) -> put(k, v) }
            when (imageFieldValue) {
                is JsonPrimitive -> put("image", imageFieldValue)
                is JsonArray -> put("image", imageFieldValue)
            }
        }

        val req = buildJsonRequest(url.toString(), apiKey, body.toString())
        val response = execute(req)
        val outputFormat = request.providerOptions["output_format"]
            ?.let { if (ImageOutputFormats.isKnown(it)) it else ImageOutputFormats.JPEG }
            ?: ImageOutputFormats.JPEG
        return parseOpenAIResponse(response, outputFormat)
    }

    private fun buildGenerationsBody(request: GenerationRequest): String {
        val obj = buildJsonObject {
            put("model", request.model.id)
            put("prompt", request.prompt)
            put("n", request.n.coerceAtLeast(1))
            request.size?.let { put("size", it) }
            request.quality?.let { put("quality", it) }
            request.outputFormat?.let { put("output_format", it) }
            request.outputCompression?.let { put("output_compression", it) }
            request.background?.let { put("background", it) }
            request.moderation?.let { put("moderation", it) }
            request.providerOptions.forEach { (k, v) -> put(k, v) }
        }
        return obj.toString()
    }

    private fun buildJsonRequest(url: String, apiKey: String, body: String): Request {
        val mediaType = "application/json".toMediaType()
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(body.toRequestBody(mediaType))
            .build()
    }

    private fun parseOpenAIResponse(response: Response, outputFormat: String): GenerationResult {
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw ProviderException.fromHttp(response.code, raw)
        }
        val root = json.parseToJsonElement(raw).jsonObject
        val dataArray = root["data"]?.jsonArray ?: emptyList()
        val images = dataArray.mapNotNull { item ->
            val obj = item.jsonObject
            val b64 = obj["b64_json"]?.jsonPrimitive?.content
            val url = obj["url"]?.jsonPrimitive?.content
            if (b64 != null || url != null) {
                GeneratedImage(b64Json = b64, url = url, outputFormat = outputFormat)
            } else null
        }
        val usage = root["usage"]?.jsonObject?.let { u ->
            val inputDetails = u["input_tokens_details"]?.jsonObject
            ProviderUsage(
                inputTextTokens = inputDetails?.get("text_tokens")?.jsonPrimitive?.intOrNull,
                inputImageTokens = inputDetails?.get("image_tokens")?.jsonPrimitive?.intOrNull,
                outputTokens = u["output_tokens"]?.jsonPrimitive?.intOrNull
            )
        }
        return GenerationResult(images = images, usage = usage, rawResponse = raw)
    }

    private fun execute(req: Request): Response {
        return try {
            httpClient.newCall(req).execute()
        } catch (e: IOException) {
            throw ProviderException(ProviderException.Kind.NETWORK, e.message ?: "Network error", cause = e)
        }
    }

    private fun normalizedBaseUrl(baseUrl: String): HttpUrl {
        val trimmed = baseUrl.trim()
        val withProtocol = if (trimmed.matches(Regex("^[a-z][a-z0-9+.-]*://.*", RegexOption.IGNORE_CASE))) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val parsed = withProtocol.toHttpUrl()
            .newBuilder()
            .username("")
            .password("")
            .query(null)
            .fragment(null)
            .build()
        val path = parsed.encodedPath.trimEnd('/')
        val finalPath = if (path.isEmpty()) "/v1" else path
        return parsed.newBuilder().encodedPath(finalPath).build()
    }

    companion object {
        private val defaultJson = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

        private val ignoredMultipartProviderOptions = setOf(
            "editRequestMode", "response_format", "output_format", "n", "size", "quality",
            "watermark", "image", "mask"
        )

        private val ignoredJsonProviderOptions = setOf(
            "editRequestMode", "n", "size", "quality", "image", "mask"
        )
    }
}
