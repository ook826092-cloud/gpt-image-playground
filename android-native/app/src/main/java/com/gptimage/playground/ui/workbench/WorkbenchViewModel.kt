package com.gptimage.playground.ui.workbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.GenerationMode
import com.gptimage.playground.data.model.GenerationParams
import com.gptimage.playground.data.model.GenerationResult
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageQuality
import com.gptimage.playground.data.model.ProviderInstance
import com.gptimage.playground.data.remote.ApiError
import com.gptimage.playground.data.remote.ApiResult
import com.gptimage.playground.data.remote.OpenAICompatibleClient
import com.gptimage.playground.data.repository.ConfigRepository
import com.gptimage.playground.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI state for the workbench screen. */
data class WorkbenchUiState(
    val prompt: String = "",
    val isGenerating: Boolean = false,
    val advancedVisible: Boolean = false,
    val count: Int = 1,
    val size: String = "1024x1024",
    val quality: ImageQuality = ImageQuality.Auto,
    val outputFormat: ImageOutputFormat = ImageOutputFormat.Png,
    val selectedProviderId: String = "",
    val availableProviders: List<ProviderInstance> = emptyList(),
    val lastResult: GenerationResult? = null,
    val lastError: String? = null,
    val infoMessage: String? = null
)

class WorkbenchViewModel(
    private val configRepository: ConfigRepository,
    private val historyRepository: HistoryRepository,
    private val client: OpenAICompatibleClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkbenchUiState())
    val uiState: StateFlow<WorkbenchUiState> = _uiState.asStateFlow()

    val config: StateFlow<AppConfig> = configRepository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    init {
        viewModelScope.launch {
            configRepository.config.collect { cfg ->
                _uiState.value = _uiState.value.copy(
                    availableProviders = cfg.providerInstances.filter { it.enabled },
                    selectedProviderId = _uiState.value.selectedProviderId.ifBlank {
                        cfg.selectedProviderInstanceId
                    }.ifBlank { cfg.providerInstances.firstOrNull { it.enabled }?.id ?: "" }
                )
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.value = _uiState.value.copy(prompt = value)
    }

    fun onCountChange(value: Int) {
        _uiState.value = _uiState.value.copy(count = value.coerceIn(1, 10))
    }

    fun onSizeChange(value: String) {
        _uiState.value = _uiState.value.copy(size = value)
    }

    fun onQualityChange(value: ImageQuality) {
        _uiState.value = _uiState.value.copy(quality = value)
    }

    fun onFormatChange(value: ImageOutputFormat) {
        _uiState.value = _uiState.value.copy(outputFormat = value)
    }

    fun onProviderChange(id: String) {
        _uiState.value = _uiState.value.copy(selectedProviderId = id)
        viewModelScope.launch {
            configRepository.update { it.copy(selectedProviderInstanceId = id) }
        }
    }

    fun toggleAdvanced(visible: Boolean? = null) {
        val next = visible ?: !_uiState.value.advancedVisible
        _uiState.value = _uiState.value.copy(advancedVisible = next)
    }

    fun clearPrompt() {
        _uiState.value = _uiState.value.copy(prompt = "")
    }

    fun dismissInfo() {
        _uiState.value = _uiState.value.copy(infoMessage = null, lastError = null)
    }

    fun generate() {
        val state = _uiState.value
        if (state.isGenerating) return
        if (state.prompt.isBlank()) {
            _uiState.value = state.copy(infoMessage = "PROMPT_EMPTY")
            return
        }
        val provider = state.availableProviders.firstOrNull { it.id == state.selectedProviderId }
            ?: state.availableProviders.firstOrNull()
        if (provider == null) {
            _uiState.value = state.copy(infoMessage = "PROVIDER_NOT_CONFIGURED")
            return
        }

        val params = GenerationParams(
            prompt = state.prompt.trim(),
            mode = GenerationMode.Generate,
            count = state.count,
            size = state.size,
            quality = state.quality,
            outputFormat = state.outputFormat,
            model = provider.modelId,
            providerInstanceId = provider.id
        )

        _uiState.value = state.copy(isGenerating = true, lastError = null, infoMessage = null)

        viewModelScope.launch {
            when (val result = client.request(provider, params)) {
                is ApiResult.Success -> {
                    historyRepository.record(
                        prompt = params.prompt,
                        mode = "generate",
                        model = params.model,
                        providerInstanceId = provider.id,
                        providerName = provider.name,
                        quality = params.quality.name,
                        outputFormat = params.outputFormat.name,
                        size = params.size,
                        count = params.count,
                        images = result.value.images,
                        durationMs = result.value.durationMs,
                        totalTokens = result.value.usage?.totalTokens ?: 0L
                    )
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        lastResult = result.value,
                        infoMessage = null
                    )
                }
                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        lastError = result.error.displayMessage()
                    )
                }
            }
        }
    }

    private fun ApiError.displayMessage(): String = message
}
