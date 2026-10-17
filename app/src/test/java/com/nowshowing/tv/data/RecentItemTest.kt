package com.nowshowing.tv.data

import com.nowshowing.tv.data.db.RecentItem
import org.junit.Assert.*
import org.junit.Test

class RecentItemTest {
    private val episode = RecentItem("tt123", "Series", "", "2024", "series", 2, 5, "provider")
    @Test fun openedEpisodeDoesNotClaimTimestampResume() {
        assertEquals("S2 · E5", episode.badge)
        assertFalse(episode.watched)
    }
    @Test fun manualWatchedStateIsExplicitAndReversible() {
        assertEquals("Marked watched", episode.copy(watched = true).badge)
        assertEquals("S2 · E5", episode.copy(watched = false).badge)
    }
    @Test fun movieIsLabelledOpenedRatherThanPlayed() {
        assertEquals("Opened", episode.copy(type = "movie").badge)
    }
}
