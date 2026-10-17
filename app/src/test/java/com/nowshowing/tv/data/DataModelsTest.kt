package com.nowshowing.tv.data

import com.nowshowing.tv.data.db.ContinueItem
import com.nowshowing.tv.data.db.WatchlistItem
import com.nowshowing.tv.data.repository.MediaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelsTest {

    @Test
    fun testContinueItemProgressFraction() {
        val itemHalf = ContinueItem(
            imdbID = "tt1375666",
            title = "Inception",
            poster = "https://poster.jpg",
            year = "2010",
            type = "movie",
            positionMs = 60_000L,
            durationMs = 120_000L
        )
        assertEquals(0.5f, itemHalf.progressFraction, 0.001f)

        val itemZero = itemHalf.copy(positionMs = 0L)
        assertEquals(0.0f, itemZero.progressFraction, 0.001f)

        val itemComplete = itemHalf.copy(positionMs = 120_000L)
        assertEquals(1.0f, itemComplete.progressFraction, 0.001f)

        val itemInvalidDuration = itemHalf.copy(durationMs = 0L)
        assertEquals(0.0f, itemInvalidDuration.progressFraction, 0.001f)
    }

    @Test
    fun testWatchlistItemCreation() {
        val item = WatchlistItem(
            imdbID = "tt0903747",
            title = "Breaking Bad",
            poster = "https://poster.jpg",
            year = "2008-2013",
            type = "series"
        )
        assertEquals("tt0903747", item.imdbID)
        assertEquals("Breaking Bad", item.title)
        assertEquals("series", item.type)
        assertTrue(item.dateAdded > 0L)
    }

    @Test
    fun testGenreRowSeedsIntegrity() {
        val rows = MediaRepository.GENRE_ROWS
        assertTrue("Must contain all major genre seeds", rows.size >= 5)
        for (row in rows) {
            assertTrue("Genre row id must not be empty", row.id.isNotBlank())
            assertTrue("Genre row label must not be empty", row.label.isNotBlank())
            assertTrue("Genre seed titles must not be empty", row.seed.isNotEmpty())
        }
    }
}
