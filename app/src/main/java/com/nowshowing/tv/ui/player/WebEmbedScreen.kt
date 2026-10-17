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
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var ready by remember { mutableStateOf(false) }
    var menu by remember(state.webEmbedUrl) { mutableStateOf(true) }
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
    LaunchedEffect(menu) {
        if (menu) menuFocus.requestFocus()
    }
    BackHandler {
        if (host?.exitFullscreen() == true) {
            menu = true
        } else if (!menu) {
            menu = true
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
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { released ->
                        released.exitFullscreen()
                        released.web.stopLoading()
                        released.removeAllViews()
                        released.web.destroy()
                        if (host === released) host = null
                    }
                )
            }
        }

        if (loading && !menu) {
            CircularProgressIndicator(Modifier.align(Alignment.TopEnd).padding(24.dp), color = AccentCyan)
        }
        if (menu) {
            Column(
                Modifier.fillMaxHeight().width(420.dp).background(SurfaceDark.copy(alpha = 0.98f)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.title, color = TextPrimary, fontSize = 24.sp)
                Text(
                    if (state.type == "series") "Season ${state.season} · Episode ${state.episode}" else "Movie",
                    color = TextSecondary
                )
                Text(failure ?: state.errorMessage ?: "Use the D-pad to move the pointer and OK to click the provider’s play button. Back or Menu opens these controls.", color = TextSecondary, fontSize = 14.sp)
                TvPlayerActionButton("Enter player", null, true, Modifier.focusRequester(menuFocus)) {
                    menu = false
                    host?.focusPlayer()
                }
                TvPlayerActionButton("Reload source", null) {
                    failure = null
                    loading = true
                    attempt++
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.type == "series") {
                        TvPlayerActionButton("Previous", null) { viewModel.playPreviousEpisode() }
                        TvPlayerActionButton("Next episode", null) { viewModel.playNextEpisode() }
                    }
                    TvPlayerActionButton("Back", null, onClick = onBack)
                }
                Text("Sources", color = TextPrimary, fontSize = 18.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SourceRegistry.UNSUPPORTED_WEB_SOURCES, key = { it.id }) { source ->
                        TvPlayerActionButton(
                            (if (source.id == state.currentSource.id) "✓ " else "") + source.name, null
                        ) { viewModel.selectSource(source) }
                    }
                    item {
                        Text("Native playback demos", color = TextSecondary, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    items(SourceRegistry.NATIVE_SOURCES, key = { it.id }) { source ->
                        TvPlayerActionButton(source.name, null) { viewModel.selectSource(source) }
                    }
                }
                Text("Web-player progress and subtitles are managed by the provider. If a player stays blank, choose another source.", color = TextMuted, fontSize = 12.sp)
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
        isFocusable = true
        isFocusableInTouchMode = true
    }
    fun focusPlayer() { requestFocus() }
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
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_UP) { exitFullscreen(); openMenu() }
            return true
        }
        if (event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)) {
            if (fullscreen != null) return super.dispatchKeyEvent(event)
            if (event.action == KeyEvent.ACTION_DOWN) web.pointerKey(event.keyCode)
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
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        px = w / 2f; py = h / 2f
    }
    fun pointerKey(code: Int) {
        val step = 24f * resources.displayMetrics.density
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> px -= step
            KeyEvent.KEYCODE_DPAD_RIGHT -> px += step
            KeyEvent.KEYCODE_DPAD_UP -> py -= step
            KeyEvent.KEYCODE_DPAD_DOWN -> py += step
            else -> {
                val now = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(now, now, action, px, py, 0)
                    dispatchTouchEvent(event)
                    event.recycle()
                }
            }
        }
        px = px.coerceIn(0f, (width - 1).coerceAtLeast(0).toFloat())
        py = py.coerceIn(0f, (height - 1).coerceAtLeast(0).toFloat())
        pointerVisible = true
        invalidate()
        removeCallbacks(hidePointer)
        postDelayed(hidePointer, 2500)
    }
    private val hidePointer = Runnable { pointerVisible = false; invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pointerVisible) {
            val radius = 6f * resources.displayMetrics.density
            canvas.drawCircle(px + scrollX, py + scrollY, radius + 2f, outline)
            canvas.drawCircle(px + scrollX, py + scrollY, radius, dot)
        }
    }
    override fun onDetachedFromWindow() {
        removeCallbacks(hidePointer)
        super.onDetachedFromWindow()
    }
}
