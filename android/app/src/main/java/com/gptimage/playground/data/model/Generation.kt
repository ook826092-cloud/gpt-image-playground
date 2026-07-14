package com.gptimage.playground.data.model

import kotlinx.serialization.Serializable

/**
 * Parameters for an image generation (text-to-image) request.
 */
data class GenerationRequest(
    val model: ImageModelDefinition,
    val prompt: String,
    val n: Int = 1,
    val size: String? = null,
    val quality: ImageQuality? = null,
    val outputFormat: ImageOutputFormat? = null,
    val outputCompression: Int? = null,
    val background: ImageBackground? = null,
    val moderation: ImageModeration? = null,
    val providerOptions: Map<String, String> = emptyMap()
)

/**
 * Parameters for an image edit request (with reference images).
 */
data class EditRequest(
    val model: ImageModelDefinition,
    val prompt: String,
    val referenceImages: List<ReferenceImage>,
    val mask: ReferenceImage? = null,
    val n: Int = 1,
    val size: String? = null,
    val quality: ImageQuality? = null,
    val providerOptions: Map<String, String> = emptyMap()
)

/**
 * A reference image to attach to an edit request. The bytes are in-memory and not serialized.
 */
data class ReferenceImage(
    val name: String,
    val mimeType: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferenceImage) return false
        return name == other.name && mimeType == other.mimeType && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * A single image returned from a generation/edit request.
 */
@Serializable
data class GeneratedImage(
    /** Base64-encoded image bytes (without data URI prefix). */
    val b64Json: String? = null,
    /** Remote URL when the provider returned a URL. */
    val url: String? = null,
    val outputFormat: ImageOutputFormat = ImageOutputFormats.PNG
)

/**
 * Result of a generation/edit request.
 */
@Serializable
data class GenerationResult(
    val images: List<GeneratedImage>,
    val usage: ProviderUsage? = null,
    val rawResponse: String? = null
)

@Serializable
data class ProviderUsage(
    val inputTextTokens: Int? = null,
    val inputImageTokens: Int? = null,
    val outputTokens: Int? = null
)
