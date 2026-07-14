package com.gptimage.playground

import android.content.Context
import com.gptimage.playground.data.db.AppDatabase
import com.gptimage.playground.data.datastore.SettingsStore
import com.gptimage.playground.data.network.ImageProviderService
import com.gptimage.playground.data.repository.HistoryRepository
import com.gptimage.playground.data.repository.ImageGenerationRepository
import com.gptimage.playground.data.repository.SettingsRepository
import java.io.File

/**
 * Lightweight manual dependency container. Created once per process via [PlaygroundApp].
 * Avoids the heavy Hilt/KSP setup while still providing singletons to the ViewModels.
 */
class ServiceLocator(val context: Context) {

    private val settingsStore: SettingsStore by lazy { SettingsStore(context.applicationContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(settingsStore) }

    private val historyRootDir: File by lazy {
        File(context.applicationContext.filesDir, "generated").apply { if (!exists()) mkdirs() }
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(
            dao = AppDatabase.get(context.applicationContext).historyDao(),
            rootDir = historyRootDir
        )
    }

    private val providerService: ImageProviderService by lazy { ImageProviderService() }

    val imageGenerationRepository: ImageGenerationRepository by lazy {
        ImageGenerationRepository(providerService, historyRepository)
    }
}
