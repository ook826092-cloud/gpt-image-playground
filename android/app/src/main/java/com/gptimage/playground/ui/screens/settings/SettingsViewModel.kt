package com.gptimage.playground.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.ServiceLocator
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderCredentials
import com.gptimage.playground.data.model.ThemeMode
import com.gptimage.playground.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settings: SettingsRepository
) : AndroidViewModel(application) {

    val config: StateFlow<AppConfig> = settings.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    private val _savedEvent = MutableStateFlow(false)
    val savedEvent = _savedEvent.asStateFlow()

    fun setProviderCredentials(provider: String, apiKey: String, baseUrl: String) {
        viewModelScope.launch {
            settings.setProviderCredentials(provider, apiKey, baseUrl)
            _savedEvent.value = true
        }
    }

    fun setDefaultModel(modelId: String) {
        viewModelScope.launch {
            settings.setDefaultModel(modelId)
            _savedEvent.value = true
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settings.setLanguage(language) }
    }

    fun consumeSavedEvent() {
        _savedEvent.value = false
    }
}

class SettingsViewModelFactory(
    private val locator: ServiceLocator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            application = locator.context,
            settings = locator.settingsRepository
        ) as T
    }
}
