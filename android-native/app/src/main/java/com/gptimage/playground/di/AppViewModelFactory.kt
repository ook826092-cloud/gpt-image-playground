package com.gptimage.playground.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gptimage.playground.ui.history.HistoryViewModel
import com.gptimage.playground.ui.settings.SettingsViewModel
import com.gptimage.playground.ui.workbench.WorkbenchViewModel

/**
 * Single ViewModelProvider.Factory backed by [AppContainer]. Each screen's
 * ViewModel is created here so composables only need one factory instance.
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WorkbenchViewModel::class.java) ->
                WorkbenchViewModel(
                    configRepository = container.configRepository,
                    historyRepository = container.historyRepository,
                    client = container.openAIClient
                ) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.configRepository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(container.historyRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
