package com.nowshowing.tv.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.data.repository.MediaRepository
import com.nowshowing.tv.data.repository.MetadataHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

enum class SortMode(val label: String) {
    FEATURED("Featured"), TITLE("Title A–Z"), NEWEST("Start year ↓"), TOP_RATED("Rating ↓")
}

/** Series ranges use their first year; unknown dates sort last. */
fun startYear(year: String): Int = Regex("^\\s*([0-9]{4})(?:\\D|$)").find(year)?.groupValues?.get(1)?.toIntOrNull() ?: 0
fun sortTitles(items: List<OmdbTitleDetail>, mode: SortMode): List<OmdbTitleDetail> = when (mode) {
    SortMode.FEATURED -> items
    SortMode.TITLE -> items.sortedBy { it.title.lowercase() }
    SortMode.NEWEST -> items.sortedWith(compareByDescending<OmdbTitleDetail> { startYear(it.year) }.thenBy { it.title.lowercase() })
    SortMode.TOP_RATED -> items.sortedWith(compareByDescending<OmdbTitleDetail> { it.imdbRating?.toFloatOrNull() ?: -1f }.thenBy { it.title.lowercase() })
}

data class BrowseUiState(
    val kind: String = "movies", val title: String = "Curated Movies",
    val isLoading: Boolean = true, val items: List<OmdbTitleDetail> = emptyList(),
    val sortMode: SortMode = SortMode.FEATURED, val message: String? = null
)

class BrowseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState = _uiState.asStateFlow()
    private var browseJob: Job? = null
    private var rawItems: List<OmdbTitleDetail> = emptyList()

    fun retryBrowse(kind: String) { MetadataHealth.retry(); loadBrowse(kind, force = true) }
    fun loadBrowse(kind: String, force: Boolean = false) {
        if (!force && _uiState.value.kind == kind && (rawItems.isNotEmpty() || browseJob?.isActive == true)) return
        browseJob?.cancel()
        if (_uiState.value.kind != kind) rawItems = emptyList()
        _uiState.value = _uiState.value.copy(kind = kind, title = if (kind == "tv") "Curated TV Shows" else "Curated Movies",
            isLoading = true, items = sortTitles(rawItems, _uiState.value.sortMode), message = null)
        browseJob = viewModelScope.launch {
            val titles = if (kind == "tv") MediaRepository.POPULAR_TV else MediaRepository.POPULAR_MOVIES
            val type = if (kind == "tv") "series" else "movie"
            val retained = rawItems
            val result = repository.lookupTitlesProgressively(titles, type) { partial ->
                rawItems = partial + retained.filterNot { old -> partial.any { it.imdbID == old.imdbID } }
                applySort()
            }
            if (result.isNotEmpty()) rawItems = result + retained.filterNot { old -> result.any { it.imdbID == old.imdbID } }
            _uiState.value = _uiState.value.copy(isLoading = false, message = MetadataHealth.failure.value?.message)
            applySort()
        }
    }
    fun setSortMode(mode: SortMode) { _uiState.value = _uiState.value.copy(sortMode = mode); applySort() }
    private fun applySort() { _uiState.value = _uiState.value.copy(items = sortTitles(rawItems, _uiState.value.sortMode)) }
}
