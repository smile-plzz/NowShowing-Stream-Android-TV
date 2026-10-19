package com.nowshowing.tv.ui.settings

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.Keep
import com.nowshowing.tv.BuildConfig
import com.nowshowing.tv.diagnostics.BrowsingRecorder
import com.nowshowing.tv.data.repository.MetadataHealth
import com.nowshowing.tv.privacy.ProtectionPreferences
import com.google.gson.Gson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Keep
data class HealthPage(val title: String, val body: String)
@Keep
data class HealthReport(val id: String, val capturedAt: String, val version: String, val pages: List<HealthPage>) {
    fun text() = "$id · $capturedAt · $version\n" + pages.mapIndexed { i, p -> "Page ${i + 1}/${pages.size}: ${p.title}\n${p.body}" }.joinToString("\n\n")
}

/** Conservatively split long evidence into continuation pages; never ellipsize or discard values. */
fun photoPages(title: String, text: String): List<HealthPage> {
    val lines = text.lines().flatMap { if (it.isEmpty()) listOf("") else it.chunked(32) }
    return lines.chunked(5).mapIndexed { index, chunk ->
        HealthPage(if (index == 0) title else "$title · continued ${index + 1}", chunk.joinToString("\n"))
    }
}

fun liveMemory(context: Context): String {
    val runtime = Runtime.getRuntime()
    val memory = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
    fun mib(n: Long) = "${n / (1024 * 1024)} MiB"
    return "App heap ${mib(runtime.totalMemory() - runtime.freeMemory())} / ${mib(runtime.maxMemory())}\nSystem available ${mib(memory.availMem)}; low memory: ${memory.lowMemory}"
}

/** One bounded local report, replaced only by explicit capture; no upload, no history/title data. */
object HealthReports {
    private fun prefs(context: Context) = context.getSharedPreferences("health_report", Context.MODE_PRIVATE)
    fun load(context: Context): HealthReport? = runCatching {
        Gson().fromJson(prefs(context).getString("last", null), HealthReport::class.java)
    }.getOrNull()?.takeIf { it.pages.isNotEmpty() }
    fun delete(context: Context) { prefs(context).edit().clear().apply() }
    fun capture(context: Context): HealthReport {
        val time = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss VV"))
        val report = HealthReport(UUID.randomUUID().toString().take(8), time,
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", buildList {
                addAll(photoPages("Summary", "Local snapshot; no automatic upload.\nMetadata: ${MetadataHealth.failure.value?.name ?: "No failure recorded"}\nNetwork probe: skipped; playback not tested.\nNext: record browsing and photograph its pages.\nCompare three matched runs on the same TV."))
                addAll(photoPages("Browsing performance", BrowsingRecorder.lastSummary))
                addAll(photoPages("Memory", liveMemory(context)))
                addAll(photoPages("Metadata requests", com.nowshowing.tv.diagnostics.MetadataMetrics.summary()))
                // diagnosticReport constructs an allowlist of public environment fields and error categories.
                val environment = diagnosticReport(context)
                addAll(photoPages("Environment, cache & metadata", environment))
                val prefs = ProtectionPreferences(context)
                addAll(photoPages("Privacy & limits", "Ad filtering: ${prefs.isDomainValidationEnabled}\nTracker filtering: ${prefs.isTrackerBlockingEnabled}\nFiltering covers WebView-exposed requests only.\nInput, CPU/GPU and video performance: unavailable.\nNo universal health score is calculated."))
            })
        prefs(context).edit().putString("last", Gson().toJson(report)).apply()
        return report
    }
}
