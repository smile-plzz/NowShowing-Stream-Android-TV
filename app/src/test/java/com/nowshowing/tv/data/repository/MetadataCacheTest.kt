package com.nowshowing.tv.data.repository

import org.junit.Assert.*
import org.junit.Test

class MetadataCacheTest {
    @Test fun recentlyAccessedEntrySurvivesCapacityEviction() {
        val cache = MetadataCache<String>(capacity = 2)
        cache.put("a", "first"); cache.put("b", "second")
        assertEquals("first", cache.get("a"))
        cache.put("c", "third")
        assertNull(cache.get("b"))
        assertEquals("first", cache.get("a"))
        assertEquals("third", cache.get("c"))
    }
    @Test fun expiryIsNotExtendedByReading() {
        var time = 0L
        val cache = MetadataCache<String>(ttlMs = 100, now = { time })
        cache.put("a", "movie")
        time = 99
        assertEquals("movie", cache.get("a"))
        time = 100
        assertNull(cache.get("a"))
    }
    @Test fun replacementGetsItsOwnExpiry() {
        var time = 0L
        val cache = MetadataCache<String>(ttlMs = 100, now = { time })
        cache.put("a", "old")
        time = 90
        cache.put("a", "new")
        time = 100
        assertEquals("new", cache.get("a"))
        time = 190
        assertNull(cache.get("a"))
    }
    @Test fun concurrentReadersAndWritersStayBoundedAndConsistent() {
        val cache = MetadataCache<String>(capacity = 8)
        val failure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
        val threads = (0..7).map { worker ->
            Thread {
                try { repeat(1000) { n ->
                    val key = "$worker:$n"
                    cache.put(key, key)
                    cache.get(key)?.let { assertEquals(key, it) }
                } } catch (error: Throwable) { failure.compareAndSet(null, error) }
            }
        }
        threads.forEach { it.start() }; threads.forEach { it.join() }
        failure.get()?.let { throw it }
        val retained = (0..7).flatMap { worker -> (0..999).map { "$worker:$it" } }
            .count { cache.get(it) != null }
        assertTrue(retained <= 8)
    }
}
