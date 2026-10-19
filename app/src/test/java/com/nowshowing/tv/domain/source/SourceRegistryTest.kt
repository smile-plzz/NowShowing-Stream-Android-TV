package com.nowshowing.tv.domain.source

import org.junit.Assert.*
import org.junit.Test

class SourceRegistryTest {
    @Test fun productionRegistryContainsOnlyWebProviders() {
        assertEquals(11, SourceRegistry.SOURCES.size)
        assertTrue(SourceRegistry.getNativeCompatibleSources().isEmpty())
        assertTrue(SourceRegistry.SOURCES.all { !it.isNativeCompatible && it.directStreamUrl == null })
        assertFalse(SourceRegistry.SOURCES.any { it.id.startsWith("native_") })
    }
    @Test fun webProvidersNeverResolveToNativeMedia() {
        SourceRegistry.SOURCES.forEach { assertNull(SourceRegistry.buildNativeMediaUrl(it, "tt1375666")) }
    }
    @Test fun movieAndSeriesRoutesRemainIntact() {
        val source = SourceRegistry.SOURCES.first { it.id == "vidsrc_to" }
        assertEquals("https://vidsrc.to/embed/movie/tt1375666", SourceRegistry.buildVideoUrl(source, "tt1375666"))
        assertEquals("https://vidsrc.to/embed/tv/tt0903747/2/5", SourceRegistry.buildVideoUrl(source, "tt0903747", 2, 5, true))
    }
    @Test fun noCompatibleFallbackLeavesOriginalSource() {
        val source = SourceRegistry.SOURCES.first()
        assertEquals(source, SourceRegistry.getNextCompatibleSource(source))
    }
}
