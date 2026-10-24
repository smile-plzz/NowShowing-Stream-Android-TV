package com.nowshowing.tv.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.fillMaxWidth
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import com.nowshowing.tv.ui.theme.CardBorder
import com.nowshowing.tv.ui.theme.SurfaceDark
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.ui.common.TvFocusableCard
import com.nowshowing.tv.ui.common.TvTopBar
import com.nowshowing.tv.ui.theme.AccentCyan
import com.nowshowing.tv.ui.theme.BgDark
import com.nowshowing.tv.ui.theme.TextMuted
import com.nowshowing.tv.ui.theme.TextPrimary

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onNavigate: (String) -> Unit,
    onTitleClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    var selectedType by rememberSaveable { mutableStateOf(WatchlistType.ALL) }
    var selectedSort by rememberSaveable { mutableStateOf(WatchlistSort.RECENT) }
    val visibleItems = remember(uiState.items, selectedType, selectedSort) {
        presentWatchlist(uiState.items, selectedType, selectedSort)
    }

    LaunchedEffect(Unit) {
        viewModel.loadWatchlist()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        TvTopBar(currentRoute = "watchlist", onNavigate = onNavigate)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Your Watchlist",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    TvPlayerActionButton("Show: ${selectedType.label}", null) {
                        selectedType = WatchlistType.entries[(selectedType.ordinal + 1) % WatchlistType.entries.size]
                    }
                }
                item {
                    TvPlayerActionButton("Sort: ${selectedSort.label}", null) {
                        selectedSort = WatchlistSort.entries[(selectedSort.ordinal + 1) % WatchlistSort.entries.size]
                    }
                }
                item { TvPlayerActionButton("Find titles", null) { onNavigate("search") } }
            }
            Text("${visibleItems.size} of ${uiState.items.size} saved titles", color = TextMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else if (visibleItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.items.isEmpty()) "Your Watchlist is Empty" else "No saved ${selectedType.label.lowercase()}",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.items.isEmpty()) "Discover movies and series to save for your evening viewing." else "Try changing the filter to show movies or series.",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TvPlayerActionButton("Find titles", null) { onNavigate("search") }
                            TvPlayerActionButton("Browse movies", null) { onNavigate("movies") }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleItems, key = { it.imdbID }) { movie ->
                        TvFocusableCard(
                            focusKey = "watchlist:${movie.imdbID}",
                            title = movie.title,
                            posterUrl = movie.poster,
                            year = movie.year,
                            badge = if (movie.type == "series") "Series" else null,
                            onClick = { onTitleClick(movie.imdbID, movie.type) }
                        )
                    }
                }
            }
        }
    }
}

