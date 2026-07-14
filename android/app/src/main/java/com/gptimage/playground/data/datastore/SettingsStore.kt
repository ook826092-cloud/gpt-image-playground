package com.gptimage.playground.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.ImageModelCatalog
import com.gptimage.playground.data.model.ProviderCredentials
import com.gptimage.playground.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists the [AppConfig] (provider credentials, default model, theme, language) to a single
 * DataStore<Preferences> entry. The config is serialized to JSON for forward/backward compatibility.
 */
class SettingsStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val key = stringPreferencesKey("app_config_json")

    val config: Flow<AppConfig> = context.settingsDataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching { json.decodeFromString<AppConfig>(raw) }.getOrNull()
        } ?: AppConfig()
    }

    suspend fun update(transform: (AppConfig) -> AppConfig) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[key]?.let { raw ->
                runCatching { json.decodeFromString<AppConfig>(raw) }.getOrNull()
            } ?: AppConfig()
            val next = transform(current)
            prefs[key] = json.encodeToString(AppConfig.serializer(), next)
        }
    }

    suspend fun setProviderCredentials(
        provider: String,
        apiKey: String,
        baseUrl: String
    ) = update { current ->
        val normalizedBaseUrl = baseUrl.trim()
        current.withCredentials(
            provider,
            ProviderCredentials(
                apiKey = apiKey.trim(),
                baseUrl = normalizedBaseUrl
            )
        )
    }

    suspend fun setDefaultModel(modelId: String) = update { it.copy(defaultModelId = modelId) }

    suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    suspend fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }
}

object SettingsDefaults {
    const val DEFAULT_MODEL_ID = ImageModelCatalog.DEFAULT_MODEL_ID
}
