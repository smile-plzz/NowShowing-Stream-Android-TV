package com.nowshowing.tv.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.db.WatchlistItem
import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val isLoading: Boolean = true,
    val items: List<WatchlistItem> = emptyList()
)

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadWatchlist() {
        // A refresh after a removal must supersede an older in-flight snapshot.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.items.isEmpty())
            val list = repository.getWatchlist()
            _uiState.value = WatchlistUiState(isLoading = false, items = list)
        }
    }

    fun removeItem(imdbId: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(imdbId)
            loadWatchlist()
        }
    }
}

