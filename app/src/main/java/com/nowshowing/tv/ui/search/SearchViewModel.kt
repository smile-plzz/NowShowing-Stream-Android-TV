package com.nowshowing.tv.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.model.OmdbTitleShort
import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class SearchUiState(
    val query: String = "",
    val selectedType: String = "all", // "all", "movie", "series"
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val results: List<OmdbTitleShort> = emptyList(),
    val totalResults: Int = 0,
    val currentPage: Int = 1,
    val errorMessage: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private var searchJob: Job? = null
    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadRecentSearches()
    }

    fun loadRecentSearches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                recentSearches = repository.getRecentSearches()
            )
        }
    }

    fun onCharInput(c: Char) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(query = _uiState.value.query + c, isLoading = false)
    }

    fun onBackspace() {
        searchJob?.cancel()
        val q = _uiState.value.query
        if (q.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(query = q.dropLast(1), isLoading = false)
        }
    }

    fun onClear() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(query = "", results = emptyList(), totalResults = 0, currentPage = 1, isLoading = false, errorMessage = null)
    }

    fun setTypeFilter(type: String) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        if (_uiState.value.query.isNotBlank()) {
            executeSearch(_uiState.value.query, reset = true)
        }
    }

    fun retrySearch() { com.nowshowing.tv.data.repository.MetadataHealth.retry(); executeSearch() }

    fun executeSearch(query: String = _uiState.value.query, reset: Boolean = true) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        if (!reset && _uiState.value.isLoading) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            repository.addRecentSearch(trimmed)
            loadRecentSearches()

            val targetPage = if (reset) 1 else _uiState.value.currentPage + 1
            _uiState.value = _uiState.value.copy(
                query = trimmed,
                isLoading = true,
                errorMessage = null
            )

            val filterType = when (_uiState.value.selectedType) {
                "movie" -> "movie"
                "series" -> "series"
                else -> null
            }

            val response = repository.search(trimmed, page = targetPage, type = filterType)
            if (response.response != "False" && response.search != null) {
                val updated = (if (reset) response.search else _uiState.value.results + response.search).distinctBy { it.imdbID }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = updated,
                    currentPage = targetPage,
                    totalResults = response.totalResults?.toIntOrNull() ?: updated.size
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = _uiState.value.results,
                    errorMessage = response.error ?: "Metadata service unavailable. Retry your search."
                )
            }
        }
    }
}
