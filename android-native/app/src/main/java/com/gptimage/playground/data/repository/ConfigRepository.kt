package com.gptimage.playground.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.ProviderInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.configDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gpt_image_playground_config"
)

/**
 * Persists the app config as a single JSON blob, mirroring the web app's
 * localStorage-backed config store (`src/lib/config.ts`). The config is
 * normalized on read so legacy/missing fields fall back to safe defaults.
 */
class ConfigRepository(
    private val context: Context,
    private val json: Json
) {
    private val key = stringPreferencesKey("config_json")

    val config: Flow<AppConfig> = context.configDataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching { json.decodeFromString(AppConfig.serializer(), raw) }
                .getOrNull()
                ?.normalized()
        } ?: defaultConfig()
    }

    suspend fun save(config: AppConfig) {
        context.configDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(AppConfig.serializer(), config)
        }
    }

    suspend fun update(transform: (AppConfig) -> AppConfig) {
        context.configDataStore.edit { prefs ->
            val current = prefs[key]?.let { raw ->
                runCatching { json.decodeFromString(AppConfig.serializer(), raw) }.getOrNull()
            } ?: defaultConfig()
            val next = transform(current).normalized()
            prefs[key] = json.encodeToString(AppConfig.serializer(), next)
        }
    }

    private fun AppConfig.normalized(): AppConfig = copy(
        providerInstances = providerInstances.ifEmpty { emptyList() },
        maxConcurrentTasks = maxConcurrentTasks.coerceIn(1, 10)
    )

    private fun defaultConfig(): AppConfig = AppConfig(
        appLanguage = AppLanguage.ZhCN.code,
        providerInstances = emptyList(),
        selectedProviderInstanceId = ""
    )

    /** Seed a default OpenAI-compatible provider entry. Used on first run. */
    fun createDefaultProvider(name: String, kind: com.gptimage.playground.data.model.ProviderKind): ProviderInstance =
        ProviderInstance(
            id = generateId(),
            name = name,
            kind = kind,
            apiKey = "",
            baseUrl = "",
            modelId = if (kind == com.gptimage.playground.data.model.ProviderKind.Gemini) "gemini-2.5-flash-image" else "gpt-image-1"
        )

    private fun generateId(): String =
        java.util.UUID.randomUUID().toString()
}
