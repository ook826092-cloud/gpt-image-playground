package com.gptimage.playground.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    val quality: String? = null,
    @SerialName("output_format")
    val outputFormat: String? = null,
    @SerialName("response_format")
    val responseFormat: String? = null,
    @SerialName("background")
    val background: String? = null,
    @SerialName("moderation")
    val moderation: String? = null
)

@Serializable
data class ImageEditRequest(
    val model: String,
    val prompt: String,
    val image: JsonElement,
    val n: Int = 1,
    val size: String = "1024x1024",
    val quality: String? = null,
    @SerialName("output_format")
    val outputFormat: String? = null,
    val mask: String? = null
)

@Serializable
data class ImageGenerationResponse(
    val data: List<ImageData>? = null,
    val usage: ImageUsage? = null
)

@Serializable
data class ImageData(
    @SerialName("b64_json")
    val b64Json: String? = null,
    val url: String? = null,
    @SerialName("revised_prompt")
    val revisedPrompt: String? = null
)

@Serializable
data class ImageUsage(
    @SerialName("total_tokens")
    val totalTokens: Long = 0L,
    @SerialName("input_tokens")
    val inputTokens: Long = 0L,
    @SerialName("output_tokens")
    val outputTokens: Long = 0L
)

/** Error payload returned by OpenAI-compatible endpoints. */
@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail? = null
)

@Serializable
data class ApiErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
