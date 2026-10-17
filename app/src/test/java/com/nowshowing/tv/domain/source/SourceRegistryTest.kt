package com.nowshowing.tv.domain.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRegistryTest {

    @Test
    fun testAllSourcesPresent() {
        assertEquals("Must contain 4 native sources + 11 legacy embed sources", 15, SourceRegistry.SOURCES.size)
        assertEquals(4, SourceRegistry.NATIVE_SOURCES.size)
        assertEquals(11, SourceRegistry.UNSUPPORTED_WEB_SOURCES.size)
    }

    @Test
    fun testNativeCompatibilityClassification() {
        // Native sources must be classified as compatible
        for (nativeSource in SourceRegistry.NATIVE_SOURCES) {
            assertTrue("Native source ${nativeSource.name} must be native-compatible", nativeSource.isNativeCompatible)
            assertEquals(SourceCompatibility.NATIVE_PLAYABLE, nativeSource.compatibility)
        }

        // Legacy 11 sources must be classified as unsupported web embeds
        for (webSource in SourceRegistry.UNSUPPORTED_WEB_SOURCES) {
            assertFalse("Web source ${webSource.name} must NOT be native-compatible", webSource.isNativeCompatible)
            assertEquals(SourceCompatibility.UNSUPPORTED_WEB_ONLY, webSource.compatibility)
            assertEquals(SourceType.UNSUPPORTED_WEB_EMBED, webSource.type)
        }
    }

    @Test
    fun testBuildNativeMediaUrlReturnsDirectStreamForNativeSourcesOnly() {
        val nativeHls = SourceRegistry.NATIVE_SOURCES.first { it.id == "native_mux_hls" }
        val streamUrl = SourceRegistry.buildNativeMediaUrl(nativeHls, "tt1375666")
        assertNotNull(streamUrl)
        assertTrue(streamUrl!!.endsWith(".m3u8"))

        // For web sources, buildNativeMediaUrl MUST return null
        val vidsrc = SourceRegistry.UNSUPPORTED_WEB_SOURCES.first { it.name == "VidSrc.to" }
        val webStreamUrl = SourceRegistry.buildNativeMediaUrl(vidsrc, "tt1375666")
        assertNull("Web-only embeds must return null from buildNativeMediaUrl", webStreamUrl)
    }

    @Test
    fun testMovieUrlConstructionForLegacyReference() {
        val vidsrc = SourceRegistry.SOURCES.first { it.name == "VidSrc.to" }
        val url = SourceRegistry.buildVideoUrl(vidsrc, "tt1375666", isSeries = false)
        assertNotNull(url)
        assertEquals("https://vidsrc.to/embed/movie/tt1375666", url)
    }

    @Test
    fun testSeriesUrlConstructionForLegacyReference() {
        val vidsrc = SourceRegistry.SOURCES.first { it.name == "VidSrc.to" }
        val url = SourceRegistry.buildVideoUrl(vidsrc, "tt0903747", season = 2, episode = 5, isSeries = true)
        assertNotNull(url)
        assertEquals("https://vidsrc.to/embed/tv/tt0903747/2/5", url)
    }

    @Test
    fun testNextCompatibleSourceCyclesOnlyCompatibleSources() {
        val firstNative = SourceRegistry.NATIVE_SOURCES.first()
        val next = SourceRegistry.getNextCompatibleSource(firstNative)
        assertTrue(next.isNativeCompatible)

        val lastNative = SourceRegistry.NATIVE_SOURCES.last()
        val wrapped = SourceRegistry.getNextCompatibleSource(lastNative)
        assertEquals(firstNative.name, wrapped.name)
    }
}
