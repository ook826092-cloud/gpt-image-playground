package com.gptimage.playground.ui.screens.album

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.ServiceLocator
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumViewModel(
    application: Application,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    val images: StateFlow<List<HistoryItem>> = historyRepository.all.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun delete(item: HistoryItem) {
        viewModelScope.launch { historyRepository.delete(item) }
    }
}

class AlbumViewModelFactory(
    private val locator: ServiceLocator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumViewModel(
            application = locator.context,
            historyRepository = locator.historyRepository
        ) as T
    }
}
