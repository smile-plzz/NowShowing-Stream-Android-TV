package com.nowshowing.tv.playback

import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.domain.source.VideoSource

data class EpisodeInfo(
    val season: Int,
    val episode: Int
)

class PlaybackCoordinator(
    private val resolver: MediaSourceResolver = MediaSourceResolver()
) {

    /**
     * Attempts resolution for the requested source. If incompatible and autoFallback is true,
     * attempts to find the first compatible native source.
     */
    fun resolvePlayback(
        source: VideoSource,
        imdbId: String,
        title: String,
        season: Int = 1,
        episode: Int = 1,
        isSeries: Boolean = false,
        resumePositionMs: Long = 0L,
        autoFallback: Boolean = false
    ): ResolutionResult {
        val result = resolver.resolve(
            source = source,
            imdbId = imdbId,
            title = title,
            season = season,
            episode = episode,
            isSeries = isSeries,
            resumePositionMs = resumePositionMs
        )

        if (result is ResolutionResult.IncompatibleSource && autoFallback) {
            val fallbackSource = resolver.findFirstCompatibleSource()
            if (fallbackSource != null && fallbackSource.name != source.name) {
                return resolver.resolve(
                    source = fallbackSource,
                    imdbId = imdbId,
                    title = title,
                    season = season,
                    episode = episode,
                    isSeries = isSeries,
                    resumePositionMs = resumePositionMs
                )
            }
        }

        return result
    }

    /**
     * Calculates the next episode in a series with optional season boundary rollover.
     * When [maxEpisodesInSeason] is specified and current [episode] >= [maxEpisodesInSeason],
     * rolls over to the next season (e.g. S1E10 -> S2E1).
     */
    fun getNextEpisode(season: Int, episode: Int, maxEpisodesInSeason: Int? = null): EpisodeInfo {
        return if (maxEpisodesInSeason != null && episode >= maxEpisodesInSeason) {
            EpisodeInfo(season = season + 1, episode = 1)
        } else {
            EpisodeInfo(season = season, episode = episode + 1)
        }
    }

    /**
     * Finds the next compatible source as a fallback when current source fails.
     */
    fun resolveFallbackSource(currentSource: VideoSource): VideoSource? {
        return SourceRegistry.getNextCompatibleSource(currentSource).takeIf { it.name != currentSource.name }
    }

    /**
     * Calculates the previous episode in a series (minimum Episode 1).
     */
    fun getPreviousEpisode(season: Int, episode: Int): EpisodeInfo? {
        if (episode <= 1) return null
        return EpisodeInfo(season = season, episode = episode - 1)
    }

    /**
     * Checks if current playback is within the 30-second window before video end.
     */
    fun isEligibleForNextEpisodePrompt(positionMs: Long, durationMs: Long): Boolean {
        if (durationMs <= 0L || positionMs <= 0L) return false
        val remainingMs = durationMs - positionMs
        return remainingMs in 1..30_000L
    }

    /**
     * Calculates clamped seek position.
     */
    fun calculateSeekPosition(currentMs: Long, durationMs: Long, deltaMs: Long): Long {
        val target = currentMs + deltaMs
        return target.coerceIn(0L, durationMs.coerceAtLeast(0L))
    }
}
