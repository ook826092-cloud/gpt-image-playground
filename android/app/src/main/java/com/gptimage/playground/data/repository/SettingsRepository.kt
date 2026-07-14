package com.gptimage.playground.data.repository

import com.gptimage.playground.data.datastore.SettingsStore
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.CustomImageModel
import com.gptimage.playground.data.model.CustomImageModels
import com.gptimage.playground.data.model.ImageModelCatalog
import com.gptimage.playground.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepository(private val store: SettingsStore) {

    val config: Flow<AppConfig> = store.config

    suspend fun current(): AppConfig = store.config.first()

    suspend fun setProviderCredentials(provider: String, apiKey: String, baseUrl: String) =
        store.setProviderCredentials(provider, apiKey, baseUrl)

    suspend fun setDefaultModel(modelId: String) = store.setDefaultModel(modelId)

    suspend fun setThemeMode(mode: ThemeMode) = store.setThemeMode(mode)

    suspend fun setLanguage(language: AppLanguage) = store.setLanguage(language)

    fun defaultModel(): String = ImageModelCatalog.DEFAULT_MODEL_ID

    /** 替换整个自定义模型列表。覆盖写，调用方应预先归一化。 */
    suspend fun setCustomImageModels(models: List<CustomImageModel>) =
        store.setCustomImageModels(models)

    /** 返回内置 + 自定义合并后的完整模型 catalog（用于工作台 / 设置页展示）。 */
    fun allModels(config: AppConfig): List<com.gptimage.playground.data.model.ImageModelDefinition> =
        CustomImageModels.mergeWithBuiltin(config.customImageModels)
}
