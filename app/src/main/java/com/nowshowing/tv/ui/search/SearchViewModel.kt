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
import kotlinx.coroutines.ensureActive

data class SearchUiState(
    val query: String = "",
    val year: Int? = null,
    val selectedType: String = "all", // "all", "movie", "series"
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val results: List<OmdbTitleShort> = emptyList(),
    val totalResults: Int = 0,
    val resultQuery: String = "",
    val currentPage: Int = 1,
    val errorMessage: String? = null
)

class SearchViewModel(application: Application, private val repository: MediaRepository) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, MediaRepository(application))

    private var searchJob: Job? = null
    private var failedPage: Int? = null
    private var resultIdentity: Triple<String, String, Int?>? = null
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadRecentSearches()
    }

    fun loadRecentSearches() {
        viewModelScope.launch {
            val recent = repository.getRecentSearches()
            _uiState.value = _uiState.value.copy(recentSearches = recent)
        }
    }

    fun setQuery(query: String) {
        searchJob?.cancel()
        failedPage = null
        _uiState.value = _uiState.value.copy(query = query.take(200), isLoading = false, errorMessage = null)
    }

    fun onCharInput(c: Char) {
        searchJob?.cancel()
        setQuery(_uiState.value.query + c)
    }

    fun onBackspace() = setQuery(_uiState.value.query.dropLast(1))

    fun onClear() {
        searchJob?.cancel()
        resultIdentity = null
        failedPage = null
        _uiState.value = _uiState.value.copy(query = "", resultQuery = "", results = emptyList(),
            totalResults = 0, currentPage = 1, isLoading = false, errorMessage = null)
    }

    fun setTypeFilter(type: String) {
        if (type !in listOf("all", "movie", "series") || type == _uiState.value.selectedType) return
        searchJob?.cancel()
        failedPage = null
        resultIdentity = null
        _uiState.value = _uiState.value.copy(selectedType = type, results = emptyList(), totalResults = 0, resultQuery = "", currentPage = 1, isLoading = false, errorMessage = null)
        if (_uiState.value.query.isNotBlank()) {
            executeSearch(_uiState.value.query, reset = true)
        }
    }

    fun setYearFilter(year: Int?) {
        if (year != null && year !in 1888..2100) return
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(year = year, results = emptyList(), totalResults = 0, currentPage = 1, isLoading = false, errorMessage = null, resultQuery = "")
        resultIdentity = null
        failedPage = null
        if (_uiState.value.query.isNotBlank()) executeSearch(reset = true)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(selectedType = "all")
        setYearFilter(null)
    }

    fun retrySearch() {
        com.nowshowing.tv.data.repository.MetadataHealth.retry()
        executeSearch(reset = failedPage?.let { it <= 1 } ?: true)
    }

    fun executeSearch(query: String = _uiState.value.query, reset: Boolean = true) {
        val trimmed = query.trim().take(200)
        val identity = Triple(trimmed, _uiState.value.selectedType, _uiState.value.year)
        val effectiveReset = reset || resultIdentity != identity
        if (trimmed.isBlank()) return

        if (!reset && _uiState.value.isLoading) return
        searchJob?.cancel()
        val targetPage = if (effectiveReset) 1 else _uiState.value.currentPage + 1
        failedPage = null
        // Publish synchronously: repeated remote clicks must see the request as busy.
        _uiState.value = _uiState.value.copy(
                query = trimmed,
                isLoading = true,
                errorMessage = null,
                results = if (effectiveReset) emptyList() else _uiState.value.results,
                totalResults = if (effectiveReset) 0 else _uiState.value.totalResults
            )
        searchJob = viewModelScope.launch {
            repository.addRecentSearch(trimmed)
            loadRecentSearches()
            val filterType = when (identity.second) {
                "movie" -> "movie"
                "series" -> "series"
                else -> null
            }

            val response = repository.search(trimmed, page = targetPage, type = filterType, year = identity.third)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (identity != Triple(_uiState.value.query.trim(), _uiState.value.selectedType, _uiState.value.year)) return@launch
            if (response.response != "False" && response.search != null) {
                resultIdentity = identity
                val updated = (if (effectiveReset) response.search else _uiState.value.results + response.search).distinctBy { it.imdbID }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = updated,
                    errorMessage = if (response.isStale) "Showing expired cached search results. ${com.nowshowing.tv.data.repository.MetadataHealth.failure.value?.message.orEmpty()}" else null,
                    currentPage = targetPage,
                    resultQuery = trimmed,
                    totalResults = response.totalResults?.toIntOrNull() ?: updated.size
                )
            } else {
                failedPage = targetPage
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = _uiState.value.results,
                    errorMessage = (response.error ?: "Metadata service unavailable. Retry your search.").let {
                        if (_uiState.value.results.isNotEmpty()) "$it Previous search results are retained." else it
                    }
                )
            }
        }
    }
}

