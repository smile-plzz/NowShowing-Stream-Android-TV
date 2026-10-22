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
    val sortMode: SortMode = SortMode.FEATURED, val message: String? = null,
    val genre: String? = null, val year: Int? = null, val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(), val totalLoaded: Int = 0
)

class BrowseViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: MediaRepository = MediaRepository(application)
) : AndroidViewModel(application) {
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
            try {
            val titles = if (kind == "tv") repository.catalogSeeds("series", MediaRepository.POPULAR_TV) else repository.catalogSeeds("movies", MediaRepository.POPULAR_MOVIES)
            val type = if (kind == "tv") "series" else "movie"
            val retained = rawItems
            val result = repository.lookupTitlesProgressively(titles, type) { partial ->
                rawItems = partial + retained.filterNot { old -> partial.any { it.imdbID == old.imdbID } }
                applySort()
            }
            if (result.isNotEmpty()) rawItems = result
            _uiState.value = _uiState.value.copy(isLoading = false, message = MetadataHealth.failure.value?.message)
            applySort()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (e: Exception) {
                com.nowshowing.tv.diagnostics.CrashDiagnostics.record(getApplication(), "Browse loading failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Could not load titles. Please retry.")
            }
        }
    }
    fun setSortMode(mode: SortMode) { _uiState.value = _uiState.value.copy(sortMode = mode); applySort() }
    fun setGenre(genre: String?) { _uiState.value = _uiState.value.copy(genre = genre); applySort() }
    fun setYear(year: Int?) { _uiState.value = _uiState.value.copy(year = year); applySort() }
    fun clearFilters() { _uiState.value = _uiState.value.copy(genre = null, year = null); applySort() }
    private fun applySort() {
        val state = _uiState.value
        fun genres(item: OmdbTitleDetail) = item.genre.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() && it != "N/A" }
        val filtered = rawItems.filter { (state.genre == null || state.genre in genres(it)) && (state.year == null || startYear(it.year) == state.year) }
        _uiState.value = state.copy(items = sortTitles(filtered, state.sortMode), totalLoaded = rawItems.size,
            genres = rawItems.flatMap { genres(it) }.distinct().sorted(), years = rawItems.map { startYear(it.year) }.filter { it > 0 }.distinct().sortedDescending())
    }
}
