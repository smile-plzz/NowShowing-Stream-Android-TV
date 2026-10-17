package com.nowshowing.tv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.db.ContinueItem
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.data.repository.GenreRowSeed
import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GenreRowState(
    val seed: GenreRowSeed,
    val items: List<OmdbTitleDetail>
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroTitle: OmdbTitleDetail? = null,
    val continueWatching: List<ContinueItem> = emptyList(),
    val trending: List<OmdbTitleDetail> = emptyList(),
    val genreRows: List<GenreRowState> = emptyList(),
    val popularMovies: List<OmdbTitleDetail> = emptyList(),
    val popularTv: List<OmdbTitleDetail> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Continue watching
            val cw = repository.getContinueWatching()

            // Trending
            val trendingTitles = repository.lookupTitles(MediaRepository.TRENDING_SEED)

            // Hero item: first trending or featured title
            val hero = trendingTitles.firstOrNull()

            // Genre rows
            val genreRows = MediaRepository.GENRE_ROWS.map { seed ->
                val items = repository.lookupTitles(seed.seed)
                GenreRowState(seed = seed, items = items)
            }

            // Popular Movies
            val popMovies = repository.lookupTitles(MediaRepository.POPULAR_MOVIES.take(14), type = "movie")

            // Popular TV
            val popTv = repository.lookupTitles(MediaRepository.POPULAR_TV.take(14), type = "series")

            _uiState.value = HomeUiState(
                isLoading = false,
                heroTitle = hero,
                continueWatching = cw,
                trending = trendingTitles,
                genreRows = genreRows,
                popularMovies = popMovies,
                popularTv = popTv
            )
        }
    }
}
