package com.gptimage.playground.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageQuality
import com.gptimage.playground.data.model.ProviderInstance
import com.gptimage.playground.data.model.ProviderKind
import com.gptimage.playground.data.model.ThemeModeConfig
import com.gptimage.playground.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val config: AppConfig = AppConfig(),
    val editingProvider: ProviderInstance? = null,
    val providerDialogOpen: Boolean = false
)

class SettingsViewModel(
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(false to null as ProviderInstance?)

    val uiState: StateFlow<SettingsUiState> =
        combine(configRepository.config, _dialogState) { config, (open, editing) ->
            SettingsUiState(
                config = config,
                providerDialogOpen = open,
                editingProvider = editing
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsUiState()
        )

    val config: StateFlow<AppConfig> = configRepository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { configRepository.update { it.copy(appLanguage = language.code) } }
    }

    fun setThemeMode(mode: ThemeModeConfig) {
        viewModelScope.launch { configRepository.update { it.copy(themeMode = mode.name) } }
    }

    fun setDefaultCount(count: Int) {
        viewModelScope.launch { configRepository.update { it.copy(defaultCount = count.coerceIn(1, 10)) } }
    }

    fun setDefaultSize(size: String) {
        viewModelScope.launch { configRepository.update { it.copy(defaultSize = size) } }
    }

    fun setDefaultQuality(quality: ImageQuality) {
        viewModelScope.launch { configRepository.update { it.copy(defaultQuality = quality.name) } }
    }

    fun setDefaultFormat(format: ImageOutputFormat) {
        viewModelScope.launch { configRepository.update { it.copy(defaultFormat = format.name) } }
    }

    fun openAddProvider() {
        val draft = configRepository.createDefaultProvider(
            name = "OpenAI",
            kind = ProviderKind.OpenAICompatible
        )
        _dialogState.value = true to draft
    }

    fun openEditProvider(provider: ProviderInstance) {
        _dialogState.value = true to provider
    }

    fun closeProviderDialog() {
        _dialogState.value = false to null
    }

    fun saveProvider(provider: ProviderInstance) {
        viewModelScope.launch {
            configRepository.update { config ->
                val exists = config.providerInstances.any { it.id == provider.id }
                val instances = if (exists) {
                    config.providerInstances.map { if (it.id == provider.id) provider else it }
                } else {
                    config.providerInstances + provider
                }
                val selected = if (config.selectedProviderInstanceId.isBlank()) provider.id
                else config.selectedProviderInstanceId
                config.copy(providerInstances = instances, selectedProviderInstanceId = selected)
            }
            _dialogState.value = false to null
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            configRepository.update { config ->
                val instances = config.providerInstances.filterNot { it.id == id }
                val selected = if (config.selectedProviderInstanceId == id) {
                    instances.firstOrNull { it.enabled }?.id ?: ""
                } else config.selectedProviderInstanceId
                config.copy(providerInstances = instances, selectedProviderInstanceId = selected)
            }
        }
    }

    fun selectProvider(id: String) {
        viewModelScope.launch { configRepository.update { it.copy(selectedProviderInstanceId = id) } }
    }

    fun toggleProviderEnabled(id: String) {
        viewModelScope.launch {
            configRepository.update { config ->
                config.copy(
                    providerInstances = config.providerInstances.map {
                        if (it.id == id) it.copy(enabled = !it.enabled) else it
                    }
                )
            }
        }
    }
}
