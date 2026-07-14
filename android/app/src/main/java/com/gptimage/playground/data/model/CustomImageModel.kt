package com.gptimage.playground.data.model

import kotlinx.serialization.Serializable

/**
 * 自定义图像模型能力开关。所有字段允许为 null，表示「按 provider 默认值」。
 *
 * 与 Web 项目 `src/lib/model-registry.ts` 的 `CustomImageModelCapabilities` 对齐。
 */
@Serializable
data class CustomImageModelCapabilities(
    val supportsStreaming: Boolean? = null,
    val supportsEditing: Boolean? = null,
    val supportsMask: Boolean? = null,
    val supportsCustomSize: Boolean? = null,
    val supportsQuality: Boolean? = null,
    val supportsOutputFormat: Boolean? = null,
    val supportsBackground: Boolean? = null,
    val supportsModeration: Boolean? = null,
    val supportsCompression: Boolean? = null
)

/**
 * 用户自定义的图像模型。与 Web 项目 `StoredCustomImageModel` 对齐。
 *
 * 字段：
 * - [id]：业务 ID，必须以 `custom:` 前缀开头，避免与内置 `IMAGE_MODEL_IDS` 冲突
 * - [provider]：归属 provider
 * - [label]：显示名（可选，缺省用 id）
 * - [capabilities]：能力开关（可选）
 * - [sizePresets]：尺寸预设 square / landscape / portrait（可选）
 * - [defaultSize]：默认尺寸，如 "2K" 或 "2048x2048"（可选）
 * - [providerOptions]：附加 provider 参数，如 `{"response_format": "url"}`（可选）
 *
 * 不实现 Web 端的 `instanceId` 字段（Android 端不需要 provider 实例多路复用）。
 */
@Serializable
data class CustomImageModel(
    val id: String,
    val provider: ImageProviderId,
    val label: String? = null,
    val capabilities: CustomImageModelCapabilities? = null,
    val sizePresets: ImageModelSizePresets? = null,
    val defaultSize: String? = null,
    val providerOptions: Map<String, String> = emptyMap()
) {
    companion object {
        /** 自定义模型 id 的前缀，避免与内置模型 id 冲突。 */
        const val ID_PREFIX = "custom:"
    }
}

/**
 * 归一化 + 去重 + 过滤自定义模型列表。
 *
 * 规则（与 Web 端 `normalizeCustomImageModels` 对齐）：
 * - id 必须以 [CustomImageModel.ID_PREFIX] 开头，否则补上前缀
 * - 跳过 id 为空或与内置模型 id 冲突的项
 * - 同 id 保留第一个
 * - provider 必须是已知 provider，否则归为 OPENAI
 */
object CustomImageModels {

    /** 已知的内置模型 id 集合，用于检测冲突。 */
    private val BUILTIN_IDS: Set<String> = ImageModelCatalog.MODELS.map { it.id }.toSet()

    fun normalize(input: List<CustomImageModel>?): List<CustomImageModel> {
        if (input.isNullOrEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        return input.mapNotNull { raw ->
            val rawId = raw.id.trim()
            if (rawId.isEmpty()) return@mapNotNull null
            // 强制加前缀
            val id = if (rawId.startsWith(CustomImageModel.ID_PREFIX)) rawId
                else "${CustomImageModel.ID_PREFIX}$rawId"
            // 跳过与内置 id 冲突（罕见：rawId 正好是内置 id 时直接拒绝）
            if (rawId in BUILTIN_IDS) return@mapNotNull null
            if (!seen.add(id)) return@mapNotNull null  // 去重

            val provider = if (ImageProviders.isKnown(raw.provider)) raw.provider else ImageProviders.OPENAI
            raw.copy(id = id, provider = provider)
        }
    }

    /**
     * 把自定义模型合并到内置 catalog 列表中（用于工作台 / 设置页展示）。
     *
     * 与 Web 端 `getAllImageModels(customModels)` 对齐：返回 `[...内置 MODELS, ...自定义]`。
     * 自定义模型的能力开关在未指定时按 provider 默认值兜底。
     */
    fun mergeWithBuiltin(custom: List<CustomImageModel>): List<ImageModelDefinition> {
        val normalized = normalize(custom)
        if (normalized.isEmpty()) return ImageModelCatalog.MODELS
        val expanded = normalized.map { customModel ->
            val caps = customModel.capabilities ?: CustomImageModelCapabilities()
            val providerDefault = defaultCapabilitiesFor(customModel.provider)
            ImageModelDefinition(
                id = customModel.id,
                label = customModel.label?.takeIf { it.isNotBlank() } ?: customModel.id.removePrefix(CustomImageModel.ID_PREFIX),
                provider = customModel.provider,
                providerLabel = ImageProviders.label(customModel.provider),
                supportsStreaming = caps.supportsStreaming ?: providerDefault.supportsStreaming ?: false,
                supportsEditing = caps.supportsEditing ?: providerDefault.supportsEditing ?: false,
                supportsMask = caps.supportsMask ?: providerDefault.supportsMask ?: false,
                supportsCustomSize = caps.supportsCustomSize ?: providerDefault.supportsCustomSize ?: false,
                supportsQuality = caps.supportsQuality ?: providerDefault.supportsQuality ?: false,
                supportsOutputFormat = caps.supportsOutputFormat ?: providerDefault.supportsOutputFormat ?: false,
                supportsBackground = caps.supportsBackground ?: providerDefault.supportsBackground ?: false,
                supportsModeration = caps.supportsModeration ?: providerDefault.supportsModeration ?: false,
                supportsCompression = caps.supportsCompression ?: providerDefault.supportsCompression ?: false,
                sizePresets = customModel.sizePresets,
                defaultSize = customModel.defaultSize,
                providerOptions = customModel.providerOptions
            )
        }
        return ImageModelCatalog.MODELS + expanded
    }

    /**
     * 按 provider 给出默认能力开关（用于自定义模型未指定时兜底）。
     * 与 Web 端 `createCustomImageModelDefinition` 中各 provider 的兜底值对齐。
     */
    private fun defaultCapabilitiesFor(provider: ImageProviderId): CustomImageModelCapabilities {
        return when (provider) {
            ImageProviders.OPENAI -> CustomImageModelCapabilities(
                supportsStreaming = true,
                supportsEditing = true,
                supportsMask = true,
                supportsQuality = true,
                supportsOutputFormat = true,
                supportsBackground = true,
                supportsModeration = true,
                supportsCompression = true
            )
            // Google / SenseNova / Seedream / Stability 默认关闭所有可选能力，
            // 用户可以在录入时显式打开。
            else -> CustomImageModelCapabilities()
        }
    }
}
