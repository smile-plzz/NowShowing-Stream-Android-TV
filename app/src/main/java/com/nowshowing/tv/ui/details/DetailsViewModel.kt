package com.nowshowing.tv.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.model.OmdbEpisode
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.data.repository.MediaRepository
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.domain.source.VideoSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val imdbId: String = "",
    val type: String = "movie",
    val isLoading: Boolean = true,
    val detail: OmdbTitleDetail? = null,
    val isInWatchlist: Boolean = false,
    val totalSeasons: Int = 1,
    val selectedSeason: Int = 1,
    val episodes: List<OmdbEpisode> = emptyList(),
    val selectedEpisode: Int = 1,
    val selectedSource: VideoSource = SourceRegistry.SOURCES.first(),
    val recommendations: List<OmdbTitleDetail> = emptyList()
)

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(imdbId: String, type: String) {
        viewModelScope.launch {
            val prefSource = SourceRegistry.getPreferredSource(getApplication())
            val inWatchlist = repository.isInWatchlist(imdbId)

            _uiState.value = DetailsUiState(
                imdbId = imdbId,
                type = type,
                isLoading = true,
                isInWatchlist = inWatchlist,
                selectedSource = prefSource
            )

            val detail = repository.getDetails(imdbId)
            if (detail != null) {
                val isSeries = detail.type == "series" || type == "series"
                val numSeasons = detail.totalSeasons?.toIntOrNull() ?: 1

                _uiState.value = _uiState.value.copy(
                    detail = detail,
                    type = if (isSeries) "series" else "movie",
                    totalSeasons = numSeasons
                )

                // If series, load season 1 episodes
                if (isSeries) {
                    loadSeasonEpisodes(imdbId, 1)
                }

                // Load recommendations by primary genre
                val primaryGenre = detail.genre?.split(",")?.firstOrNull()?.trim()
                if (!primaryGenre.isNullOrBlank()) {
                    val recsResp = repository.search(primaryGenre, page = 1, type = if (isSeries) "series" else "movie")
                    val recsList = (recsResp.search ?: emptyList())
                        .filter { it.imdbID != imdbId }
                        .take(6)
                        .mapNotNull { repository.getDetails(it.imdbID) }
                    _uiState.value = _uiState.value.copy(recommendations = recsList)
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season, selectedEpisode = 1)
        loadSeasonEpisodes(_uiState.value.imdbId, season)
    }

    fun selectEpisode(episode: Int) {
        _uiState.value = _uiState.value.copy(selectedEpisode = episode)
    }

    private fun loadSeasonEpisodes(imdbId: String, season: Int) {
        viewModelScope.launch {
            val seasonData = repository.getSeason(imdbId, season)
            val eps = seasonData?.episodes ?: emptyList()
            _uiState.value = _uiState.value.copy(episodes = eps)
        }
    }

    fun toggleWatchlist() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val isNowInList = repository.toggleWatchlist(detail)
            _uiState.value = _uiState.value.copy(isInWatchlist = isNowInList)
        }
    }

    fun setSource(source: VideoSource) {
        _uiState.value = _uiState.value.copy(selectedSource = source)
        SourceRegistry.setPreferredSource(getApplication(), source.name)
    }
}
