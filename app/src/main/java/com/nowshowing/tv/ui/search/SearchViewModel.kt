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
        _uiState.value = _uiState.value.copy(query = _uiState.value.query + c)
    }

    fun onBackspace() {
        val q = _uiState.value.query
        if (q.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(query = q.dropLast(1))
        }
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(query = "", results = emptyList(), errorMessage = null)
    }

    fun setTypeFilter(type: String) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        if (_uiState.value.query.isNotBlank()) {
            executeSearch(_uiState.value.query, reset = true)
        }
    }

    fun executeSearch(query: String = _uiState.value.query, reset: Boolean = true) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            repository.addRecentSearch(trimmed)
            loadRecentSearches()

            val targetPage = if (reset) 1 else _uiState.value.currentPage + 1
            _uiState.value = _uiState.value.copy(
                query = trimmed,
                isLoading = true,
                errorMessage = null,
                currentPage = targetPage
            )

            val filterType = when (_uiState.value.selectedType) {
                "movie" -> "movie"
                "series" -> "series"
                else -> null
            }

            val response = repository.search(trimmed, page = targetPage, type = filterType)
            if (response.response != "False" && response.search != null) {
                val updated = if (reset) response.search else _uiState.value.results + response.search
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = updated,
                    totalResults = response.totalResults?.toIntOrNull() ?: updated.size
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = if (reset) emptyList() else _uiState.value.results,
                    errorMessage = if (reset) "No titles found matching \"$trimmed\"" else null
                )
            }
        }
    }
}
