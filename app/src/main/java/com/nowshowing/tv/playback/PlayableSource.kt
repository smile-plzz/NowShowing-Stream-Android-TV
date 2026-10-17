package com.nowshowing.tv.playback

import com.nowshowing.tv.domain.source.MediaFormat
import com.nowshowing.tv.domain.source.VideoSource

data class PlayableSource(
    val source: VideoSource,
    val mediaUri: String,
    val format: MediaFormat,
    val title: String,
    val imdbId: String,
    val season: Int = 1,
    val episode: Int = 1,
    val isSeries: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val startPositionMs: Long = 0L
)

sealed class ResolutionResult {
    data class Success(val playableSource: PlayableSource) : ResolutionResult()

    data class IncompatibleSource(
        val source: VideoSource,
        val reason: String = "Native playback is unavailable for this source. Try another source."
    ) : ResolutionResult()

    data class Error(val message: String) : ResolutionResult()
}
