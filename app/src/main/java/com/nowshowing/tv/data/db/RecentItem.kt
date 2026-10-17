package com.nowshowing.tv.data.db

/** An opened provider page, never a claim that video played or a timestamp was measured. */
data class RecentItem(
    val imdbID: String,
    val title: String,
    val poster: String,
    val year: String,
    val type: String,
    val season: Int = 1,
    val episode: Int = 1,
    val sourceId: String,
    val watched: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val badge: String
        get() = when {
            watched -> "Marked watched"
            type == "series" -> "S$season · E$episode"
            else -> "Opened"
        }
}
