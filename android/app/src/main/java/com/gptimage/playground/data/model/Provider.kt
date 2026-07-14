package com.gptimage.playground.data.model

import kotlinx.serialization.Serializable

/**
 * Image provider identifier. Mirrors the web project's ImageProviderId.
 */
typealias ImageProviderId = String

object ImageProviders {
    const val OPENAI: ImageProviderId = "openai"
    const val GOOGLE: ImageProviderId = "google"
    const val SENSENOVA: ImageProviderId = "sensenova"
    const val SEEDREAM: ImageProviderId = "seedream"
    const val STABILITY: ImageProviderId = "stability"

    val ALL: List<ImageProviderId> = listOf(OPENAI, GOOGLE, SEEDREAM, SENSENOVA, STABILITY)

    fun isKnown(value: String): Boolean = value in ALL

    fun label(provider: ImageProviderId): String = when (provider) {
        GOOGLE -> "Google Gemini"
        SENSENOVA -> "SenseNova"
        SEEDREAM -> "Seedream"
        STABILITY -> "Stability AI"
        else -> "OpenAI Compatible"
    }

    fun defaultBaseUrl(provider: ImageProviderId): String = when (provider) {
        SENSENOVA -> "https://token.sensenova.cn/v1"
        SEEDREAM -> "https://ark.cn-beijing.volces.com/api/v3"
        GOOGLE -> "https://generativelanguage.googleapis.com/v1beta"
        STABILITY -> "https://api.stability.ai"
        else -> "https://api.openai.com/v1"
    }
}

@Serializable
data class ImageModelSizePresets(
    val square: String? = null,
    val landscape: String? = null,
    val portrait: String? = null
)

@Serializable
data class ImageModelDefinition(
    val id: String,
    val label: String,
    val provider: ImageProviderId,
    val providerLabel: String,
    val supportsStreaming: Boolean = false,
    val supportsEditing: Boolean = false,
    val supportsMask: Boolean = false,
    val supportsCustomSize: Boolean = false,
    val supportsQuality: Boolean = false,
    val supportsOutputFormat: Boolean = false,
    val supportsBackground: Boolean = false,
    val supportsModeration: Boolean = false,
    val supportsCompression: Boolean = false,
    val sizePresets: ImageModelSizePresets? = null,
    val defaultSize: String? = null,
    val providerOptions: Map<String, String> = emptyMap()
)

/**
 * Built-in image model catalog, mirrors the web project's IMAGE_MODELS list.
 */
object ImageModelCatalog {

    val DEFAULT_MODEL_ID: String = "gpt-image-2"

    private val GPT_IMAGE_SIZES = ImageModelSizePresets(
        square = "1024x1024",
        landscape = "1536x1024",
        portrait = "1024x1536"
    )

