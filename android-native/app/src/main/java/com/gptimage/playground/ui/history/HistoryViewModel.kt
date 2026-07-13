package com.gptimage.playground.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val history: StateFlow<List<HistoryItem>> = historyRepository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun delete(id: String) {
        viewModelScope.launch { historyRepository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}
