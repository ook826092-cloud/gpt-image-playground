package com.gptimage.playground.data.network

import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.GeneratedImage
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ProviderUsage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Base64

/**
 * Stability AI image generation client.
 *
 * Targets the v2beta Stable Image generation endpoint (`/v2beta/stable-image/generate/sd3`),
 * which accepts `multipart/form-data` and returns the image as base64 inside a JSON envelope
 * when `Accept: application/json` is requested.
 *
 * Only text-to-image is supported; the SD3 generate endpoint does not perform reference-based
 * editing, so [ImageProviderService] rejects edit requests for Stability models.
 *
 * Size handling: the catalog stores aspect ratios (e.g. "1:1", "16:9", "9:16") in the size
 * presets; these are forwarded as the `aspect_ratio` form field. Pixel "WxH" values are ignored.
 */
class StabilityImageClient(
    private val httpClient: OkHttpClient = HttpFactory.client,
    private val json: Json = defaultJson
) {

    suspend fun generate(request: GenerationRequest, apiKey: String, baseUrl: String): GenerationResult {
        val url = buildEndpointUrl(baseUrl)
        val body = buildMultipartBody(request)
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(body)
            .build()
        val response = execute(req)
        return parseResponse(response, request)
    }

    private fun buildEndpointUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().let {
            if (it.matches(Regex("^[a-z][a-z0-9+.-]*://.*", RegexOption.IGNORE_CASE))) it else "https://$it"
        }.toHttpUrl()
            .newBuilder()
            .username("")
            .password("")
            .query(null)
            .fragment(null)
            .build()
        val path = trimmed.encodedPath.trimEnd('/')
        val finalPath = if (path.isEmpty()) "/v2beta" else path
        return trimmed.newBuilder()
            .encodedPath("$finalPath/stable-image/generate/sd3")
            .build()
            .toString()
    }

    private fun buildMultipartBody(request: GenerationRequest): MultipartBody {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("prompt", request.prompt)
            .addFormDataPart("model", request.model.id)
            .addFormDataPart("mode", "text-to-image")

        request.size
            ?.takeIf { it.matches(ASPECT_RATIO_REGEX) }
            ?.let { builder.addFormDataPart("aspect_ratio", it) }

        val outputFormat = request.outputFormat
            ?: request.model.providerOptions["output_format"]
            ?: ImageOutputFormats.PNG
        if (outputFormat in ImageOutputFormats.ALL) {
            builder.addFormDataPart("output_format", outputFormat)
        }

        request.model.providerOptions["negative_prompt"]?.let { builder.addFormDataPart("negative_prompt", it) }
        request.model.providerOptions["style_preset"]?.let { builder.addFormDataPart("style_preset", it) }
        request.model.providerOptions["seed"]?.let { builder.addFormDataPart("seed", it) }
        request.model.providerOptions["cfg_scale"]?.let { builder.addFormDataPart("cfg_scale", it) }

        return builder.build()
    }

    private fun parseResponse(response: Response, request: GenerationRequest): GenerationResult {
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw ProviderException.fromHttp(response.code, raw)
        }
        val root = json.parseToJsonElement(raw).jsonObject
        val b64 = root["image"]?.jsonPrimitive?.content
            ?: throw ProviderException(
                kind = ProviderException.Kind.PARSE,
                message = "Stability response missing image field",
                rawBody = raw.take(500)
            )
        val format = (request.outputFormat ?: ImageOutputFormats.PNG)
            .let { if (ImageOutputFormats.isKnown(it)) it else ImageOutputFormats.PNG }
        val image = GeneratedImage(b64Json = b64, outputFormat = format)
        val finishReason = root["finish_reason"]?.jsonPrimitive?.content
        if (finishReason != null && finishReason != "SUCCESS" && finishReason != "CONTENT_FILTERED") {
            // Still return the image when present; surface the reason only when no image is available.
        }
        return GenerationResult(images = listOf(image), usage = ProviderUsage(), rawResponse = raw)
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
