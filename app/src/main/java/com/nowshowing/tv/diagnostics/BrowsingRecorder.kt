package com.nowshowing.tv.diagnostics

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Opt-in window rendering metrics. No titles, routes, URLs or input events are retained. */
object BrowsingRecorder {
    private val handler = Handler(Looper.getMainLooper())
    private val _recording = MutableStateFlow(false)
    val recording = _recording.asStateFlow()
    private var window: Window? = null
    private var started = 0L
    private var screen = "Settings"
    private var refreshHz = Float.NaN
    private var samples = 0
    private var slow = 0
    private var missingCallbacks = 0
    private val durations = DoubleArray(12000)
    private val screens = linkedSetOf<String>()
    var lastSummary: String = "Not recorded. Select Record browsing performance, then navigate for up to 60 seconds."
        private set
    private val timeout = Runnable { stop("60 second limit") }
    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
        if (_recording.value && screen in setOf("Home", "Movies", "TV", "Search", "Watchlist", "Mood", "Details")) {
            val ns = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            if (ns >= 0) {
                val ms = ns / 1_000_000.0
                durations[samples] = ms; samples++; screens.add(screen)
                if (ms > 1000.0 / refreshHz) slow++
                missingCallbacks += dropped
                if (samples >= 12000) stop("sample limit")
            }
        }
    }

    fun setScreen(route: String?) {
        screen = when (route?.substringBefore('/')) {
            "home" -> "Home"; "movies" -> "Movies"; "tv" -> "TV"; "search" -> "Search"
            "watchlist" -> "Watchlist"; "mood" -> "Mood"; "details" -> "Details"; else -> "Settings / playback (excluded)"
        }
    }

    @Suppress("DEPRECATION")
    fun start(target: Window) {
        stop("restarted")
        window = target; refreshHz = target.windowManager.defaultDisplay.refreshRate.takeIf { it > 0 && it.isFinite() } ?: Float.NaN
        screens.clear(); samples = 0; slow = 0; missingCallbacks = 0
        started = SystemClock.elapsedRealtime()
        try {
            target.addOnFrameMetricsAvailableListener(listener, handler)
            _recording.value = true
            handler.postDelayed(timeout, 60_000)
        } catch (_: Exception) { window = null; lastSummary = "Unavailable: this window did not accept frame instrumentation." }
    }

    fun stop(reason: String = "stopped by owner") {
        if (!_recording.value) return
        _recording.value = false
        handler.removeCallbacks(timeout)
        window?.removeOnFrameMetricsAvailableListener(listener); window = null
        val seconds = (SystemClock.elapsedRealtime() - started) / 1000.0
        val sorted = durations.copyOf(samples).sorted()
        fun percentile(p: Double) = if (sorted.isEmpty()) "unavailable" else String.format(Locale.US, "%.2f ms", sorted[(kotlin.math.ceil(sorted.size * p).toInt() - 1).coerceAtLeast(0)])
        lastSummary = "Captured: ${java.time.ZonedDateTime.now()}\nDuration: ${String.format(Locale.US, "%.1f", seconds)} s; $reason\n" +
            "Scenario: ${screens.joinToString().ifEmpty { "No browsing frames observed" }}\n" +
            "Method: FrameMetrics TOTAL_DURATION, app window rendering (not video FPS or input latency).\n" +
            "Samples: $samples; refresh at start: ${refreshHz.takeIf { it.isFinite() }?.let { "$it Hz" } ?: "unavailable"}\n" +
            "Median: ${percentile(.5)}; p95: ${percentile(.95)}; p99: ${percentile(.99)}\n" +
            (if (refreshHz.isFinite()) "Slow frames: $slow (duration > ${String.format(Locale.US, "%.2f", 1000.0 / refreshHz)} ms).\n" else "Slow-frame threshold: unavailable (unknown refresh rate).\n") +
            "Lost metric callbacks: $missingCallbacks (not dropped display frames).\n" +
            "Settings and playback excluded. Input latency, CPU/GPU load and display drops not measured. Refresh-rate changes are not tracked."

    }
}
