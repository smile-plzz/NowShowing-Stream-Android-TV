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
    @Test fun getNextSourceCyclesThroughAllProvidersInOrder() {
        var current = SourceRegistry.SOURCES.first()
        val visited = mutableListOf(current)
        for (i in 1 until SourceRegistry.SOURCES.size) {
            current = SourceRegistry.getNextSource(current)
            visited.add(current)
            assertEquals(SourceRegistry.SOURCES[i], current)
        }
        assertEquals(SourceRegistry.SOURCES.size, visited.distinct().size)
    }
    @Test fun getNextSourceWrapsAroundAtEndOfList() {
        val lastSource = SourceRegistry.SOURCES.last()
        val wrapped = SourceRegistry.getNextSource(lastSource)
        assertEquals(SourceRegistry.SOURCES.first(), wrapped)
    }
    @Test fun getNextSourceWithUnknownSourceDefaultsToFirst() {
        val unknown = VideoSource(
            id = "non_existent_source",
            name = "NonExistent",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://example.com/movie/"
        )
        val result = SourceRegistry.getNextSource(unknown)
        assertEquals(SourceRegistry.SOURCES.first(), result)
    }
    @Test fun getNextSourcePreservesVideoUrlParameters() {
        val first = SourceRegistry.SOURCES.first()
        val next = SourceRegistry.getNextSource(first)
        val url = SourceRegistry.buildVideoUrl(next, "tt1375666", 3, 7, isSeries = true)
        assertNotNull(url)
        assertTrue(url!!.contains("tt1375666"))
        assertTrue(url.contains("3") && url.contains("7"))
    }
}
