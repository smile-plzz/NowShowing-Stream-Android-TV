package com.nowshowing.tv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.repository.MediaRepository
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.domain.source.VideoSource
import com.nowshowing.tv.playback.PlaybackCoordinator
import com.nowshowing.tv.playback.PlayableSource
import com.nowshowing.tv.playback.ResolutionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.nowshowing.tv.data.db.RecentItem
import com.nowshowing.tv.data.model.OmdbEpisode

data class PlayerUiState(
    val imdbId: String = "",
    val title: String = "",
    val poster: String = "",
    val year: String = "",
    val type: String = "movie",
    val season: Int = 1,
    val episode: Int = 1,
    val currentSource: VideoSource = SourceRegistry.NATIVE_SOURCES.first(),
    val playableSource: PlayableSource? = null,
    val webEmbedUrl: String? = null,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = true,
    val isBuffering: Boolean = false,
    val isIncompatibleSource: Boolean = false,
    val errorMessage: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val seekFeedbackText: String? = null,
    val showNextEpisodePrompt: Boolean = false,
    val showSourceSelector: Boolean = false,
    val showControls: Boolean = true,
    val isNetworkError: Boolean = false,
    val retryCount: Int = 0,
    val showSeriesComplete: Boolean = false,
    val episodeTitle: String = "",
    val episodeList: List<OmdbEpisode> = emptyList(),
    val episodeListSeason: Int = 1,
    val totalSeasons: Int = 1,
    val episodesLoading: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val coordinator = PlaybackCoordinator()
    private var historyJob: Job? = null
    private var initializeJob: Job? = null
    private var episodesJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initialize(imdbId: String, season: Int, episode: Int, type: String) {
        initializeJob?.cancel()
        if (type == "diagnostic") {
            val source = SourceRegistry.NATIVE_SOURCES.firstOrNull { it.id == imdbId } ?: return
            _uiState.value = PlayerUiState(imdbId = imdbId, title = source.name, type = type, currentSource = source)
            resolveAndLoad(source)
            return
        }
        initializeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                imdbId = imdbId,
                season = season,
                episode = episode,
                type = type,
                isLoading = true,
                errorMessage = null,
                isIncompatibleSource = false
            )

            val detail = repository.getDetails(imdbId)
            val lastOpened = repository.getRecentlyOpened().firstOrNull { it.imdbID == imdbId }
            val titleName = detail?.title ?: lastOpened?.title ?: "NowShowing"
            val posterUrl = detail?.poster ?: lastOpened?.poster ?: ""
            val yearVal = detail?.year ?: lastOpened?.year ?: ""

            // Retrieve saved playback position from continue watching if available
            val continueList = repository.getContinueWatching()
            val savedEntry = continueList.firstOrNull { it.imdbID == imdbId }
            val resumeMs = savedEntry?.takeIf { it.season == season && it.episode == episode }?.positionMs ?: 0L

            val preferredSource = SourceRegistry.getSourceForTitle(getApplication(), imdbId)

            _uiState.value = _uiState.value.copy(
                title = titleName,
                poster = posterUrl,
                year = yearVal,
                totalSeasons = detail?.totalSeasons?.toIntOrNull()?.coerceAtLeast(1) ?: season,
                currentSource = preferredSource,
                currentPositionMs = resumeMs
            )

            resolveAndLoad(preferredSource, resumeMs)
            if (type == "series") loadEpisodes(season)
        }
    }

    private fun resolveAndLoad(source: VideoSource, resumeMs: Long = 0L) {
        val s = _uiState.value
        if (!source.isNativeCompatible) {
            val url = SourceRegistry.buildVideoUrl(source, s.imdbId, s.season, s.episode, s.type == "series")
            _uiState.value = s.copy(
                currentSource = source, playableSource = null, webEmbedUrl = url,
                isLoading = false, isIncompatibleSource = false, isNetworkError = false,
                currentPositionMs = 0L, durationMs = 0L, showNextEpisodePrompt = false,
                errorMessage = if (url == null) "This source has no player for this title." else null,
                showSourceSelector = false
            )
            if (url != null) recordOpened(source)
            return
        }
        _uiState.value = s.copy(webEmbedUrl = null, isNetworkError = false)
        val result = coordinator.resolvePlayback(
            source = source,
            imdbId = s.imdbId,
            title = s.title,
            season = s.season,
            episode = s.episode,
            isSeries = s.type == "series",
            resumePositionMs = resumeMs,
            autoFallback = false
        )

        when (result) {
            is ResolutionResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    currentSource = source,
                    playableSource = result.playableSource,
                    isLoading = false,
                    isIncompatibleSource = false,
                    errorMessage = null,
                    showSourceSelector = false
                )

                // Update database continue watching record
                if (s.type != "diagnostic") viewModelScope.launch {
                    repository.updateContinueWatching(
                        imdbId = s.imdbId,
                        title = s.title,
                        poster = s.poster,
                        year = s.year,
                        type = s.type,
                        season = s.season,
                        episode = s.episode,
                        positionMs = resumeMs,
                        durationMs = 0L
                    )
                }
            }
            is ResolutionResult.IncompatibleSource -> {
                _uiState.value = _uiState.value.copy(
                    currentSource = source,
                    playableSource = null,
                    isLoading = false,
                    isIncompatibleSource = true,
                    errorMessage = "Native playback is unavailable for this source (${source.name}). Try another source.",
                    showSourceSelector = false
                )
            }
            is ResolutionResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    currentSource = source,
                    playableSource = null,
                    isLoading = false,
                    isIncompatibleSource = false,
                    errorMessage = result.message,
                    showSourceSelector = false
                )
            }
        }
    }

    private fun recordOpened(source: VideoSource) {
        val s = _uiState.value
        SourceRegistry.setSourceForTitle(getApplication(), s.imdbId, source)
        val previous = historyJob
        historyJob = viewModelScope.launch {
            previous?.join()
            repository.recordRecentlyOpened(RecentItem(
                imdbID = s.imdbId, title = s.title, poster = s.poster, year = s.year,
                type = s.type, season = s.season, episode = s.episode, sourceId = source.id
            ))
        }
    }

    fun selectSource(source: VideoSource) {
        if (_uiState.value.type == "diagnostic") {
            _uiState.value = _uiState.value.copy(currentSource = source, title = source.name, isLoading = true, errorMessage = null, showSourceSelector = false)
            resolveAndLoad(source)
            return
        }
        SourceRegistry.setPreferredSource(getApplication(), source.name)
        SourceRegistry.setSourceForTitle(getApplication(), _uiState.value.imdbId, source)
        _uiState.value = _uiState.value.copy(
            currentSource = source,
            isLoading = true,
            errorMessage = null,
            isIncompatibleSource = false,
            showSourceSelector = false
        )
        resolveAndLoad(source, _uiState.value.currentPositionMs)
    }

    fun switchToNextCompatibleSource() {
        val next = SourceRegistry.getNextCompatibleSource(_uiState.value.currentSource)
        selectSource(next)
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        resolveAndLoad(_uiState.value.currentSource, _uiState.value.currentPositionMs)
    }

    fun loadEpisodes(season: Int) {
        episodesJob?.cancel()
        _uiState.value = _uiState.value.copy(episodeListSeason = season, episodesLoading = true, episodeList = emptyList())
        val id = _uiState.value.imdbId
        episodesJob = viewModelScope.launch {
            val episodes = repository.getSeason(id, season)?.episodes.orEmpty()
            val s = _uiState.value
            _uiState.value = s.copy(
                episodeList = episodes, episodesLoading = false,
                episodeTitle = if (s.season == season) episodes.firstOrNull { it.episode.toIntOrNull() == s.episode }?.title.orEmpty() else s.episodeTitle
            )
        }
    }

    fun playEpisode(season: Int, episode: Int) {
        if (season < 1 || episode < 1) return
        val s = _uiState.value
        val title = if (s.episodeListSeason == season) s.episodeList.firstOrNull { it.episode.toIntOrNull() == episode }?.title.orEmpty() else ""
        _uiState.value = s.copy(season = season, episode = episode, episodeTitle = title,
            currentPositionMs = 0L, durationMs = 0L, showNextEpisodePrompt = false, isLoading = true)
        resolveAndLoad(s.currentSource, 0L)
        if (s.episodeListSeason != season) loadEpisodes(season)
    }

    fun playNextEpisode() {
        val s = _uiState.value
        val knownEnd = if (s.episodeListSeason == s.season && s.episodeList.isNotEmpty())
            s.episodeList.mapNotNull { it.episode.toIntOrNull() }.maxOrNull() else null
        if (knownEnd != null && s.episode >= knownEnd && s.season >= s.totalSeasons) {
            _uiState.value = s.copy(showSeriesComplete = true)
            return
        }
        val next = coordinator.getNextEpisode(s.season, s.episode, knownEnd)
        playEpisode(next.season, next.episode)
    }

    fun playPreviousEpisode() {
        val s = _uiState.value
        val previous = coordinator.getPreviousEpisode(s.season, s.episode) ?: return
        playEpisode(previous.season, previous.episode)
    }

    fun updateProgress(positionMs: Long, durationMs: Long) {
        val s = _uiState.value
        val shouldPrompt = coordinator.isEligibleForNextEpisodePrompt(positionMs, durationMs) && s.type == "series"

        _uiState.value = s.copy(
            currentPositionMs = positionMs,
            durationMs = durationMs,
            showNextEpisodePrompt = shouldPrompt
        )

        if (s.type != "diagnostic") viewModelScope.launch {
            repository.updateContinueWatching(
                imdbId = s.imdbId,
                title = s.title,
                poster = s.poster,
                year = s.year,
                type = s.type,
                season = s.season,
                episode = s.episode,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    fun setBuffering(isBuffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = isBuffering)
    }

    fun setPlaying(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
    }

    fun setSeekFeedback(text: String?) {
        _uiState.value = _uiState.value.copy(seekFeedbackText = text)
    }

    fun openSourceSelector() {
        _uiState.value = _uiState.value.copy(showSourceSelector = true)
    }

    fun closeSourceSelector() {
        _uiState.value = _uiState.value.copy(showSourceSelector = false)
    }

    fun dismissNextEpisodePrompt() {
        _uiState.value = _uiState.value.copy(showNextEpisodePrompt = false)
    }

    fun onNetworkError() {
        _uiState.value = _uiState.value.copy(
            isNetworkError = true,
            isBuffering = false
        )
    }

    fun onPlaybackError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            playableSource = null,
            isLoading = false
        )
    }

    fun retryPlayback() {
        _uiState.value = _uiState.value.copy(
            isNetworkError = false,
            retryCount = _uiState.value.retryCount + 1,
            isLoading = true
        )
        resolveAndLoad(_uiState.value.currentSource, _uiState.value.currentPositionMs)
    }

    fun onMovieFinished() {
        _uiState.value = _uiState.value.copy(showSeriesComplete = true)
    }

    fun dismissCompletion() {
        _uiState.value = _uiState.value.copy(showSeriesComplete = false)
    }
}
