package com.nowshowing.tv.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Message
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.privacy.*
import com.nowshowing.tv.ui.theme.*
import java.io.ByteArrayInputStream

/** HTML providers stay in WebView; direct HLS/DASH/MP4 remain in Media3. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebEmbedScreen(state: PlayerUiState, viewModel: PlayerViewModel, onBack: () -> Unit) {
    val menuFocus = remember { FocusRequester() }
    val nextFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var ready by remember { mutableStateOf(false) }
    var menu by remember(state.webEmbedUrl) { mutableStateOf(false) }
    var section by remember(state.webEmbedUrl) { mutableStateOf("controls") }
    val helpPrefs = remember { context.getSharedPreferences("tv_remote_help", Context.MODE_PRIVATE) }
    var seenHelp by remember { mutableStateOf(helpPrefs.getBoolean("seen", false)) }
    var failure by remember(state.webEmbedUrl) { mutableStateOf<String?>(null) }
    var loading by remember(state.webEmbedUrl) { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }
    var host by remember { mutableStateOf<EmbedHost?>(null) }
    val currentOnBack by rememberUpdatedState(onBack)
    val prefs = remember { ProtectionPreferences(context) }
    val stats = remember { BlockingStatistics(context) }

    LaunchedEffect(Unit) {
        FilterListManager.initialize(context)
        ready = true
    }
    // Page completion does not prove a nested provider video has started.
    LaunchedEffect(state.webEmbedUrl, attempt, ready) {
        if (ready && state.webEmbedUrl != null) {
            kotlinx.coroutines.delay(25_000)
            if (loading) {
                loading = false
                menu = true
            }
        }
    }
    DisposableEffect(lifecycle, host) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> host?.web?.onPause()
                Lifecycle.Event.ON_RESUME -> host?.web?.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(menu, section, host, state.webEmbedUrl) {
        withFrameNanos { }
        if (menu) {
            if (section == "controls" && state.type == "series") nextFocus.requestFocus() else menuFocus.requestFocus()
        } else host?.focusPlayer()
    }
    BackHandler {
        if (host?.exitFullscreen() == true) {
            host?.focusPlayer()
        } else if (!menu) {
            menu = true
        } else if (section != "controls") {
            section = "controls"
        } else {
            currentOnBack()
        }
    }

    Box(Modifier.fillMaxSize().background(BgDark)) {
        if (ready && state.webEmbedUrl != null) {
            key(state.webEmbedUrl, attempt) {
                AndroidView(
                    factory = { ctx ->
                        EmbedHost(ctx) {
                            section = "controls"
                            menu = true
                        }.apply {
                            host = this
                            val rootUrl = state.webEmbedUrl
                            val sourceName = state.currentSource.name
                            web.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = false
                                allowContentAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                setSupportMultipleWindows(true)
                                javaScriptCanOpenWindowsAutomatically = false
                            }
                            web.setDownloadListener { _, _, _, _, _ -> /* No external downloads. */ }
                            web.webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(v: WebView?, dialog: Boolean, gesture: Boolean, msg: Message?): Boolean {
                                    stats.recordRedirectBlocked()
                                    return false
                                }
                                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                    showFullscreen(view, callback)
                                }
                                override fun onHideCustomView() { exitFullscreen() }
                                override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
                            }
                            web.webViewClient = object : WebViewClient() {
                                private fun blocked() = WebResourceResponse(
                                    "text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0))
                                )

                                override fun shouldInterceptRequest(v: WebView, request: WebResourceRequest): WebResourceResponse? {
                                    val url = request.url.toString()
                                    if (request.url.scheme != "https") return blocked()
                                    if (!prefs.isSourceExempt(sourceName)) {
                                        val decision = FilterListManager.engine.evaluate(
                                            url, rootUrl, prefs.isDomainValidationEnabled, prefs.isTrackerBlockingEnabled
                                        )
                                        if (decision.isBlocked) {
                                            if (decision.type == BlockType.TRACKER) stats.recordTrackerBlocked()
                                            else stats.recordThreatBlocked()
                                            return blocked()
                                        }
                                    }
                                    return null
                                }

                                override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                                    val url = request.url.toString()
                                    val safe = WebEmbedPolicy.allowNavigation(rootUrl, url, request.isForMainFrame, prefs.isRedirectIsolationEnabled)
                                    if (!safe) stats.recordRedirectBlocked()
                                    return !safe
                                }

                                override fun onPageFinished(v: WebView, url: String) {
                                    loading = false
                                }

                                override fun onReceivedError(v: WebView, request: WebResourceRequest, error: WebResourceError) {
                                    if (request.isForMainFrame) {
                                        failure = "The provider could not load. Retry or choose another source."
                                        loading = false
                                        menu = true
                                    }
                                }

                                override fun onReceivedHttpError(v: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                    if (request.isForMainFrame) {
                                        failure = "Provider returned HTTP ${response.statusCode}. Choose another source or retry."
                                        loading = false
                                        menu = true
                                    }
                                }
                                // Default SSL handling cancels certificate errors; never bypass it.
                            }
                            web.loadUrl(rootUrl)
                            post { if (!menu) focusPlayer() }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { released ->
                        released.exitFullscreen()
                        released.web.onPause()
                        released.web.stopLoading()
                        released.removeAllViews()
                        released.web.destroy()
                        if (host === released) host = null
                    }
                )
            }
        }

        if (!menu && !seenHelp) {
            Text("Arrows move pointer · OK clicks · Back opens controls and help", color = TextPrimary,
                fontSize = 16.sp, modifier = Modifier.align(Alignment.BottomCenter).background(BgDark).padding(16.dp))
        }
        if (loading && !menu) {
            CircularProgressIndicator(Modifier.align(Alignment.TopEnd).padding(24.dp), color = AccentCyan)
        }
        if (menu) {
            Column(
                Modifier.fillMaxHeight().width(480.dp).background(SurfaceDark.copy(alpha = 0.98f)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.title, color = TextPrimary, fontSize = 24.sp, maxLines = 2)
                if (state.type == "series") {
                    Text("Season ${state.season} · Episode ${state.episode}", color = TextSecondary, fontSize = 16.sp)
                    if (state.episodeTitle.isNotBlank()) Text(state.episodeTitle, color = TextPrimary, fontSize = 16.sp)
                }
                Text(state.currentSource.name, color = AccentCyan, fontSize = 14.sp)
                if (loading) Text("Opening provider page…", color = TextMuted, fontSize = 14.sp)
                TvPlayerActionButton(
                    "Return to video", null, true,
                    Modifier.focusRequester(menuFocus)
                ) {
                    menu = false
                    host?.focusPlayer()
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (section == "controls") {
                        if (failure != null || state.errorMessage != null) {
                            item { Text(failure ?: state.errorMessage.orEmpty(), color = AccentGold, fontSize = 15.sp) }
                        }
                        if (state.showSeriesComplete) {
                            item { Text("You reached the last known episode. You can choose another season or title.", color = TextSecondary, fontSize = 15.sp) }
                        }
                        if (!seenHelp) {
                            item {
                                Text("Arrows move the pointer · OK clicks · Hold an arrow to move faster. Back exits fullscreen, then opens these controls.",
                                    color = TextSecondary, fontSize = 15.sp)
                                TvPlayerActionButton("Got it", null) {
                                    seenHelp = true
                                    helpPrefs.edit().putBoolean("seen", true).apply()
                                }
                            }
                        }
                        item { TvPlayerActionButton("Change source", null) { section = "sources" } }
                        if (state.type == "series") {
                            item { TvPlayerActionButton(if (state.advancingEpisode) "Loading next episode…" else "Next episode", null, true, Modifier.focusRequester(nextFocus)) { viewModel.playNextEpisode() } }
                            state.episodeMessage?.let { message -> item { Text(message, color = AccentGold, fontSize = 16.sp) } }
                            item { Text("Automatic advancement is unavailable for this web provider. Use Next episode.", color = TextMuted, fontSize = 15.sp) }
                            item { TvPlayerActionButton("Episodes", null) { section = "episodes" } }
                        }
                        item { TvPlayerActionButton("Retry this source", null) {
                            failure = null
                            loading = true
                            attempt++
                        } }
                        item { TvPlayerActionButton("Remote help", null) { section = "help" } }
                        item { TvPlayerActionButton("Back to title", null, onClick = onBack) }
                    } else {
                        item { TvPlayerActionButton("Back to controls", null) { section = "controls" } }
                        when (section) {
                            "sources" -> {
                                item { Text("Choose a source", color = TextPrimary, fontSize = 20.sp) }
                                item { Text("A checkmark shows your chosen source. Availability depends on the provider.", color = TextSecondary, fontSize = 15.sp) }
                                items(SourceRegistry.UNSUPPORTED_WEB_SOURCES, key = { it.id }) { source ->
                                    TvPlayerActionButton(
                                        (if (source.id == state.currentSource.id) "✓ " else "") + source.name,
                                        null, isAccent = source.id == state.currentSource.id
                                    ) { viewModel.selectSource(source) }
                                }
                            }
                            "episodes" -> {
                                item {
                                    Text("Choose a season", color = TextPrimary, fontSize = 18.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(state.totalSeasons) { index ->
                                            val season = index + 1
                                            TvPlayerActionButton("Season $season", null,
                                                isAccent = state.episodeListSeason == season) { viewModel.loadEpisodes(season) }
                                        }
                                    }
                                }
                                if (state.episodesLoading) {
                                    item { Text("Loading episodes…", color = TextSecondary, fontSize = 15.sp) }
                                } else if (state.episodeList.isEmpty()) {
                                    item {
                                        Text("Episode details are unavailable. You can retry or use the next episode control.",
                                            color = TextSecondary, fontSize = 15.sp)
                                        TvPlayerActionButton("Retry episode list", null) { viewModel.loadEpisodes(state.episodeListSeason) }
                                    }
                                }
                                items(state.episodeList, key = { it.imdbID }) { episode ->
                                    val number = episode.episode.toIntOrNull() ?: 1
                                    val current = state.season == state.episodeListSeason && state.episode == number
                                    TvPlayerActionButton(
                                        (if (current) "✓ " else "") + "Episode $number · ${episode.title}",
                                        null, isAccent = current
                                    ) { viewModel.playEpisode(state.episodeListSeason, number) }
                                }
                                item { TvPlayerActionButton("Next episode", null) { viewModel.playNextEpisode() } }
                            }
                            "help" -> {
                                item { Text("Using your remote", color = TextPrimary, fontSize = 20.sp) }
                                item { Text(
                                    "Arrows: move the pointer.\nOK: click the provider’s buttons.\nHold an arrow: move faster.\nBack: leave fullscreen first, open controls next, then return to the title.\nMenu: open app controls.\n\nPlayback, seeking, and subtitles inside the video are controlled by the provider. If a page stays blank, use Retry this source or Change source.",
                                    color = TextSecondary, fontSize = 16.sp
                                ) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Touch events reach cross-origin iframe controls without a JavaScript-to-native bridge. */
@SuppressLint("ViewConstructor")
internal class EmbedHost(context: Context, private val openMenu: () -> Unit) : FrameLayout(context) {
    private var fullscreen: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    val web = PointerWebView(context)
    init {
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        web.onPointerChanged = { invalidate() }
        isFocusable = true
        isFocusableInTouchMode = true
    }
    fun focusPlayer() { requestFocus(); web.showPointer() }
    fun showFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (fullscreen != null) { callback.onCustomViewHidden(); return }
        fullscreen = view
        fullscreenCallback = callback
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        focusPlayer()
    }
    fun exitFullscreen(): Boolean {
        val view = fullscreen ?: return false
        removeView(view)
        fullscreen = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        return true
    }
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (fullscreen != null) web.drawPointer(canvas, false)
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_UP) {
                if (event.keyCode == KeyEvent.KEYCODE_MENU || !exitFullscreen()) openMenu()
                else focusPlayer()
            }
            return true
        }
        if (event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)) {
            if (event.action == KeyEvent.ACTION_DOWN &&
                (event.repeatCount == 0 || event.keyCode !in listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER))) {
                web.pointerKey(event.keyCode, event.repeatCount, fullscreen ?: web)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@SuppressLint("ViewConstructor")
internal class PointerWebView(context: Context) : WebView(context) {
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
    private var px = 0f
    private var py = 0f
    private var pointerVisible = false
    var onPointerChanged: () -> Unit = {}
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        px = w / 2f; py = h / 2f
    }
    fun pointerKey(code: Int, repeatCount: Int = 0, target: View = this) {
        val step = PointerMotion.stepDp(repeatCount) * resources.displayMetrics.density
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> px -= step
            KeyEvent.KEYCODE_DPAD_RIGHT -> px += step
            KeyEvent.KEYCODE_DPAD_UP -> py -= step
            KeyEvent.KEYCODE_DPAD_DOWN -> py += step
            else -> {
                val now = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(now, now, action, px, py, 0)
                    target.dispatchTouchEvent(event)
                    event.recycle()
                }
            }
        }
        px = px.coerceIn(0f, (width - 1).coerceAtLeast(0).toFloat())
        py = py.coerceIn(0f, (height - 1).coerceAtLeast(0).toFloat())
        showPointer()
    }
    fun showPointer() {
        pointerVisible = true
        invalidate()
        onPointerChanged()
        removeCallbacks(hidePointer)
        postDelayed(hidePointer, 2500)
    }
    private val hidePointer = Runnable { pointerVisible = false; invalidate(); onPointerChanged() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPointer(canvas)
    }
    fun drawPointer(canvas: Canvas, includeScroll: Boolean = true) {
        if (pointerVisible) {
            val x = px + if (includeScroll) scrollX else 0
            val y = py + if (includeScroll) scrollY else 0
            val radius = 6f * resources.displayMetrics.density
            canvas.drawCircle(x, y, radius + 2f, outline)
            canvas.drawCircle(x, y, radius, dot)
        }
    }
    override fun onDetachedFromWindow() {
        removeCallbacks(hidePointer)
        super.onDetachedFromWindow()
    }
}
