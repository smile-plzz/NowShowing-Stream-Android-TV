package com.nowshowing.tv.ui.player

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.domain.source.VideoSource
import com.nowshowing.tv.ui.theme.AccentCyan
import com.nowshowing.tv.ui.theme.AccentGold
import com.nowshowing.tv.ui.theme.FocusBorder
import com.nowshowing.tv.ui.theme.FocusGlow
import com.nowshowing.tv.ui.theme.SurfaceDark
import com.nowshowing.tv.ui.theme.SurfaceVariant
import com.nowshowing.tv.ui.theme.TextMuted
import com.nowshowing.tv.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    imdbId: String,
    season: Int,
    episode: Int,
    type: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showHud by remember { mutableStateOf(true) }
    var hudCountdown by remember { mutableStateOf(5) }
    var showAudioSubDrawer by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var selectedAudioLang by remember { mutableStateOf("Auto") }

    val primaryFocusRequester = remember { FocusRequester() }

    BackHandler {
        onBack()
    }

    LaunchedEffect(imdbId, season, episode, type) {
        viewModel.initialize(imdbId, season, episode, type)
    }

    // Web providers deliver HTML players rather than native stream URLs.
    if (!uiState.isLoading && !uiState.currentSource.isNativeCompatible) {
        WebEmbedScreen(uiState, viewModel, onBack)
        return
    }

    // Auto-hide HUD countdown timer
    LaunchedEffect(showHud, hudCountdown) {
        if (showHud) {
            delay(1000)
            if (hudCountdown > 1) {
                hudCountdown--
            } else {
                showHud = false
            }
        }
    }

    // Clear seek feedback after 1.5s
    LaunchedEffect(uiState.seekFeedbackText) {
        if (uiState.seekFeedbackText != null) {
            delay(1500)
            viewModel.setSeekFeedback(null)
        }
    }

    // Next Episode Countdown Timer
    var nextEpCountdown by remember(uiState.showNextEpisodePrompt) { mutableStateOf(30) }
    LaunchedEffect(uiState.showNextEpisodePrompt) {
        if (uiState.showNextEpisodePrompt) {
            for (i in 30 downTo 1) {
                nextEpCountdown = i
                delay(1000)
            }
            viewModel.playNextEpisode()
        }
    }

    val playable = uiState.playableSource

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showHud = true
                            hudCountdown = 5
                            false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        if (playable != null) {
            // AndroidX Media3 / ExoPlayer Native Engine
            val exoPlayer = remember(playable.mediaUri) {
                ExoPlayer.Builder(context).build().apply {
                    setAudioAttributes(
                        androidx.media3.common.AudioAttributes.Builder()
                            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        /* handleAudioFocus = */ true
                    )
                    val mediaItem = MediaItem.fromUri(playable.mediaUri)
                    setMediaItem(mediaItem)
                    if (playable.startPositionMs > 0L) {
                        seekTo(playable.startPositionMs)
                    }
                    prepare()
                    playWhenReady = true
                }
            }

            // ExoPlayer Lifecycle & State Synchronization
            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> viewModel.setBuffering(true)
                            Player.STATE_READY -> viewModel.setBuffering(false)
                            Player.STATE_ENDED -> {
                                if (uiState.type == "series") {
                                    viewModel.playNextEpisode()
                                } else {
                                    // Movie finished — show completion
                                    viewModel.onMovieFinished()
                                }
                            }
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        viewModel.setPlaying(isPlaying)
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        val isNetworkError = error.errorCode ==
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            error.errorCode ==
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                            error.errorCode ==
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                        if (isNetworkError) {
                            viewModel.onNetworkError()
                        } else {
                            viewModel.onPlaybackError(error.message ?: "Unknown playback error")
                        }
                    }
                }
                exoPlayer.addListener(listener)

                onDispose {
                    viewModel.updateProgress(exoPlayer.currentPosition, exoPlayer.duration)
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                }
            }

            // Periodic Playback Progress Tracking Coroutine
            LaunchedEffect(exoPlayer, uiState.isPlaying) {
                while (true) {
                    if (exoPlayer.isPlaying) {
                        viewModel.updateProgress(exoPlayer.currentPosition, exoPlayer.duration)
                    }
                    delay(1000)
                }
            }

            // Player View Surface (Zero WebViews)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Custom TV 10-foot HUD used instead
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        showHud = !showHud
                        hudCountdown = 5
                    }
            )

            // D-Pad Remote Control Receiver for Active Playback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(primaryFocusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER,
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                    showHud = true
                                    hudCountdown = 4
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                    exoPlayer.play()
                                    showHud = true
                                    hudCountdown = 4
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    exoPlayer.pause()
                                    showHud = true
                                    hudCountdown = 6
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                    val newPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    viewModel.setSeekFeedback("⏪ -10s")
                                    showHud = true
                                    hudCountdown = 4
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT,
                                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                                    val newPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(dur)
                                    exoPlayer.seekTo(newPos)
                                    viewModel.setSeekFeedback("⏩ +10s")
                                    showHud = true
                                    hudCountdown = 4
                                    true
                                }
                                KeyEvent.KEYCODE_BACK -> {
                                    viewModel.updateProgress(exoPlayer.currentPosition, exoPlayer.duration)
                                    onBack()
                                    true
                                }
                                KeyEvent.KEYCODE_MEDIA_STOP -> {
                                    viewModel.updateProgress(exoPlayer.currentPosition, exoPlayer.duration)
                                    exoPlayer.stop()
                                    onBack()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            )

            // Seek Feedback Pill Overlay
            AnimatedVisibility(
                visible = uiState.seekFeedbackText != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    AccentCyan.copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = uiState.seekFeedbackText ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Buffering Indicator
            if (uiState.isBuffering) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Cinematic 10-Foot HUD Overlay
            AnimatedVisibility(
                visible = showHud,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.88f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(32.dp)
                ) {
                    // Header Row: Back Button, Title, Season/Episode, Source Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvPlayerIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDesc = "Back to Details",
                                onClick = {
                                    viewModel.updateProgress(exoPlayer.currentPosition, exoPlayer.duration)
                                    onBack()
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = uiState.title,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val sub = if (uiState.type == "series") {
                                    "Season ${uiState.season} Episode ${uiState.episode}"
                                } else {
                                    uiState.year
                                }
                                Text(
                                    text = sub,
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Subtitles & Audio Drawer Button
                            TvPlayerActionButton(
                                label = "Audio & Subs",
                                icon = Icons.Default.Subtitles,
                                isAccent = false,
                                onClick = { showAudioSubDrawer = true }
                            )

                            // Source Indicator & Switcher Button
                            TvPlayerActionButton(
                                label = "Source: ${uiState.currentSource.name}",
                                icon = Icons.Default.SwapHoriz,
                                isAccent = true,
                                onClick = { viewModel.openSourceSelector() }
                            )
                        }
                    }

                    // Bottom Controls Row: Timeline, Play/Pause, Next/Prev Episode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        // Time Scrubber & Labels
                        val posSec = uiState.currentPositionMs / 1000
                        val durSec = uiState.durationMs / 1000
                        val progressFraction = if (uiState.durationMs > 0) {
                            (uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTimeSec(posSec),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTimeSec(durSec),
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom scrubber timeline with buffered and played segments
                        val bufferedFraction = exoPlayer.bufferedPercentage / 100f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        ) {
                            // Background track
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            // Buffered segment
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bufferedFraction.coerceIn(0f, 1f))
                                    .background(Color.White.copy(alpha = 0.45f))
                            )
                            // Played segment
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                    .background(AccentCyan)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Controls Action Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.type == "series" && uiState.episode > 1) {
                                TvPlayerIconButton(
                                    icon = Icons.Default.SkipPrevious,
                                    contentDesc = "Previous Episode",
                                    onClick = { viewModel.playPreviousEpisode() }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            TvPlayerIconButton(
                                icon = Icons.Default.FastRewind,
                                contentDesc = "Rewind 10 seconds",
                                onClick = {
                                    val newPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    viewModel.setSeekFeedback("⏪ -10s")
                                }
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            TvPlayerIconButton(
                                icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDesc = if (uiState.isPlaying) "Pause" else "Play",
                                isPrimary = true,
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            TvPlayerIconButton(
                                icon = Icons.Default.FastForward,
                                contentDesc = "Forward 10 seconds",
                                onClick = {
                                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                                    val newPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(dur)
                                    exoPlayer.seekTo(newPos)
                                    viewModel.setSeekFeedback("⏩ +10s")
                                }
                            )

                            if (uiState.type == "series") {
                                Spacer(modifier = Modifier.width(16.dp))
                                TvPlayerIconButton(
                                    icon = Icons.Default.SkipNext,
                                    contentDesc = "Next Episode",
                                    onClick = { viewModel.playNextEpisode() }
                                )
                            }
                        }
                    }
                }
            }

            // Next Episode Suggestion Card (Appears in bottom-right during last 30s)
            if (uiState.showNextEpisodePrompt && uiState.type == "series") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 36.dp, bottom = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark.copy(alpha = 0.95f))
                        .border(1.5.dp, AccentCyan, RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Next in ${nextEpCountdown}s",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Season ${uiState.season} Episode ${uiState.episode + 1}",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvPlayerActionButton(
                                label = "Play Now",
                                icon = Icons.Default.PlayArrow,
                                isAccent = true,
                                onClick = { viewModel.playNextEpisode() }
                            )
                            TvPlayerActionButton(
                                label = "Dismiss",
                                icon = null,
                                onClick = { viewModel.dismissNextEpisodePrompt() }
                            )
                        }
                    }
                }
            }

            // Series or Movie Completion Card
            if (uiState.showSeriesComplete) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 36.dp, bottom = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark.copy(alpha = 0.95f))
                        .border(1.5.dp, AccentGold, RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "🎬 Finished!",
                            color = AccentGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TvPlayerActionButton(
                            label = "Back to Details",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = {
                                viewModel.dismissCompletion()
                                onBack()
                            }
                        )
                    }
                }
            }

            // Subtitles & Audio Drawer Dialog
            if (showAudioSubDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .clickable { showAudioSubDrawer = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(500.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .border(1.5.dp, FocusBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp)
                            .clickable(enabled = false) {}
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Audio & Subtitles",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TvPlayerActionButton(
                                label = "Close",
                                icon = null,
                                onClick = { showAudioSubDrawer = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Subtitles (WebVTT / Embedded)",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvPlayerActionButton(
                                label = if (subtitlesEnabled) "Subtitles: ON" else "Subtitles: OFF",
                                icon = if (subtitlesEnabled) Icons.Default.Check else null,
                                isAccent = subtitlesEnabled,
                                onClick = {
                                    subtitlesEnabled = !subtitlesEnabled
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(
                                            androidx.media3.common.C.TRACK_TYPE_TEXT,
                                            !subtitlesEnabled
                                        )
                                        .build()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Audio Language Track",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Auto", "English", "Original").forEach { lang ->
                                TvPlayerActionButton(
                                    label = lang,
                                    icon = if (selectedAudioLang == lang) Icons.Default.Check else null,
                                    isAccent = selectedAudioLang == lang,
                                    onClick = {
                                        selectedAudioLang = lang
                                        val paramBuilder = exoPlayer.trackSelectionParameters.buildUpon()
                                        if (lang == "English") {
                                            paramBuilder.setPreferredAudioLanguage("en")
                                        } else {
                                            paramBuilder.setPreferredAudioLanguage(null)
                                        }
                                        exoPlayer.trackSelectionParameters = paramBuilder.build()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else if (uiState.isIncompatibleSource || uiState.errorMessage != null) {
            // Mandatory Incompatible / Error State Screen (Zero WebViews)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SurfaceVariant.copy(alpha = 0.5f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(540.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (uiState.isIncompatibleSource) Icons.Default.TvOff else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (uiState.isIncompatibleSource) AccentGold else Color.Red,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (uiState.isIncompatibleSource) "Native Playback Unavailable" else "Playback Error",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = uiState.errorMessage ?: "Native playback is unavailable for this source. Try another source.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TvPlayerActionButton(
                            label = "⚡ Switch to Compatible Native Source",
                            icon = Icons.Default.SwapHoriz,
                            isAccent = true,
                            onClick = { viewModel.switchToNextCompatibleSource() }
                        )

                        TvPlayerActionButton(
                            label = "Choose from Source List",
                            icon = null,
                            onClick = { viewModel.openSourceSelector() }
                        )

                        TvPlayerActionButton(
                            label = "Back to Details",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onBack
                        )
                    }
                }
            }
        } else {
            // Loading Spinner
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Resolving native media stream...", color = TextMuted, fontSize = 14.sp)
                }
            }
        }

        // TV Source Selection Modal Dialog
        if (uiState.showSourceSelector) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { viewModel.closeSourceSelector() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(580.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, FocusBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                        .clickable(enabled = false) {}
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Playback Source",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TvPlayerActionButton(
                            label = "Close",
                            icon = null,
                            onClick = { viewModel.closeSourceSelector() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(SourceRegistry.SOURCES) { source ->
                            SourceItemCard(
                                source = source,
                                isSelected = source.name == uiState.currentSource.name,
                                onClick = { viewModel.selectSource(source) }
                            )
                        }
                    }
                }
            }
        }

        // Network Error Retry Dialog
        if (uiState.isNetworkError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(480.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connection Lost",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Network stream connection failed. Check your network or try another source.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvPlayerActionButton(
                            label = "Retry",
                            icon = Icons.Default.Refresh,
                            isAccent = true,
                            onClick = { viewModel.retryPlayback() }
                        )
                        TvPlayerActionButton(
                            label = "Next Source",
                            icon = Icons.Default.SwapHoriz,
                            onClick = { viewModel.switchToNextCompatibleSource() }
                        )
                        TvPlayerActionButton(
                            label = "Back",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onBack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceItemCard(
    source: VideoSource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> FocusBorder
        isSelected -> AccentCyan.copy(alpha = 0.15f)
        else -> SurfaceVariant
    }

    val textColor = if (isFocused) Color.Black else TextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) Color.White else if (isSelected) AccentCyan else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.name,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (source.description.isNotBlank()) {
                Text(
                    text = source.description,
                    color = if (isFocused) Color.Black.copy(alpha = 0.7f) else TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Compatibility Tag
        val tagText = if (source.isNativeCompatible) "NATIVE PLAYABLE" else "WEB PLAYER"
        val tagColor = if (source.isNativeCompatible) AccentCyan else Color(0xFFFF6B6B)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isFocused) Color.Black.copy(alpha = 0.2f) else tagColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = tagText,
                color = if (isFocused) Color.Black else tagColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TvPlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val size = if (isPrimary) 52.dp else 40.dp
    val iconSize = if (isPrimary) 26.dp else 20.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isFocused) FocusBorder else if (isPrimary) AccentCyan else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (isFocused) Color.Black else if (isPrimary) Color.Black else TextPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun TvPlayerActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isAccent: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> FocusBorder
        isAccent -> AccentCyan.copy(alpha = 0.2f)
        else -> SurfaceDark
    }

    val textColor = when {
        isFocused -> Color.Black
        isAccent -> AccentCyan
        else -> TextPrimary
    }

    Row(
        modifier = modifier
            .shadow(
                elevation = if (isFocused) 12.dp else 2.dp,
                shape = RoundedCornerShape(8.dp),
                spotColor = if (isFocused) FocusGlow else Color.Transparent
            )
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else if (isAccent) AccentCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTimeSec(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val hours = s / 3600
    val mins = (s % 3600) / 60
    val secs = s % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}
