package com.gptimage.playground.data.repository

import com.gptimage.playground.data.datastore.SettingsStore
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.AppLanguage
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
}
