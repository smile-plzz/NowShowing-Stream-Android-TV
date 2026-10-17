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
    val showSeriesComplete: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val coordinator = PlaybackCoordinator()
    private var historyJob: Job? = null
    private var initializeJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initialize(imdbId: String, season: Int, episode: Int, type: String) {
        initializeJob?.cancel()
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
                currentSource = preferredSource,
                currentPositionMs = resumeMs
            )

            resolveAndLoad(preferredSource, resumeMs)
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
                viewModelScope.launch {
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

    fun playNextEpisode() {
        val s = _uiState.value
        val nextEp = coordinator.getNextEpisode(s.season, s.episode)
        _uiState.value = _uiState.value.copy(
            season = nextEp.season,
            episode = nextEp.episode,
            currentPositionMs = 0L,
            durationMs = 0L,
            showNextEpisodePrompt = false,
            isLoading = true
        )
        resolveAndLoad(_uiState.value.currentSource, 0L)
    }

    fun playPreviousEpisode() {
        val s = _uiState.value
        val prevEp = coordinator.getPreviousEpisode(s.season, s.episode) ?: return
        _uiState.value = _uiState.value.copy(
            season = prevEp.season,
            episode = prevEp.episode,
            currentPositionMs = 0L,
            durationMs = 0L,
            isLoading = true
        )
        resolveAndLoad(_uiState.value.currentSource, 0L)
    }

    fun updateProgress(positionMs: Long, durationMs: Long) {
        val s = _uiState.value
        val shouldPrompt = coordinator.isEligibleForNextEpisodePrompt(positionMs, durationMs) && s.type == "series"

        _uiState.value = s.copy(
            currentPositionMs = positionMs,
            durationMs = durationMs,
            showNextEpisodePrompt = shouldPrompt
        )

        viewModelScope.launch {
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
