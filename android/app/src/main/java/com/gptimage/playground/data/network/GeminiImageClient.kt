package com.gptimage.playground.data.network

import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.GeneratedImage
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ProviderUsage
import com.gptimage.playground.data.model.ReferenceImage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Base64

/**
 * Google Gemini image generation client.
 *
 * Uses the v1beta generateContent endpoint with the API key passed as a `?key=` query parameter.
 * Both text-to-image and image editing are sent as `generateContent` requests with parts that
 * include the prompt text and any reference image bytes (base64 inline data).
 */
class GeminiImageClient(
    private val httpClient: OkHttpClient = HttpFactory.client,
    private val json: Json = defaultJson
) {

    suspend fun generate(request: GenerationRequest, apiKey: String, baseUrl: String): GenerationResult {
        val url = buildEndpointUrl(baseUrl, request.model.id, apiKey)
        val body = buildBody(
            prompt = request.prompt,
            references = emptyList(),
            size = request.size,
            providerOptions = request.model.providerOptions
        )
        val req = buildJsonRequest(url, body)
        val response = execute(req)
        return parseGeminiResponse(response)
    }

    suspend fun edit(request: EditRequest, apiKey: String, baseUrl: String): GenerationResult {
        val url = buildEndpointUrl(baseUrl, request.model.id, apiKey)
        val body = buildBody(
            prompt = request.prompt,
            references = request.referenceImages,
            size = request.size,
            providerOptions = request.model.providerOptions
        )
        val req = buildJsonRequest(url, body)
        val response = execute(req)
        return parseGeminiResponse(response)
    }

    private fun buildEndpointUrl(baseUrl: String, model: String, apiKey: String): String {
        val trimmed = baseUrl.trim().let {
            if (it.matches(Regex("^[a-z][a-z0-9+.-]*://.*", RegexOption.IGNORE_CASE))) it else "https://$it"
        }.toHttpUrl()
            .newBuilder()
            .username("")
            .password("")
            .fragment(null)
            .build()
        val path = trimmed.encodedPath.trimEnd('/')
        val finalPath = if (path.isEmpty()) "/v1beta" else path
        return trimmed.newBuilder()
            .encodedPath("$finalPath/models/$model:generateContent")
            .addQueryParameter("key", apiKey)
            .build()
            .toString()
    }

    private fun buildBody(
        prompt: String,
        references: List<ReferenceImage>,
        size: String? = null,
        providerOptions: Map<String, String> = emptyMap()
    ): String {
        val parts = buildJsonArray {
            add(buildJsonObject { put("text", prompt) })
            references.forEach { image ->
                val dataUri = Base64.getEncoder().encodeToString(image.data)
                add(
                    buildJsonObject {
                        put(
                            "inline_data",
                            buildJsonObject {
                                put("mime_type", image.mimeType)
                                put("data", dataUri)
                            }
                        )
                    }
                )
            }
        }
        return buildJsonObject {
            put(
                "contents",
                buildJsonArray {
                    add(buildJsonObject { put("parts", parts) })
                }
            )
            // Only forward aspectRatio when the size looks like "W:H"; legacy "WxH" presets
            // are ignored to avoid breaking the original Nano Banana 2 model catalog entry.
            val aspectRatio = size?.takeIf { it.matches(ASPECT_RATIO_REGEX) }
            val imageSize = providerOptions["imageSize"]
            if (aspectRatio != null || imageSize != null) {
                put(
                    "generationConfig",
                    buildJsonObject {
                        put("responseModalities", buildJsonArray { add(JsonPrimitive("IMAGE")) })
                        put(
                            "imageConfig",
                            buildJsonObject {
                                if (aspectRatio != null) put("aspectRatio", aspectRatio)
                                if (imageSize != null) put("imageSize", imageSize)
                            }
                        )
                    }
                )
            }
        }.toString()
    }

    private fun buildJsonRequest(url: String, body: String): Request {
        val mediaType = "application/json".toMediaType()
        return Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(body.toRequestBody(mediaType))
            .build()
    }

    private fun parseGeminiResponse(response: Response): GenerationResult {
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw ProviderException.fromHttp(response.code, raw)
        }
        val root = json.parseToJsonElement(raw).jsonObject
        val candidates = root["candidates"]?.jsonArray ?: emptyList()
        val images = candidates.flatMap { candidate ->
            val parts = candidate.jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray ?: emptyList()
            parts.mapNotNull { part ->
                val inline = part.jsonObject["inline_data"] as? JsonObject ?: return@mapNotNull null
                val mimeType = inline["mime_type"]?.jsonPrimitive?.content ?: "image/png"
                val data = inline["data"]?.jsonPrimitive?.content
                if (data != null) {
                    val format = when (mimeType) {
                        "image/jpeg" -> ImageOutputFormats.JPEG
                        "image/webp" -> ImageOutputFormats.WEBP
                        else -> ImageOutputFormats.PNG
                    }
                    GeneratedImage(b64Json = data, outputFormat = format)
                } else null
            }
        }
        val usage = root["usageMetadata"]?.jsonObject?.let { u ->
            ProviderUsage(
                inputTextTokens = u["promptTokenCount"]?.jsonPrimitive?.intOrNull,
                outputTokens = u["candidatesTokenCount"]?.jsonPrimitive?.intOrNull
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

    companion object {
        private val ASPECT_RATIO_REGEX = Regex("^\\d+:\\d+$")

        private val defaultJson = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
    }
}
