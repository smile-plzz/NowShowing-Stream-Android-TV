package com.nowshowing.tv.ui.details

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import com.nowshowing.tv.ui.common.rememberReturnFocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.ui.common.TvContentRow
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
import com.nowshowing.tv.ui.theme.TextSecondary

@Composable
fun DetailsScreen(
    imdbId: String,
    type: String,
    viewModel: DetailsViewModel,
    onNavigate: (String) -> Unit,
    onPlay: (String, Int, Int, String) -> Unit,
    onSelectRecommended: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val detailsScroll = rememberLazyListState()
    var showSourceSelector by remember { mutableStateOf(false) }

    LaunchedEffect(imdbId, type) {
        viewModel.loadDetails(imdbId, type)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        TvTopBar(currentRoute = "", onNavigate = onNavigate)

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
        } else {
            val detail = uiState.detail
            if (detail == null) {
                val (recoveryFocus, _) = rememberReturnFocusRequester("details-action:play")
                val focusState = com.nowshowing.tv.ui.common.LocalReturnFocus.current
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(com.nowshowing.tv.data.repository.MetadataHealth.failure.value?.message ?: "Could not load title details.", color = TextMuted, fontSize = 16.sp)
                        com.nowshowing.tv.ui.player.TvPlayerActionButton("Retry details", null, modifier = Modifier.focusRequester(recoveryFocus)) {
                            focusState?.pending = "details-action:play"
                            com.nowshowing.tv.data.repository.MetadataHealth.retry()
                            viewModel.loadDetails(imdbId, type)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = detailsScroll,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    if (com.nowshowing.tv.data.repository.MetadataHealth.usedStaleCache) {
                        item(key = "stale_notice") { Text("Some metadata this session uses expired cached results. Check Settings → Diagnostics for service status.",
                            color = TextMuted, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)) }
                    }
                    // Hero Backdrop & Main Metadata Row
                    item(key = "header_info") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            // Backdrop Image with Coil size downsampling and crossfade
                            if (!detail.poster.isNullOrBlank() && detail.poster != "N/A") {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(detail.poster)
                                        .size(1280, 720)
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = detail.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Dark Vertical Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Black.copy(alpha = 0.95f),
                                                BgDark
                                            )
                                        )
                                    )
                            )

                            // Left-Edge Horizontal Gradient Scrim for 100% typography & poster contrast
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF0D0E12),
                                                Color(0xFF0D0E12).copy(alpha = 0.65f),
                                                Color.Transparent
                                            ),
                                            startX = 0f,
                                            endX = 850f
                                        )
                                    )
                            )

                            // Poster + Details layout
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 48.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Poster Card
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(270.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                ) {
                                    if (!detail.poster.isNullOrBlank() && detail.poster != "N/A") {
                                        val context = LocalContext.current
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(detail.poster)
                                                .size(300, 450)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = detail.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(28.dp))

                                // Text details & action buttons
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    // Title
                                    Text(
                                        text = detail.title,
                                        color = TextPrimary,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Tags row: Year, Genre, Rating, Runtime, Rated
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TagBadge(text = detail.year)
                                        if (!detail.genre.isNullOrBlank() && detail.genre != "N/A") {
                                            detail.genre.split(",").take(3).forEach { g ->
                                                TagBadge(text = g.trim())
                                            }
                                        }
                                        if (!detail.imdbRating.isNullOrBlank() && detail.imdbRating != "N/A") {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SurfaceVariant)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = AccentGold,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = detail.imdbRating,
                                                    color = AccentGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        if (!detail.runtime.isNullOrBlank() && detail.runtime != "N/A") {
                                            TagBadge(text = detail.runtime)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Plot
                                    Text(
                                        text = detail.plot ?: "",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!detail.director.isNullOrBlank() && detail.director != "N/A") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Director: ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = detail.director, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    if (!detail.actors.isNullOrBlank() && detail.actors != "N/A") {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Starring: ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = detail.actors, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Primary Action Buttons Row
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        // Play / Resume Button
                                        DetailActionButton(
                                            label = if (uiState.type == "series") "Play S${uiState.selectedSeason}:E${uiState.selectedEpisode}" else "Play",
                                            icon = Icons.Default.PlayArrow,
                                            isPrimary = true,
                                            onClick = {
                                                onPlay(
                                                    detail.imdbID,
                                                    uiState.selectedSeason,
                                                    uiState.selectedEpisode,
                                                    uiState.type
                                                )
                                            }
                                        )

                                        // Watchlist Toggle
                                        DetailActionButton(
                                            label = if (uiState.isInWatchlist) "In Watchlist" else "Watchlist",
                                            icon = if (uiState.isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            isPrimary = false,
                                            onClick = { viewModel.toggleWatchlist() }
                                        )

                                        // Source Selector Trigger
                                        DetailActionButton(
                                            label = "Source: ${uiState.selectedSource.name}",
                                            icon = Icons.Default.Source,
                                            isPrimary = false,
                                            onClick = { showSourceSelector = !showSourceSelector }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Source Switcher Row (if toggled)
                    if (showSourceSelector) {
                        item(key = "source_selector") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Select Streaming Provider",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(SourceRegistry.UNSUPPORTED_WEB_SOURCES) { src ->
                                        SourceChip(
                                            source = src,
                                            isSelected = uiState.selectedSource.name == src.name,
                                            onClick = {
                                                viewModel.setSource(src)
                                                showSourceSelector = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TV Series Seasons & Episodes Selector
                    if (uiState.type == "series" && uiState.totalSeasons > 0) {
                        item(key = "seasons_row") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Seasons",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(uiState.totalSeasons) { index ->
                                        val seasonNum = index + 1
                                        SeasonPill(
                                            season = seasonNum,
                                            isSelected = uiState.selectedSeason == seasonNum,
                                            onClick = { viewModel.selectSeason(seasonNum) }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.episodesLoading || uiState.episodeError != null) {
                            item(key = "episodes_status") {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 8.dp)) {
                                    Text(if (uiState.episodesLoading) "Loading season ${uiState.selectedSeason} episodes..."
                                        else uiState.episodeError.orEmpty(), color = TextMuted, fontSize = 14.sp)
                                    if (!uiState.episodesLoading) {
                                        com.nowshowing.tv.ui.player.TvPlayerActionButton("Retry episodes", null) {
                                            viewModel.retryEpisodes()
                                        }
                                    }
                                }
                            }
                        }

                        // Episodes Row
                        if (uiState.episodes.isNotEmpty()) {
                            item(key = "episodes_row") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Season ${uiState.selectedSeason} Episodes",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(uiState.episodes) { ep ->
                                            val epNum = ep.episode.toIntOrNull() ?: 1
                                            EpisodeCard(
                                                episode = ep,
                                                isSelected = uiState.selectedEpisode == epNum,
                                                onClick = {
                                                    viewModel.selectEpisode(epNum)
                                                    onPlay(
                                                        detail.imdbID,
                                                        uiState.selectedSeason,
                                                        epNum,
                                                        "series"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Metadata details: Director, Cast, Country
                    item(key = "cast_details") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 12.dp)
                        ) {
                            if (!detail.director.isNullOrBlank() && detail.director != "N/A") {
                                Text(
                                    text = "Director: ${detail.director}",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            if (!detail.actors.isNullOrBlank() && detail.actors != "N/A") {
                                Text(
                                    text = "Cast: ${detail.actors}",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            if (!detail.country.isNullOrBlank() && detail.country != "N/A") {
                                Text(
                                    text = "Country: ${detail.country}",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Recommended Titles
                    if (uiState.recommendations.isNotEmpty()) {
                        item(key = "recommendations") {
                            TvContentRow(
                                title = "More Like This",
                                items = uiState.recommendations,
                                onItemClick = { m ->
                                    onSelectRecommended(
                                        m.imdbID,
                                        if (m.type == "series") "series" else "movie"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val (actionFocus, rememberAction) = rememberReturnFocusRequester(if (isPrimary) "details-action:play" else "details-action:$label")
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "detail_action_btn_scale"
    )

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
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 12.dp else 2.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = if (isFocused) FocusGlow else Color.Transparent
            )
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .focusRequester(actionFocus)
            .clickable(interactionSource = interactionSource, indication = null, onClick = { rememberAction(); onClick() })
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SourceChip(
    source: com.nowshowing.tv.domain.source.VideoSource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> FocusBorder
        isSelected -> AccentCyan.copy(alpha = 0.2f)
        else -> SurfaceDark
    }

    val textColor = if (isFocused) Color.Black else TextPrimary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) Color.White else if (isSelected) AccentCyan else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = source.name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        val tagText = if (source.isNativeCompatible) "NATIVE" else "EMBED"
        val tagColor = if (source.isNativeCompatible) AccentCyan else Color(0xFFFF6B6B)
        Text(
            text = "[$tagText]",
            color = if (isFocused) Color.Black.copy(alpha = 0.8f) else tagColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SeasonPill(
    season: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "season_pill_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 8.dp else 0.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = if (isFocused) FocusGlow else Color.Transparent
            )
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) FocusBorder else if (isSelected) SurfaceVariant else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = if (isFocused) Color.White else if (isSelected) AccentCyan else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            text = "Season $season",
            color = if (isFocused) Color.Black else if (isSelected) AccentCyan else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EpisodeCard(
    episode: com.nowshowing.tv.data.model.OmdbEpisode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (episodeFocus, rememberEpisode) = rememberReturnFocusRequester("episode:${episode.imdbID}")
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "episode_card_scale"
    )

    Column(
        modifier = Modifier
            .width(190.dp)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 10.dp else 0.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = if (isFocused) FocusGlow else Color.Transparent
            )
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) FocusBorder else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) Color.White else if (isSelected) AccentCyan else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .focusRequester(episodeFocus)
            .clickable(interactionSource = interactionSource, indication = null, onClick = { rememberEpisode(); onClick() })
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "EPISODE ${episode.episode}",
                color = if (isFocused) Color.Black else AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (!episode.imdbRating.isNullOrBlank() && episode.imdbRating != "N/A") {
                Text(
                    text = "★ ${episode.imdbRating}",
                    color = if (isFocused) Color.Black else AccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = episode.title,
            color = if (isFocused) Color.Black else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

