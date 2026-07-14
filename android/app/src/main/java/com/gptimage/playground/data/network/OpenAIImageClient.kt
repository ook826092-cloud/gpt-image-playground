package com.gptimage.playground.data.network

import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.GeneratedImage
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderUsage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
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
