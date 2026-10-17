package com.nowshowing.tv.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

enum class SortMode(val label: String) {
    FEATURED("Featured"),
    TITLE("Title A–Z"),
    NEWEST("Newest"),
    TOP_RATED("Top Rated")
}

data class BrowseUiState(
    val kind: String = "movies",
    val title: String = "Popular Movies",
    val isLoading: Boolean = true,
    val items: List<OmdbTitleDetail> = emptyList(),
    val sortMode: SortMode = SortMode.FEATURED
)

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private var browseJob: Job? = null
    private var rawItems: List<OmdbTitleDetail> = emptyList()

    fun loadBrowse(kind: String) {
        if (_uiState.value.kind == kind && (rawItems.isNotEmpty() || browseJob?.isActive == true)) return
        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                kind = kind,
                title = if (kind == "tv") "Popular TV Shows" else "Popular Movies",
                isLoading = true
            )

            val titles = if (kind == "tv") MediaRepository.POPULAR_TV else MediaRepository.POPULAR_MOVIES
            val type = if (kind == "tv") "series" else "movie"

            rawItems = repository.lookupTitles(titles, type)
            applySort(_uiState.value.sortMode)
        }
    }

    fun setSortMode(mode: SortMode) {
        _uiState.value = _uiState.value.copy(sortMode = mode)
        applySort(mode)
    }

    private fun applySort(mode: SortMode) {
        val sorted = when (mode) {
            SortMode.FEATURED -> rawItems
            SortMode.TITLE -> rawItems.sortedBy { it.title }
            SortMode.NEWEST -> rawItems.sortedByDescending { it.year.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            SortMode.TOP_RATED -> rawItems.sortedByDescending { it.imdbRating?.toFloatOrNull() ?: 0f }
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            items = sorted
        )
    }
}
