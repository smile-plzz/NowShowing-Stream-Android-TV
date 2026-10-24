package com.nowshowing.tv.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nowshowing.tv.data.db.RecentItem
import com.nowshowing.tv.ui.common.ShimmerBox
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val recoveryFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    var focusedRecovery by rememberSaveable { mutableStateOf(false) }
    val emptyFailure = !uiState.isLoading && uiState.message != null && uiState.heroTitle == null &&
        uiState.trending.isEmpty() && uiState.popularMovies.isEmpty() && uiState.popularTv.isEmpty() &&
        uiState.genreRows.all { it.items.isEmpty() }
    LaunchedEffect(emptyFailure) {
        if (emptyFailure && !focusedRecovery) {
            listState.scrollToItem(1 + (if (uiState.recentlyOpened.isNotEmpty()) 1 else 0) + (if (uiState.continueWatching.isNotEmpty()) 1 else 0))
            withFrameNanos { }
            focusedRecovery = runCatching { recoveryFocus.requestFocus(); true }.getOrDefault(false)
        }
    }
    val historyFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    var restoreHistoryFocus by remember { mutableStateOf(false) }
    LaunchedEffect(restoreHistoryFocus) { if (restoreHistoryFocus) { withFrameNanos { }; historyFocus.requestFocus(); restoreHistoryFocus = false } }
    var manageHistory by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshHistory()
        }
        lifecycle.addObserver(observer)
        viewModel.refreshHistory()
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (manageHistory) {
        HistoryManager(
            recent = uiState.recentlyOpened, native = uiState.continueWatching,
            onClose = { manageHistory = false; restoreHistoryFocus = true },
            onRemoveRecent = viewModel::removeRecent, onMarkWatched = viewModel::markWatched,
            onRemoveNative = viewModel::removeContinue
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Sticky TV Top Bar
        TvTopBar(currentRoute = "home", onNavigate = onNavigate)

        run {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                item(key = "history_heading") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Find your next watch", color = TextMuted, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvPlayerActionButton(if (uiState.isLoading) "Loading…" else "Refresh", null, enabled = !uiState.isLoading) { viewModel.retryMetadata() }
                            TvPlayerActionButton("Manage history", null, modifier = Modifier.focusRequester(historyFocus)) { manageHistory = true }
                        }
                    }
                }
                if (uiState.recentlyOpened.isNotEmpty()) {
                    item(key = "recently_opened") {
                        RecentlyOpenedRow(uiState.recentlyOpened) {
                            onPlayClick(it.imdbID, it.season, it.episode, it.type)
                        }
                    }
                }
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

                if (uiState.heroTitle == null && uiState.isLoading) {
                    item(key = "hero_loading") {
                        ShimmerBox(Modifier.fillMaxWidth().height(340.dp).padding(horizontal = 48.dp, vertical = 12.dp))
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

                if (uiState.isLoading || uiState.message != null) item(key = "load_feedback") {
                    Column(Modifier.padding(horizontal = 48.dp, vertical = 8.dp)) {
                        if (uiState.isLoading) Text("Loading more shelves…", color = TextMuted, fontSize = 13.sp)
                        uiState.message?.let { Text(it, color = TextMuted, fontSize = 14.sp) }
                        if (!uiState.isLoading) {
                            TvPlayerActionButton("Retry metadata", null, modifier = Modifier.focusRequester(recoveryFocus)) { viewModel.retryMetadata() }
                            if (uiState.message != null) TvPlayerActionButton("Metadata settings", null) { onNavigate("settings") }
                        }
                    }
                }
                // Mood Teaser Banner
                item(key = "mood_teaser") {
                    MoodTeaserBanner(onClick = { onNavigate("mood") })
                }

                // Trending Now
                item(key = "trending") {
                    TvContentRow(
                        title = "Featured picks",
                        isLoading = uiState.isLoading,
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
                        isLoading = uiState.isLoading,
                        items = row.items,
                        onItemClick = { m ->
                            onTitleClick(m.imdbID, if (m.type == "series") "series" else "movie")
                        }
                    )
                }

                // Popular Movies
                item(key = "pop_movies") {
                    TvContentRow(
                        title = "Curated Movies",
                        isLoading = uiState.isLoading,
                        items = uiState.popularMovies,
                        onItemClick = { m -> onTitleClick(m.imdbID, "movie") }
                    )
                }

                // Popular TV Shows
                item(key = "pop_tv") {
                    TvContentRow(
                        title = "Curated TV Shows",
                        isLoading = uiState.isLoading,
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
        // Backdrop poster image with Coil size downsampling and crossfade
        if (!hero.poster.isNullOrBlank() && hero.poster != "N/A") {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(hero.poster)
                    .size(1280, 720)
                    .crossfade(300)
                    .build(),
                contentDescription = hero.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Horizontal gradient shadow overlay
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

        // Vertical bottom gradient scrim for 100% text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            BgDark.copy(alpha = 0.7f),
                            BgDark
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
    val (actionFocus, rememberAction) = rememberReturnFocusRequester("home-action:$label")
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "hero_btn_scale"
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
                elevation = if (isFocused) 14.dp else 2.dp,
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { rememberAction(); onClick() }
            )
            .padding(horizontal = 22.dp, vertical = 11.dp),
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
            text = "Continue Watching · Direct streams",
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
                    focusKey = "continue:${item.imdbID}",
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
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.025f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "mood_teaser_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 10.dp)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 10.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = if (isFocused) FocusGlow else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) SurfaceVariant else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusBorder else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 26.dp, vertical = 18.dp)
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

@Composable
private fun ShimmerHomeScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "home_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_shimmer_offset"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SurfaceDark,
            SurfaceVariant.copy(alpha = 0.8f),
            SurfaceDark
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerOffset + 600f, 0f)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(top = 8.dp)
    ) {
        // Hero shimmer box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = 48.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Multiple row shimmers
        repeat(2) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .width(160.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(6) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(225.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(shimmerBrush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
