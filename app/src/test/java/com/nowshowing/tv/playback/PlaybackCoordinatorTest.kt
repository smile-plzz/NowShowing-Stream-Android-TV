package com.nowshowing.tv.playback

import com.nowshowing.tv.domain.source.SourceRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackCoordinatorTest {

    private lateinit var coordinator: PlaybackCoordinator
    private lateinit var resolver: MediaSourceResolver

    @Before
    fun setUp() {
        resolver = MediaSourceResolver()
        coordinator = PlaybackCoordinator(resolver)
    }

    @Test
    fun testResolvePlaybackWithAutoFallback() {
        // Given an unsupported web embed source, autoFallback=true should find the first native source
        val webSource = SourceRegistry.UNSUPPORTED_WEB_SOURCES.first { it.name == "VidSrc.to" }
        val result = coordinator.resolvePlayback(
            source = webSource,
            imdbId = "tt1375666",
            title = "Inception",
            autoFallback = true
        )

        assertTrue("No registered demo fallback is allowed", result is ResolutionResult.IncompatibleSource)
    }

    @Test
    fun testResolvePlaybackWithoutAutoFallbackRejectsWebEmbed() {
        val webSource = SourceRegistry.UNSUPPORTED_WEB_SOURCES.first { it.name == "VidSrc.to" }
        val result = coordinator.resolvePlayback(
            source = webSource,
            imdbId = "tt1375666",
            title = "Inception",
            autoFallback = false
        )

        assertTrue(result is ResolutionResult.IncompatibleSource)
        val incompatible = result as ResolutionResult.IncompatibleSource
        assertTrue(incompatible.reason.contains("Native playback is unavailable"))
    }

    @Test
    fun testEpisodeSequencingMultiSeason() {
        val next = coordinator.getNextEpisode(season = 1, episode = 10)
        assertEquals(1, next.season)
        assertEquals(11, next.episode)

        val prev = coordinator.getPreviousEpisode(season = 2, episode = 2)
        assertNotNull(prev)
        assertEquals(2, prev!!.season)
        assertEquals(1, prev.episode)

        val noPrev = coordinator.getPreviousEpisode(season = 1, episode = 1)
        assertNull(noPrev)
    }

    @Test
    fun testSeekPositionClamping() {
        val durationMs = 120_000L // 2 minutes

        // Seek forward 10s from 30s -> 40s
        assertEquals(40_000L, coordinator.calculateSeekPosition(30_000L, durationMs, 10_000L))

        // Seek backward 10s from 5s -> clamped to 0
        assertEquals(0L, coordinator.calculateSeekPosition(5_000L, durationMs, -10_000L))

        // Seek forward 30s from 110s -> clamped to duration (120s)
        assertEquals(120_000L, coordinator.calculateSeekPosition(110_000L, durationMs, 30_000L))
    }

    @Test
    fun testNextEpisodePromptWindow() {
        val durationMs = 100_000L

        // 15 seconds remaining -> inside 30-second prompt window
        assertTrue(coordinator.isEligibleForNextEpisodePrompt(positionMs = 85_000L, durationMs = durationMs))

        // Exactly 30 seconds remaining -> inside prompt window
        assertTrue(coordinator.isEligibleForNextEpisodePrompt(positionMs = 70_000L, durationMs = durationMs))

        // 35 seconds remaining -> not yet eligible
        assertTrue(!coordinator.isEligibleForNextEpisodePrompt(positionMs = 65_000L, durationMs = durationMs))

        // Ended or 0 duration
        assertTrue(!coordinator.isEligibleForNextEpisodePrompt(positionMs = 100_000L, durationMs = 100_000L))
        assertTrue(!coordinator.isEligibleForNextEpisodePrompt(positionMs = 0L, durationMs = 0L))
    }

    @Test
    fun testSeasonBoundaryRollover() {
        // Season finale rollover: S1E10 with 10 episodes in season -> S2E1
        val next = coordinator.getNextEpisode(season = 1, episode = 10, maxEpisodesInSeason = 10)
        assertEquals(2, next.season)
        assertEquals(1, next.episode)

        // Mid-season progression: S1E9 with 10 episodes -> S1E10
        val midNext = coordinator.getNextEpisode(season = 1, episode = 9, maxEpisodesInSeason = 10)
        assertEquals(1, midNext.season)
        assertEquals(10, midNext.episode)
    }

    @Test
    fun testResolveFallbackSource() {
        val current = SourceRegistry.SOURCES.first()
        val fallback = coordinator.resolveFallbackSource(current)
        assertNull(fallback)
    }
}
