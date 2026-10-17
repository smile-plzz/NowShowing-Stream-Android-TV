package com.nowshowing.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nowshowing.tv.data.db.ContinueItem
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.ui.common.TvContentRow
import com.nowshowing.tv.ui.common.TvFocusableCard
import com.nowshowing.tv.ui.common.TvTopBar
import com.nowshowing.tv.ui.theme.AccentCyan
import com.nowshowing.tv.ui.theme.AccentGold
import com.nowshowing.tv.ui.theme.BgDark
import com.nowshowing.tv.ui.theme.FocusBorder
import com.nowshowing.tv.ui.theme.FocusGlow
import com.nowshowing.tv.ui.theme.SurfaceDark
import com.nowshowing.tv.ui.theme.SurfaceVariant
import com.nowshowing.tv.ui.theme.TextMuted
import com.nowshowing.tv.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    onTitleClick: (String, String) -> Unit,
    onPlayClick: (String, Int, Int, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Sticky TV Top Bar
        TvTopBar(currentRoute = "home", onNavigate = onNavigate)

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                // Hero Banner
                uiState.heroTitle?.let { hero ->
                    item(key = "hero_banner") {
                        HomeHeroBanner(
                            hero = hero,
                            onPlay = {
                                onPlayClick(
                                    hero.imdbID,
                                    1,
                                    1,
                                    if (hero.type == "series") "series" else "movie"
                                )
                            },
                            onMoreInfo = {
                                onTitleClick(
                                    hero.imdbID,
                                    if (hero.type == "series") "series" else "movie"
                                )
                            }
                        )
                    }
                }

                // Continue Watching
                if (uiState.continueWatching.isNotEmpty()) {
                    item(key = "continue_watching") {
                        ContinueWatchingRow(
                            items = uiState.continueWatching,
                            onItemClick = { item ->
                                onPlayClick(item.imdbID, item.season, item.episode, item.type)
                            }
                        )
                    }
                }

                // Mood Teaser Banner
                item(key = "mood_teaser") {
                    MoodTeaserBanner(onClick = { onNavigate("mood") })
                }

                // Trending Now
                item(key = "trending") {
                    TvContentRow(
                        title = "Trending Now",
                        items = uiState.trending,
                        onItemClick = { m ->
                            onTitleClick(m.imdbID, if (m.type == "series") "series" else "movie")
                        }
                    )
                }

                // Genre Rows
                items(uiState.genreRows, key = { it.seed.id }) { row ->
                    TvContentRow(
                        title = row.seed.label,
                        items = row.items,
                        onItemClick = { m ->
                            onTitleClick(m.imdbID, if (m.type == "series") "series" else "movie")
                        }
                    )
                }

                // Popular Movies
                item(key = "pop_movies") {
                    TvContentRow(
                        title = "Popular Movies",
                        items = uiState.popularMovies,
                        onItemClick = { m -> onTitleClick(m.imdbID, "movie") }
                    )
                }

                // Popular TV Shows
                item(key = "pop_tv") {
                    TvContentRow(
                        title = "Popular TV Shows",
                        items = uiState.popularTv,
                        onItemClick = { m -> onTitleClick(m.imdbID, "series") }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeroBanner(
    hero: OmdbTitleDetail,
    onPlay: () -> Unit,
    onMoreInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 48.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
    ) {
        // Backdrop poster image
        if (!hero.poster.isNullOrBlank() && hero.poster != "N/A") {
            AsyncImage(
                model = hero.poster,
                contentDescription = hero.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )
        }

        // Gradient shadow overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.2f)
                        )
                    )
                )
        )

        // Text & Action Buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Genre and Year tags
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentCyan)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = hero.year,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!hero.imdbRating.isNullOrBlank() && hero.imdbRating != "N/A") {
                    Text(text = "•", color = TextMuted)
                    Text(
                        text = "★ ${hero.imdbRating}",
                        color = AccentGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!hero.runtime.isNullOrBlank() && hero.runtime != "N/A") {
                    Text(text = "•", color = TextMuted)
                    Text(text = hero.runtime, color = TextMuted, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = hero.title,
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Plot
            Text(
                text = hero.plot ?: "",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvActionButton(
                    label = "Play",
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    onClick = onPlay
                )
                TvActionButton(
                    label = "More Info",
                    icon = Icons.Default.Info,
                    isPrimary = false,
                    onClick = onMoreInfo
                )
            }
        }
    }
}

@Composable
fun TvActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> FocusBorder
        isPrimary -> AccentCyan
        else -> SurfaceVariant
    }

    val contentColor = when {
        isFocused || isPrimary -> Color.Black
        else -> TextPrimary
    }

    Row(
        modifier = Modifier
            .shadow(
                elevation = if (isFocused) 12.dp else 2.dp,
                shape = RoundedCornerShape(8.dp),
                spotColor = if (isFocused) FocusGlow else Color.Transparent
            )
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ContinueWatchingRow(
    items: List<ContinueItem>,
    onItemClick: (ContinueItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Continue Watching",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = { it.imdbID }) { item ->
                val epBadge = if (item.type == "series") "S${item.season}:E${item.episode}" else "Resume"
                TvFocusableCard(
                    title = item.title,
                    posterUrl = item.poster,
                    year = item.year,
                    badge = epBadge,
                    progressFraction = item.progressFraction,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun MoodTeaserBanner(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) SurfaceVariant else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🎭", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Not sure what to watch?",
                    color = if (isFocused) AccentCyan else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Tell us your mood and we'll find precise genre-verified matches →",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}
