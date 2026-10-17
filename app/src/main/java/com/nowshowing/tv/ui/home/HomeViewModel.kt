package com.nowshowing.tv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.db.ContinueItem
import com.nowshowing.tv.data.db.RecentItem
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.data.repository.GenreRowSeed
import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GenreRowState(val seed: GenreRowSeed, val items: List<OmdbTitleDetail>)

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroTitle: OmdbTitleDetail? = null,
    val continueWatching: List<ContinueItem> = emptyList(),
    val recentlyOpened: List<RecentItem> = emptyList(),
    val trending: List<OmdbTitleDetail> = emptyList(),
    val genreRows: List<GenreRowState> = MediaRepository.GENRE_ROWS.map { GenreRowState(it, emptyList()) },
    val popularMovies: List<OmdbTitleDetail> = emptyList(),
    val popularTv: List<OmdbTitleDetail> = emptyList(),
    val message: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { loadHomeData() }

    fun refreshHistory() {
        viewModelScope.launch {
            val recent = repository.getRecentlyOpened()
            val native = repository.getContinueWatching()
            _uiState.update { it.copy(recentlyOpened = recent, continueWatching = native) }
        }
    }

    fun removeRecent(imdbId: String) {
        viewModelScope.launch { repository.removeRecentlyOpened(imdbId); refreshHistory() }
    }

    fun markWatched(item: RecentItem) {
        viewModelScope.launch { repository.setRecentlyWatched(item.imdbID, !item.watched); refreshHistory() }
    }

    fun removeContinue(imdbId: String) {
        viewModelScope.launch { repository.removeFromContinueWatching(imdbId); refreshHistory() }
    }

    fun loadHomeData() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            refreshHistory()
            // Each shelf publishes independently. Network concurrency is bounded in the repository.
            coroutineScope {
                launch {
                    val titles = repository.lookupTitles(MediaRepository.TRENDING_SEED)
                    _uiState.update { it.copy(trending = titles, heroTitle = titles.firstOrNull() ?: it.heroTitle) }
                }
                MediaRepository.GENRE_ROWS.forEach { seed ->
                    launch {
                        val titles = repository.lookupTitles(seed.seed)
                        _uiState.update { s ->
                            s.copy(genreRows = s.genreRows.map { if (it.seed.id == seed.id) GenreRowState(seed, titles) else it })
                        }
                    }
                }
                launch {
                    val titles = repository.lookupTitles(MediaRepository.POPULAR_MOVIES.take(14), "movie")
                    _uiState.update { it.copy(popularMovies = titles) }
                }
                launch {
                    val titles = repository.lookupTitles(MediaRepository.POPULAR_TV.take(14), "series")
                    _uiState.update { it.copy(popularTv = titles) }
                }
            }
            _uiState.update {
                val hasTitles = it.trending.isNotEmpty() || it.genreRows.any { row -> row.items.isNotEmpty() } ||
                    it.popularMovies.isNotEmpty() || it.popularTv.isNotEmpty()
                it.copy(isLoading = false, message = if (hasTitles) null else "Could not load titles. Check your connection and try again.")
            }
        }
    }
}
