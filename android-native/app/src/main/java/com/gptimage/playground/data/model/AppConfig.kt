package com.gptimage.playground.data.model

import kotlinx.serialization.Serializable

/** Supported app languages, mirrors the web app's i18n language list. */
enum class AppLanguage(val code: String, val nativeName: String) {
    ZhCN("zh-CN", "简体中文"),
    EnUS("en-US", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage? =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Theme mode chosen by the user. */
enum class ThemeModeConfig { Light, Dark, System }

/**
 * Provider kinds supported by the native app. The web app supports more
 * (Seedream, SenseNova, etc.); the native port starts with the two most
 * common and is designed to extend.
 */
enum class ProviderKind(val id: String) {
    OpenAICompatible("openai"),
    Gemini("gemini");

    companion object {
        fun fromId(id: String?): ProviderKind =
            values().firstOrNull { it.id == id } ?: OpenAICompatible
    }
}

/** Image quality, mirrors the web app's ImageQuality type. */
enum class ImageQuality { Low, Medium, High, Auto }

/** Output format, mirrors the web app's ImageOutputFormat type. */
enum class ImageOutputFormat { Png, Jpeg, Webp }

/** Generation mode: text-to-image or reference-image edit. */
enum class GenerationMode { Generate, Edit }

@Serializable
data class ProviderInstance(
    val id: String,
    val name: String,
    val kind: ProviderKind = ProviderKind.OpenAICompatible,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelId: String = "gpt-image-1",
    val enabled: Boolean = true
) {
    /** Effective base URL: falls back to the official endpoint when blank. */
    val effectiveBaseUrl: String
        get() = baseUrl.trim().ifBlank {
            when (kind) {
                ProviderKind.OpenAICompatible -> "https://api.openai.com/v1"
                ProviderKind.Gemini -> "https://generativelanguage.googleapis.com/v1beta"
            }
        }
}

@Serializable
data class AppConfig(
    val schemaVersion: Int = 1,
    val appLanguage: String = AppLanguage.ZhCN.code,
    val themeMode: String = ThemeModeConfig.System.name,
    val providerInstances: List<ProviderInstance> = emptyList(),
    val selectedProviderInstanceId: String = "",
    val maxConcurrentTasks: Int = 3,
    val defaultCount: Int = 1,
    val defaultSize: String = "1024x1024",
    val defaultQuality: String = ImageQuality.Auto.name,
    val defaultFormat: String = ImageOutputFormat.Png.name
) {
    val resolvedLanguage: AppLanguage
        get() = AppLanguage.fromCode(appLanguage) ?: AppLanguage.ZhCN

    val resolvedThemeMode: ThemeModeConfig
        get() = runCatching { ThemeModeConfig.valueOf(themeMode) }.getOrDefault(ThemeModeConfig.System)

    val resolvedQuality: ImageQuality
        get() = runCatching { ImageQuality.valueOf(defaultQuality) }.getOrDefault(ImageQuality.Auto)

    val resolvedFormat: ImageOutputFormat
        get() = runCatching { ImageOutputFormat.valueOf(defaultFormat) }.getOrDefault(ImageOutputFormat.Png)

    val selectedProvider: ProviderInstance?
        get() = providerInstances.firstOrNull { it.id == selectedProviderInstanceId && it.enabled }
            ?: providerInstances.firstOrNull { it.enabled }
}

/** Parameters for a single image generation request. */
data class GenerationParams(
    val prompt: String,
    val mode: GenerationMode = GenerationMode.Generate,
    val count: Int = 1,
    val size: String = "1024x1024",
    val quality: ImageQuality = ImageQuality.Auto,
    val outputFormat: ImageOutputFormat = ImageOutputFormat.Png,
    val model: String = "gpt-image-1",
    val providerInstanceId: String = "",
    /** Data URIs for reference images when mode == Edit. */
    val referenceImages: List<String> = emptyList()
)

/** A single produced image, normalized from the provider response. */
data class ImageResult(
    /** Base64 payload without the data-uri prefix, when returned inline. */
    val base64: String? = null,
    /** Remote URL when the provider returns a hosted link. */
    val url: String? = null,
    val outputFormat: ImageOutputFormat = ImageOutputFormat.Png
) {
    val hasContent: Boolean get() = !base64.isNullOrEmpty() || !url.isNullOrEmpty()
}

/** Aggregated result of a generation task. */
data class GenerationResult(
    val images: List<ImageResult>,
    val usage: ProviderUsage? = null,
    val durationMs: Long
)

/** Token/credit usage reported by the provider, mirrors the web app's ProviderUsage. */
@Serializable
data class ProviderUsage(
    val totalTokens: Long = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0
)
