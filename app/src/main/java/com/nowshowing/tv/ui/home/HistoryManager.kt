package com.nowshowing.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nowshowing.tv.data.db.ContinueItem
import com.nowshowing.tv.data.db.RecentItem
import com.nowshowing.tv.ui.common.TvFocusableCard
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import com.nowshowing.tv.ui.theme.*

@Composable
internal fun RecentlyOpenedRow(items: List<RecentItem>, onOpen: (RecentItem) -> Unit) {
    val rowState = rememberLazyListState()
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("Recently opened", color = TextPrimary, fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 48.dp))
        Text("Return to your last episode and source", color = TextMuted, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 6.dp))
        LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items, key = { it.imdbID }) { item ->
                TvFocusableCard(title = item.title, posterUrl = item.poster, year = item.year,
                    badge = item.badge, focusKey = "recent:${item.imdbID}",
                    onClick = { onOpen(item) })
            }
        }
    }
}

@Composable
internal fun HistoryManager(
    recent: List<RecentItem>,
    native: List<ContinueItem>,
    onClose: () -> Unit,
    onRemoveRecent: (String) -> Unit,
    onMarkWatched: (RecentItem) -> Unit,
    onRemoveNative: (String) -> Unit
) {
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { closeFocus.requestFocus() }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.width(620.dp).heightIn(max = 520.dp)
            .background(SurfaceDark, RoundedCornerShape(20.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your viewing history", color = TextPrimary, fontSize = 22.sp)
            Text("Opened titles are saved on this TV. “Watched” is a manual label; web-player timestamps stay with the provider.",
                color = TextMuted, fontSize = 13.sp)
            TvPlayerActionButton("Done", null, modifier = Modifier.focusRequester(closeFocus), onClick = onClose)
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (recent.isEmpty() && native.isEmpty()) {
                    item { Text("Nothing here yet. Open a movie or episode to get started.", color = TextMuted) }
                }
                items(recent, key = { "recent:${it.imdbID}" }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, color = TextPrimary, fontSize = 16.sp)
                        Text(item.badge, color = TextMuted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvPlayerActionButton(if (item.watched) "Mark unwatched" else "Mark watched", null) { onMarkWatched(item) }
                            TvPlayerActionButton("Remove", null) { onRemoveRecent(item.imdbID) }
                        }
                    }
                }
                items(native, key = { "native:${it.imdbID}" }) { item ->
                    Column {
                        Text(item.title + " · Direct stream", color = TextPrimary)
                        TvPlayerActionButton("Remove from Continue Watching", null) { onRemoveNative(item.imdbID) }
                    }
                }
            }
        }
    }
}