    val MODELS: List<ImageModelDefinition> = listOf(
        ImageModelDefinition(
            id = "gpt-image-2",
            label = "gpt-image-2",
            provider = ImageProviders.OPENAI,
            providerLabel = "OpenAI",
            supportsStreaming = true,
            supportsEditing = true,
            supportsMask = true,
            supportsCustomSize = true,
            supportsQuality = true,
            supportsOutputFormat = true,
            supportsModeration = true,
            supportsCompression = true,
            sizePresets = GPT_IMAGE_SIZES
        ),
        ImageModelDefinition(
            id = "gpt-image-1.5",
            label = "gpt-image-1.5",
            provider = ImageProviders.OPENAI,
            providerLabel = "OpenAI",
            supportsStreaming = true,
            supportsEditing = true,
            supportsMask = true,
            supportsQuality = true,
            supportsOutputFormat = true,
            supportsBackground = true,
            supportsModeration = true,
            supportsCompression = true,
            sizePresets = GPT_IMAGE_SIZES
        ),
        ImageModelDefinition(
            id = "gpt-image-1",
            label = "gpt-image-1",
            provider = ImageProviders.OPENAI,
            providerLabel = "OpenAI",
            supportsStreaming = true,
            supportsEditing = true,
            supportsMask = true,
            supportsQuality = true,
            supportsOutputFormat = true,
            supportsBackground = true,
            supportsModeration = true,
            supportsCompression = true,
            sizePresets = GPT_IMAGE_SIZES
        ),
        ImageModelDefinition(
            id = "gpt-image-1-mini",
            label = "gpt-image-1-mini",
            provider = ImageProviders.OPENAI,
            providerLabel = "OpenAI",
            supportsStreaming = true,
            supportsEditing = true,
            supportsMask = true,
            supportsQuality = true,
            supportsOutputFormat = true,
            supportsBackground = true,
            supportsModeration = true,
            supportsCompression = true,
            sizePresets = GPT_IMAGE_SIZES
        ),
        ImageModelDefinition(
            id = "gemini-3.1-flash-image-preview",
            label = "Gemini Nano Banana 2",
            provider = ImageProviders.GOOGLE,
            providerLabel = "Google",
            supportsEditing = true,
            sizePresets = ImageModelSizePresets(
                square = "1024x1024",
                landscape = "1264x848",
                portrait = "848x1264"
            )
        ),
        ImageModelDefinition(
            id = "gemini-3-pro-image-preview",
            label = "Gemini Nano Banana Pro",
            provider = ImageProviders.GOOGLE,
            providerLabel = "Google",
            supportsEditing = true,
            sizePresets = ImageModelSizePresets(
                square = "1:1",
                landscape = "16:9",
                portrait = "9:16"
            )
        ),
        ImageModelDefinition(
            id = "gemini-3.1-flash-lite-image",
            label = "Gemini Nano Banana 2 Lite",
            provider = ImageProviders.GOOGLE,
            providerLabel = "Google",
            supportsEditing = true,
            sizePresets = ImageModelSizePresets(
                square = "1:1",
                landscape = "16:9",
                portrait = "9:16"
            )
        ),
        ImageModelDefinition(
            id = "sensenova-u1-fast",
            label = "SenseNova U1 Fast",
            provider = ImageProviders.SENSENOVA,
            providerLabel = "SenseNova",
            supportsCustomSize = true,
            defaultSize = "2752x1536",
            sizePresets = ImageModelSizePresets(
                square = "2048x2048",
                landscape = "2752x1536",
                portrait = "1536x2752"
            )
        ),
        ImageModelDefinition(
            id = "doubao-seedream-5.0-lite",
            label = "Doubao Seedream 5.0 Lite",
            provider = ImageProviders.SEEDREAM,
            providerLabel = "Seedream",
            supportsEditing = true,
            supportsCustomSize = true,
            supportsOutputFormat = true,
            defaultSize = "2K",
            sizePresets = ImageModelSizePresets(
                square = "2048x2048",
                landscape = "2560x1440",
                portrait = "1440x2560"
            ),
            providerOptions = mapOf("response_format" to "url", "watermark" to "false")
        ),
        ImageModelDefinition(
            id = "doubao-seedream-5-0-260128",
            label = "Doubao Seedream 5.0",
            provider = ImageProviders.SEEDREAM,
            providerLabel = "Seedream",
            supportsEditing = true,
            supportsCustomSize = true,
            supportsOutputFormat = true,
            defaultSize = "2K",
            sizePresets = ImageModelSizePresets(
                square = "2048x2048",
                landscape = "2560x1440",
                portrait = "1440x2560"
            ),
            providerOptions = mapOf("response_format" to "url", "watermark" to "false")
        ),
        ImageModelDefinition(
            id = "doubao-seedream-4.5",
            label = "Doubao Seedream 4.5",
            provider = ImageProviders.SEEDREAM,
            providerLabel = "Seedream",
            supportsEditing = true,
            supportsCustomSize = true,
            defaultSize = "2K",
            sizePresets = ImageModelSizePresets(
                square = "2048x2048",
                landscape = "2560x1440",
                portrait = "1440x2560"
            ),
            providerOptions = mapOf("response_format" to "url", "watermark" to "false")
        ),
        ImageModelDefinition(
            id = "doubao-seedream-4.0-250828",
            label = "Doubao Seedream 4.0",
            provider = ImageProviders.SEEDREAM,
            providerLabel = "Seedream",
            supportsEditing = true,
            supportsCustomSize = true,
            defaultSize = "2K",
            sizePresets = ImageModelSizePresets(
                square = "2048x2048",
                landscape = "2560x1440",
                portrait = "1440x2560"
            ),
            providerOptions = mapOf("response_format" to "url", "watermark" to "false")
        ),
        ImageModelDefinition(
            id = "doubao-seedream-3.0-t2i",
            label = "Doubao Seedream 3.0 T2I",
            provider = ImageProviders.SEEDREAM,
            providerLabel = "Seedream",
            supportsCustomSize = true,
            defaultSize = "2K",
            sizePresets = ImageModelSizePresets(
                square = "2048x2048",
                landscape = "2560x1440",
                portrait = "1440x2560"
            )
        ),
        ImageModelDefinition(
            id = "sd-3.5-large",
            label = "Stable Diffusion 3.5 Large",
            provider = ImageProviders.STABILITY,
            providerLabel = "Stability AI",
            supportsCustomSize = true,
            supportsOutputFormat = true,
            defaultSize = "1:1",
            sizePresets = ImageModelSizePresets(
                square = "1:1",
                landscape = "16:9",
                portrait = "9:16"
            )
        ),
        ImageModelDefinition(
            id = "sd-3.5-large-turbo",
            label = "Stable Diffusion 3.5 Large Turbo",
            provider = ImageProviders.STABILITY,
            providerLabel = "Stability AI",
            supportsCustomSize = true,
            supportsOutputFormat = true,
            defaultSize = "1:1",
            sizePresets = ImageModelSizePresets(
                square = "1:1",
                landscape = "16:9",
                portrait = "9:16"
            )
        )
    )

    val MODEL_IDS: List<String> = MODELS.map { it.id }

    fun byId(id: String): ImageModelDefinition? = MODELS.firstOrNull { it.id == id }

    fun inferProviderFromModelId(model: String): ImageProviderId {
        val normalized = model.lowercase()
        return when {
            normalized.startsWith("gemini-") -> ImageProviders.GOOGLE
            normalized.startsWith("sensenova-") -> ImageProviders.SENSENOVA
            normalized.startsWith("doubao-seedream-") || normalized.startsWith("doubao-seededit-") -> ImageProviders.SEEDREAM
            normalized.startsWith("sd-") || normalized.startsWith("stable-") -> ImageProviders.STABILITY
            else -> ImageProviders.OPENAI
        }
    }

    fun groupByProvider(): List<Pair<ImageProviderId, List<ImageModelDefinition>>> {
        return ImageProviders.ALL.mapNotNull { provider ->
            val models = MODELS.filter { it.provider == provider }
            if (models.isEmpty()) null else provider to models
        }
    }
}
