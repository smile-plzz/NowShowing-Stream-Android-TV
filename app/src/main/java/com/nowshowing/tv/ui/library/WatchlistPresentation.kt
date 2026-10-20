package com.nowshowing.tv.ui.library

import com.nowshowing.tv.data.db.WatchlistItem
import java.util.Locale

enum class WatchlistType(val label: String, val value: String?) {
    ALL("All titles", null), MOVIES("Movies", "movie"), SERIES("TV series", "series")
}
enum class WatchlistSort(val label: String) {
    RECENT("Recently saved"), TITLE("Title A–Z"), YEAR("Newest release")
}

/** Local library operations never request metadata or alter saved records. */
fun presentWatchlist(items: List<WatchlistItem>, type: WatchlistType, sort: WatchlistSort): List<WatchlistItem> {
    val filtered = items.filter { type.value == null || it.type == type.value }
    val titleOrder = compareBy<WatchlistItem> { it.title.lowercase(Locale.ROOT) }.thenBy { it.imdbID }
    return when (sort) {
        WatchlistSort.RECENT -> filtered.sortedWith(compareByDescending<WatchlistItem> { it.dateAdded }.then(titleOrder))
        WatchlistSort.TITLE -> filtered.sortedWith(titleOrder)
        WatchlistSort.YEAR -> filtered.sortedWith(compareByDescending<WatchlistItem> {
            Regex("^\\d{4}").find(it.year)?.value?.toIntOrNull() ?: Int.MIN_VALUE
        }.then(titleOrder))
    }
}
