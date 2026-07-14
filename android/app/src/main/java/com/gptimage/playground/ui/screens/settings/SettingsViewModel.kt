package com.gptimage.playground.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.ServiceLocator
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.CustomImageModel
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderCredentials
import com.gptimage.playground.data.model.ThemeMode
import com.gptimage.playground.data.repository.SettingsRepository
import com.gptimage.playground.data.repository.UrlSafety
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

    /** 一次性错误提示（用于 URL 安全校验失败等场景）。 */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun consumeSavedEvent() { _savedEvent.value = false }
    fun consumeErrorMessage() { _errorMessage.value = null }

    /**
     * 保存 provider 凭据。会做 base url 安全校验：
     * - 若 baseUrl 非空且 [UrlSafety.validatePublicHttpBaseUrl] 返回 Bad，则不保存并发出错误提示。
     * - 否则归一化 baseUrl 后保存。
     */
    fun setProviderCredentials(provider: String, apiKey: String, baseUrl: String) {
        val trimmed = baseUrl.trim()
        val normalized = if (trimmed.isEmpty()) "" else UrlSafety.normalizeOpenAICompatibleBaseUrl(trimmed)
        if (normalized.isNotEmpty()) {
            val result = UrlSafety.validatePublicHttpBaseUrl(normalized)
            if (result is UrlSafety.Result.Bad) {
                _errorMessage.value = result.reason
                return
            }
        }
        viewModelScope.launch {
            settings.setProviderCredentials(provider, apiKey.trim(), normalized)
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

    // —— 自定义模型 CRUD ——

    /**
     * 添加或更新自定义模型。若 [model.id] 已存在则覆盖，否则追加。
     */
    fun upsertCustomModel(model: CustomImageModel) {
        viewModelScope.launch {
            val current = settings.current()
            val existing = current.customImageModels.toMutableList()
            val idx = existing.indexOfFirst { it.id == model.id }
            if (idx >= 0) existing[idx] = model else existing.add(model)
            settings.setCustomImageModels(existing)
            _savedEvent.value = true
        }
    }

    fun deleteCustomModel(id: String) {
        viewModelScope.launch {
            val current = settings.current()
            val next = current.customImageModels.filterNot { it.id == id }
            settings.setCustomImageModels(next)
            // 如果删除的是当前默认模型，回退到内置默认
            if (current.defaultModelId == id) {
                settings.setDefaultModel(com.gptimage.playground.data.model.ImageModelCatalog.DEFAULT_MODEL_ID)
            }
            _savedEvent.value = true
        }
    }

    /** 返回内置 + 自定义合并后的完整模型 catalog。 */
    fun allModels(): List<ImageModelDefinition> = settings.allModels(config.value)
}

class SettingsViewModelFactory(
    private val locator: ServiceLocator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            application = locator.application,
            settings = locator.settingsRepository
        ) as T
    }
}
