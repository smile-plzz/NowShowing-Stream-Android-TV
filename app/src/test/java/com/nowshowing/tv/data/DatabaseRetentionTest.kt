package com.nowshowing.tv.data

import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.db.RecentItem
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class DatabaseRetentionTest {
    private lateinit var db: NowShowingDatabase
    @Before fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(NowShowingDatabase.DATABASE_NAME)
        db = NowShowingDatabase(context)
    }
    @After fun tearDown() { db.close() }
    private fun opened(episode: Int = 1) = RecentItem("tt123", "A title", "", "2026", "series", 1, episode, "source")
    @Test fun reopeningPreservesManualTitleLabel() {
        db.upsertRecentlyOpened(opened())
        db.setRecentlyWatched("tt123", true)
        db.upsertRecentlyOpened(opened(2))
        assertTrue(db.getRecentlyOpened().single().watched)
        assertEquals(2, db.getRecentlyOpened().single().episode)
        db.setRecentlyWatched("tt123", false)
        db.upsertRecentlyOpened(opened())
        assertFalse(db.getRecentlyOpened().single().watched)
    }
    @Test fun cacheClearPreservesHistoryAndLabels() {
        db.upsertRecentlyOpened(opened()); db.setRecentlyWatched("tt123", true)
        db.addRecentSearch("Dune"); db.setCachedPayload("test", "{}"); db.clearMetadata()
        assertNull(db.getCachedPayload("test"))
        assertTrue(db.getRecentlyOpened().single().watched)
        assertEquals(listOf("Dune"), db.getRecentSearches())
    }
    @Test fun cacheAndSearchRetentionAreBounded() {
        repeat(550) { db.setCachedPayload("item_$it", "{}") }
        val count = db.readableDatabase.rawQuery("SELECT count(*) FROM metadata_cache", null).use { it.moveToFirst(); it.getInt(0) }
        assertEquals(500, count)
        repeat(20) { db.addRecentSearch("query_$it") }
        val searches = db.readableDatabase.rawQuery("SELECT count(*) FROM search_history", null).use { it.moveToFirst(); it.getInt(0) }
        assertEquals(8, searches)
        db.setCachedPayload("too_big", "x".repeat(65537)); assertNull(db.getCachedPayload("too_big"))
    }
    @Test fun staleFallbackHasHardRetentionLimit() {
        db.setCachedPayload("stale", "warm", -1000)
        assertNull(db.getCachedPayload("stale")); assertEquals("warm", db.getCachedPayload("stale", true))
        db.setCachedPayload("ancient", "old", -31L * 24 * 60 * 60 * 1000)
        assertNull(db.getCachedPayload("ancient", true))
    }
}
