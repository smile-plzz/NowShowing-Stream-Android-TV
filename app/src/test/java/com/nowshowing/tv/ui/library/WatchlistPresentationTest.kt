package com.nowshowing.tv.ui.library

import com.nowshowing.tv.data.db.WatchlistItem
import org.junit.Assert.*
import org.junit.Test

class WatchlistPresentationTest {
    private val titles = listOf(
        WatchlistItem("old", "Zulu", "", "1999", "movie", 30),
        WatchlistItem("series", "alpha", "", "2024–", "series", 20),
        WatchlistItem("unknown", "Beta", "", "N/A", "movie", 10),
        WatchlistItem("new", "Gamma", "", "2025", "movie", 40)
    )
    @Test fun yearUsesStartYearAndPlacesUnknownLast() {
        assertEquals(listOf("new", "series", "old", "unknown"),
            presentWatchlist(titles, WatchlistType.ALL, WatchlistSort.YEAR).map { it.imdbID })
    }
    @Test fun filterThenSortLeavesSourceRecordsIntact() {
        assertEquals(listOf("unknown", "new", "old"),
            presentWatchlist(titles, WatchlistType.MOVIES, WatchlistSort.TITLE).map { it.imdbID })
        assertEquals(listOf("old", "series", "unknown", "new"), titles.map { it.imdbID })
    }
    @Test fun recentOrderIsIndependentOfInputOrder() {
        assertEquals(listOf("new", "old", "series", "unknown"),
            presentWatchlist(titles.reversed(), WatchlistType.ALL, WatchlistSort.RECENT).map { it.imdbID })
        assertTrue(presentWatchlist(emptyList(), WatchlistType.SERIES, WatchlistSort.YEAR).isEmpty())
    }
}
