package com.gptimage.playground.data.network

import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderCredentials
import kotlinx.coroutines.flow.Flow

/**
 * High-level image generation service. Dispatches to the right underlying provider client
 * (OpenAI-compatible, Gemini, or Stability) based on the [ImageModelDefinition.provider] of the request.
 */
class ImageProviderService(
    private val openAiClient: OpenAIImageClient = OpenAIImageClient(),
    private val geminiClient: GeminiImageClient = GeminiImageClient(),
    private val stabilityClient: StabilityImageClient = StabilityImageClient()
) {

    suspend fun generate(
        request: GenerationRequest,
        credentials: ProviderCredentials
    ): GenerationResult {
        require(request.prompt.isNotBlank()) { "Prompt must not be blank" }
        val apiKey = requireApiKey(credentials, request.model)
        val baseUrl = credentials.baseUrl.ifBlank {
            ImageProviders.defaultBaseUrl(request.model.provider)
        }
        return when (request.model.provider) {
            ImageProviders.GOOGLE ->
                geminiClient.generate(request, apiKey, baseUrl)
            ImageProviders.STABILITY ->
                stabilityClient.generate(request, apiKey, baseUrl)
            else ->
                openAiClient.generate(request, apiKey, baseUrl)
        }
    }

    suspend fun edit(
        request: EditRequest,
        credentials: ProviderCredentials
    ): GenerationResult {
        require(request.prompt.isNotBlank()) { "Prompt must not be blank" }
        require(request.referenceImages.isNotEmpty()) { "Edit requires at least one reference image" }
        // 拒绝非 OpenAI provider 的 mask：Gemini / SenseNova / Seedream / Stability 全部不支持
        if (request.mask != null && !request.model.supportsMask) {
            throw ProviderException(
                kind = ProviderException.Kind.BAD_REQUEST,
                message = "${request.model.label} 暂不支持蒙版编辑，请移除蒙版后重试"
            )
        }
        val apiKey = requireApiKey(credentials, request.model)
        val baseUrl = credentials.baseUrl.ifBlank {
            ImageProviders.defaultBaseUrl(request.model.provider)
        }
        return when (request.model.provider) {
            ImageProviders.GOOGLE ->
                geminiClient.edit(request, apiKey, baseUrl)
            ImageProviders.STABILITY ->
                throw ProviderException(
                    kind = ProviderException.Kind.BAD_REQUEST,
                    message = "Stability SD3 only supports text-to-image, not editing"
                )
            else ->
                openAiClient.edit(request, apiKey, baseUrl)
        }
    }

    /**
     * 流式生成。仅 OpenAI 兼容上游支持（`gpt-image-*` 系列已在 [ImageModelDefinition.supportsStreaming] 标记）。
     * 其他 provider 调用此方法会抛 [UnsupportedOperationException]，调用方应回退到非流式 [generate]。
     *
     * @param partialImages 部分图像预览数量，1-3 之间。Web 端默认 2。
     */
    fun generateStream(
        request: GenerationRequest,
        credentials: ProviderCredentials,
        partialImages: Int = 2
    ): Flow<StreamEvent> {
        require(request.prompt.isNotBlank()) { "Prompt must not be blank" }
        require(request.model.supportsStreaming) {
            "Model ${request.model.id} does not support streaming"
        }
        val apiKey = requireApiKey(credentials, request.model)
        val baseUrl = credentials.baseUrl.ifBlank {
            ImageProviders.defaultBaseUrl(request.model.provider)
        }
        return when (request.model.provider) {
            ImageProviders.OPENAI ->
                openAiClient.generateStream(request, apiKey, baseUrl, partialImages)
            else ->
                throw UnsupportedOperationException(
                    "Streaming not supported for provider ${request.model.provider}"
                )
        }
    }

    /**
     * 流式编辑。仅 OpenAI multipart 编辑路径支持流式（`gpt-image-*` 系列）。
     */
    fun editStream(
        request: EditRequest,
        credentials: ProviderCredentials,
        partialImages: Int = 2
    ): Flow<StreamEvent> {
        require(request.prompt.isNotBlank()) { "Prompt must not be blank" }
        require(request.referenceImages.isNotEmpty()) { "Edit requires at least one reference image" }
        require(request.model.supportsStreaming) {
            "Model ${request.model.id} does not support streaming"
        }
        // 与非流式 edit 一致：拒绝非 OpenAI provider 的 mask 请求
        if (request.mask != null && !request.model.supportsMask) {
            throw ProviderException(
                kind = ProviderException.Kind.BAD_REQUEST,
                message = "${request.model.label} 暂不支持蒙版编辑，请移除蒙版后重试"
            )
        }
        val apiKey = requireApiKey(credentials, request.model)
        val baseUrl = credentials.baseUrl.ifBlank {
            ImageProviders.defaultBaseUrl(request.model.provider)
        }
        return when (request.model.provider) {
            ImageProviders.OPENAI ->
                openAiClient.editStream(request, apiKey, baseUrl, partialImages)
            else ->
                throw UnsupportedOperationException(
                    "Streaming edit not supported for provider ${request.model.provider}"
                )
        }
    }

    private fun requireApiKey(credentials: ProviderCredentials, model: ImageModelDefinition): String {
        if (credentials.apiKey.isBlank()) {
            throw ProviderException(
                kind = ProviderException.Kind.AUTH,
                message = "API Key for ${ImageProviders.label(model.provider)} is not configured"
            )
        }
        return credentials.apiKey
    }
}
