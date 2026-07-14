package com.gptimage.playground.data.network

import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderCredentials

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
