package com.gptimage.playground.data.model

import kotlinx.serialization.Serializable

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class AppLanguage {
    /** Follow the system locale. */
    SYSTEM,
    SIMPLIFIED_CHINESE,
    ENGLISH
}

@Serializable
data class ProviderCredentials(
    val apiKey: String = "",
    val baseUrl: String = ""
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}

@Serializable
data class AppConfig(
    val openai: ProviderCredentials = ProviderCredentials(),
    val google: ProviderCredentials = ProviderCredentials(),
    val sensenova: ProviderCredentials = ProviderCredentials(),
    val seedream: ProviderCredentials = ProviderCredentials(),
    val stability: ProviderCredentials = ProviderCredentials(),
    val defaultModelId: String = ImageModelCatalog.DEFAULT_MODEL_ID,
    val defaultSize: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM
) {
    fun credentialsFor(provider: ImageProviderId): ProviderCredentials = when (provider) {
        ImageProviders.GOOGLE -> google
        ImageProviders.SENSENOVA -> sensenova
        ImageProviders.SEEDREAM -> seedream
        ImageProviders.STABILITY -> stability
        else -> openai
    }

    fun withCredentials(provider: ImageProviderId, credentials: ProviderCredentials): AppConfig {
        return when (provider) {
            ImageProviders.GOOGLE -> copy(google = credentials)
            ImageProviders.SENSENOVA -> copy(sensenova = credentials)
            ImageProviders.SEEDREAM -> copy(seedream = credentials)
            ImageProviders.STABILITY -> copy(stability = credentials)
            else -> copy(openai = credentials)
        }
    }

    val hasAnyProviderConfigured: Boolean
        get() = openai.isConfigured || google.isConfigured ||
            sensenova.isConfigured || seedream.isConfigured || stability.isConfigured
}

/**
 * Image output format, mirrors the web project's ImageOutputFormat.
 */
typealias ImageOutputFormat = String

object ImageOutputFormats {
    const val PNG: ImageOutputFormat = "png"
    const val JPEG: ImageOutputFormat = "jpeg"
    const val WEBP: ImageOutputFormat = "webp"

    val ALL: List<ImageOutputFormat> = listOf(PNG, JPEG, WEBP)

    fun isKnown(value: String): Boolean = value in ALL

    fun extension(format: ImageOutputFormat): String = format.lowercase()

    fun mimeType(format: ImageOutputFormat): String = when (format.lowercase()) {
        JPEG -> "image/jpeg"
        WEBP -> "image/webp"
        else -> "image/png"
    }
}

typealias ImageQuality = String

object ImageQualities {
    const val AUTO: ImageQuality = "auto"
    const val LOW: ImageQuality = "low"
    const val MEDIUM: ImageQuality = "medium"
    const val HIGH: ImageQuality = "high"

    val ALL: List<ImageQuality> = listOf(AUTO, LOW, MEDIUM, HIGH)
}

typealias ImageBackground = String

object ImageBackgrounds {
    const val AUTO: ImageBackground = "auto"
    const val TRANSPARENT: ImageBackground = "transparent"
    const val OPAQUE: ImageBackground = "opaque"
    val ALL: List<ImageBackground> = listOf(AUTO, TRANSPARENT, OPAQUE)
}

typealias ImageModeration = String

object ImageModerations {
    const val AUTO: ImageModeration = "auto"
    const val LOW: ImageModeration = "low"
    val ALL: List<ImageModeration> = listOf(AUTO, LOW)
}
