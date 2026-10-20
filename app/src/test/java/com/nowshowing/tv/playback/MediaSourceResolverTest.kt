package com.nowshowing.tv.playback

import com.nowshowing.tv.domain.source.MediaFormat
import com.nowshowing.tv.domain.source.SourceRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSourceResolverTest {

    private val resolver = MediaSourceResolver()

    @Test
    fun testRejectionOfUnsupportedWebEmbedSources() {
        val webEmbedSource = SourceRegistry.UNSUPPORTED_WEB_SOURCES.first { it.name == "VidSrc.to" }
        val result = resolver.resolve(
            source = webEmbedSource,
            imdbId = "tt1375666",
            title = "Inception"
        )

        assertTrue("Unsupported web embeds must return IncompatibleSource", result is ResolutionResult.IncompatibleSource)
        val incompatible = result as ResolutionResult.IncompatibleSource
        assertEquals(webEmbedSource.name, incompatible.source.name)
        assertTrue(incompatible.reason.contains("Native playback is unavailable for this source"))
    }

    @Test
    fun testResolutionOfNativeHlsSource() {
        val hlsSource = com.nowshowing.tv.domain.source.VideoSource(id = "test_hls", name = "Fixture HLS", type = com.nowshowing.tv.domain.source.SourceType.NATIVE_STREAM, directStreamUrl = "https://example.org/fixture.m3u8")
        val result = resolver.resolve(
            source = hlsSource,
            imdbId = "tt1375666",
            title = "Inception",
            resumePositionMs = 45000L
        )

        assertTrue("Native HLS source must resolve successfully", result is ResolutionResult.Success)
        val success = result as ResolutionResult.Success
        assertNotNull(success.playableSource)
        assertEquals(MediaFormat.HLS, success.playableSource.format)
        assertEquals("Inception", success.playableSource.title)
        assertEquals(45000L, success.playableSource.startPositionMs)
        assertTrue(success.playableSource.mediaUri.startsWith("https://"))
    }

    @Test
    fun testResolutionOfNativeMp4Source() {
        val mp4Source = com.nowshowing.tv.domain.source.VideoSource(id = "test_mp4", name = "Fixture MP4", type = com.nowshowing.tv.domain.source.SourceType.NATIVE_STREAM, directStreamUrl = "https://example.org/fixture.mp4")
        val result = resolver.resolve(
            source = mp4Source,
            imdbId = "tt0903747",
            title = "Breaking Bad",
            season = 2,
            episode = 4,
            isSeries = true
        )

        assertTrue(result is ResolutionResult.Success)
        val success = result as ResolutionResult.Success
        assertEquals(MediaFormat.MP4, success.playableSource.format)
        assertEquals(2, success.playableSource.season)
        assertEquals(4, success.playableSource.episode)
        assertTrue(success.playableSource.isSeries)
    }

    @Test
    fun testPlaybackCoordinatorNextAndPreviousEpisodes() {
        val coordinator = PlaybackCoordinator(resolver)

        val next = coordinator.getNextEpisode(season = 2, episode = 3)
        assertEquals(2, next.season)
        assertEquals(4, next.episode)

        val prev = coordinator.getPreviousEpisode(season = 2, episode = 3)
        assertNotNull(prev)
        assertEquals(2, prev!!.season)
        assertEquals(2, prev.episode)

        val noPrev = coordinator.getPreviousEpisode(season = 1, episode = 1)
        assertEquals(null, noPrev)
    }

    @Test
    fun testNextEpisodePromptEligibility() {
        val coordinator = PlaybackCoordinator(resolver)

        // 20s remaining -> eligible (within 30s)
        assertTrue(coordinator.isEligibleForNextEpisodePrompt(positionMs = 100_000L, durationMs = 120_000L))

        // 60s remaining -> not yet eligible
        assertTrue(!coordinator.isEligibleForNextEpisodePrompt(positionMs = 60_000L, durationMs = 120_000L))

        // Position 0 or Duration 0 -> not eligible
        assertTrue(!coordinator.isEligibleForNextEpisodePrompt(positionMs = 0L, durationMs = 0L))
    }
}
