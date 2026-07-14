package com.gptimage.playground.ui.screens.workbench

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.ServiceLocator
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.model.ImageModelCatalog
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderCredentials
import com.gptimage.playground.data.model.ReferenceImage as DataReferenceImage
import com.gptimage.playground.data.network.ProviderException
import com.gptimage.playground.data.repository.GenerationOutcome as RepoGenerationOutcome
import com.gptimage.playground.data.repository.ImageGenerationRepository
import com.gptimage.playground.data.repository.ReferenceImages
import com.gptimage.playground.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WorkbenchViewModel(
    application: Application,
    private val settings: SettingsRepository,
    private val generationRepository: ImageGenerationRepository
) : AndroidViewModel(application) {

    private val local = MutableStateFlow(WorkbenchUiState())

    private val configFlow: StateFlow<AppConfig> = settings.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    val state: StateFlow<WorkbenchUiState> = combine(local, configFlow) { current, config ->
        val availableModels = ImageModelCatalog.MODELS
        val resolvedModel = current.model ?: availableModels.firstOrNull { it.id == config.defaultModelId }
            ?: availableModels.firstOrNull()
        val providerForModel = resolvedModel?.provider ?: ImageProviders.OPENAI
        val providerConfigured = config.credentialsFor(providerForModel).isConfigured
        current.copy(
            availableModels = availableModels,
            model = resolvedModel,
            providerConfigured = providerConfigured
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WorkbenchUiState()
    )

    fun updatePrompt(value: String) = local.update { it.copy(prompt = value) }

    fun selectModel(model: ImageModelDefinition) = local.update {
        it.copy(model = model, size = model.defaultSize ?: model.sizePresets?.square)
    }

    fun setAdvancedExpanded(expanded: Boolean) = local.update { it.copy(advancedExpanded = expanded) }

    fun setCount(count: Int) = local.update {
        it.copy(count = count.coerceIn(1, 4))
    }

    fun setSize(size: String?) = local.update { it.copy(size = size) }

    fun setQuality(quality: String?) = local.update { it.copy(quality = quality) }

    fun setOutputFormat(format: String?) = local.update { it.copy(outputFormat = format) }

    fun setBackground(background: String?) = local.update { it.copy(background = background) }

    fun setModeration(moderation: String?) = local.update { it.copy(moderation = moderation) }

    fun addReference(uri: Uri, name: String, mimeType: String) = local.update {
        it.copy(referenceImages = it.referenceImages + ReferenceImageUi(uri, name, mimeType))
    }

    fun removeReferenceAt(index: Int) = local.update {
        val updated = it.referenceImages.toMutableList()
        if (index in updated.indices) updated.removeAt(index)
        it.copy(referenceImages = updated)
    }

    fun clearReferences() = local.update { it.copy(referenceImages = emptyList()) }

    fun useHistoryItemAsReference(item: HistoryItem) {
        val file = java.io.File(item.imagePath)
        if (file.exists()) {
            val uri = Uri.fromFile(file)
            local.update {
                it.copy(
                    referenceImages = it.referenceImages + ReferenceImageUi(
                        uri = uri,
                        name = file.name,
                        mimeType = guessMimeType(file)
                    )
                )
            }
        }
    }

    fun sendToEdit(item: HistoryItem) {
        useHistoryItemAsReference(item)
        local.update { it.copy(prompt = item.prompt) }
    }

    fun generate() {
        val current = state.value
        val model = current.model ?: return
        if (current.prompt.isBlank()) return
        if (!current.providerConfigured) return

        viewModelScope.launch {
            local.update { it.copy(isGenerating = true, error = null) }
            val config = configFlow.value
            val credentials = config.credentialsFor(model.provider)

            val outcome = if (current.referenceImages.isEmpty()) {
                generateTextToImage(current, model, credentials)
            } else {
                editImage(current, model, credentials)
            }

            when (outcome) {
                is RepoGenerationOutcome.Success -> local.update {
                    it.copy(isGenerating = false, lastResult = outcome.item, error = null)
                }
                is RepoGenerationOutcome.Failure -> {
                    val message = errorMessage(outcome.error)
                    local.update { it.copy(isGenerating = false, error = message) }
                }
            }
        }
    }

    fun clearError() = local.update { it.copy(error = null) }

    private suspend fun generateTextToImage(
        current: WorkbenchUiState,
        model: ImageModelDefinition,
        credentials: ProviderCredentials
    ): RepoGenerationOutcome {
        val request = GenerationRequest(
            model = model,
            prompt = current.prompt,
            n = current.count,
            size = current.size,
            quality = current.quality,
            outputFormat = current.outputFormat,
            background = current.background,
            moderation = current.moderation,
            providerOptions = model.providerOptions
        )
        return generationRepository.generate(request, credentials, model.provider)
    }

    private suspend fun editImage(
        current: WorkbenchUiState,
        model: ImageModelDefinition,
        credentials: ProviderCredentials
    ): RepoGenerationOutcome {
        val references = loadReferenceImages(current.referenceImages)
        if (references.isEmpty()) {
            return RepoGenerationOutcome.Failure(
                ProviderException(ProviderException.Kind.BAD_REQUEST, "无法读取参考图")
            )
        }
        val request = EditRequest(
            model = model,
            prompt = current.prompt,
            referenceImages = references,
            n = current.count,
            size = current.size,
            quality = current.quality,
            providerOptions = model.providerOptions
        )
        return generationRepository.edit(request, credentials, model.provider)
    }

    private suspend fun loadReferenceImages(uris: List<ReferenceImageUi>): List<DataReferenceImage> =
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            uris.mapNotNull { ref ->
                try {
                    val tempFile = File(context.cacheDir, "ref_${System.currentTimeMillis()}_${ref.name}")
                    context.contentResolver.openInputStream(ref.uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@mapNotNull null
                    ReferenceImages.fromFile(tempFile)
                } catch (e: Throwable) {
                    null
                }
            }
        }

    private fun errorMessage(error: ProviderException): String = when (error.kind) {
        ProviderException.Kind.NETWORK -> "网络异常，请检查网络后重试"
        ProviderException.Kind.AUTH -> "API Key 无效或未配置"
        ProviderException.Kind.RATE_LIMIT -> "请求过于频繁，请稍后重试"
        ProviderException.Kind.BAD_REQUEST -> "请求参数有误：${error.message}"
        ProviderException.Kind.SERVER -> "供应商服务异常，请稍后再试"
        ProviderException.Kind.PARSE -> "返回数据格式异常"
        ProviderException.Kind.UNKNOWN -> error.message ?: "未知错误"
    }

    private fun guessMimeType(file: java.io.File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "png" -> "image/png"
            else -> "image/png"
        }
    }
}

class WorkbenchViewModelFactory(
    private val locator: ServiceLocator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkbenchViewModel(
            application = locator.application,
            settings = locator.settingsRepository,
            generationRepository = locator.imageGenerationRepository
        ) as T
    }
}
