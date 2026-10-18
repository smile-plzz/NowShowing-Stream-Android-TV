package com.nowshowing.tv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.ui.theme.*

private data class TvDestination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    TvDestination("home", "Home", Icons.Default.Home),
    TvDestination("movies", "Movies", Icons.Default.Movie),
    TvDestination("tv", "Series", Icons.Default.Tv),
    TvDestination("mood", "Moods", Icons.Default.Mood),
    TvDestination("search", "Search", Icons.Default.Search),
    TvDestination("watchlist", "Watchlist", Icons.Default.Bookmark),
    TvDestination("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun TvTopBar(currentRoute: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(80.dp).background(BgDark).padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.padding(end = 24.dp)) {
            Text("NOWSHOWING", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("YOUR EVENING, SORTED", color = AccentCyan, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        LazyRow(Modifier.weight(1f).testTag("primary-navigation"), horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
            items(destinations, key = { it.route }) { destination ->
                TvSectionTab(destination.label, currentRoute == destination.route, icon = destination.icon) {
                    if (currentRoute != destination.route) onNavigate(destination.route)
                }
            }
        }
    }
}
