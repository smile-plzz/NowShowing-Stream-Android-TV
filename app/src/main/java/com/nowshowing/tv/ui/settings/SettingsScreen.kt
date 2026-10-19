package com.nowshowing.tv.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowshowing.tv.BuildConfig
import com.nowshowing.tv.data.repository.MetadataHealth
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.ui.common.TvSectionTab
import com.nowshowing.tv.ui.common.TvTopBar
import com.nowshowing.tv.ui.navigation.Screen
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import com.nowshowing.tv.ui.theme.*

private enum class SettingsSection(val label: String, val description: String) {
    PLAYBACK("Playback", "Settle in and keep watching."),
    METADATA("Metadata", "Keep titles available and recover when loading fails."),
    PRIVACY("Privacy", "Manage request filtering and source exceptions."),
    HELP("Help", "Remote controls and playback recovery."),
    HEALTH("App health", "Run a local check or record browsing; freeze the report for photographs."),
    ABOUT("About", "App information and a report you can share for support.")
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigate: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sectionName by rememberSaveable {
        mutableStateOf(if (MetadataHealth.failure.value != null) SettingsSection.METADATA.name else SettingsSection.PLAYBACK.name)
    }
    val section = SettingsSection.valueOf(sectionName)
    var metadataKey by remember { mutableStateOf("") }
    var reportMessage by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }
    var tipsReset by remember { mutableStateOf(false) }
    val entryFocus = remember { FocusRequester() }
    val reportFocus = remember { FocusRequester() }
    var restoreReportFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { withFrameNanos { }; entryFocus.requestFocus() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var live by remember { mutableStateOf("Live readings unavailable") }
    val recording by com.nowshowing.tv.diagnostics.BrowsingRecorder.recording.collectAsStateWithLifecycle()
    LaunchedEffect(section, lifecycleOwner, showReport) {
        if (section == SettingsSection.HEALTH && !showReport) lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) { live = liveMemory(context); kotlinx.coroutines.delay(2000) }
        }
    }
    LaunchedEffect(restoreReportFocus) {
        if (restoreReportFocus) { withFrameNanos { }; reportFocus.requestFocus(); restoreReportFocus = false }
    }
    if (showReport) state.healthReport?.let { HealthPhotoDialog(it) { showReport = false; restoreReportFocus = true } }

    Column(Modifier.fillMaxSize().background(BgDark)) {
        TvTopBar("settings", onNavigate)
        Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Settings", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}", color = TextMuted, fontSize = 15.sp)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 40.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SettingsSection.entries, key = { it.name }) { tab ->
                TvSectionTab(tab.label, tab == section,
                    modifier = if (tab == section) Modifier.focusRequester(entryFocus) else Modifier) { sectionName = tab.name }
            }
        }
        key(section) {
            val scroll = rememberLazyListState()
            LazyColumn(state = scroll, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item(key = "intro") { Text(section.description, color = TextSecondary, fontSize = 17.sp) }
                when (section) {
                    SettingsSection.PLAYBACK -> {
                        item(key = "autoplay") {
                            SettingToggleItem("Autoplay next episode",
                                "Continue automatically when the source supports verified completion. Web players currently use manual Next episode.",
                                state.autoplay, viewModel::toggleAutoplay)
                        }
                        item(key = "playback_help") {
                            InfoCard("Watching a series", "Press Back to open player controls. Next episode is ready to select; Episodes lets you change the season or episode. Your last episode and source are remembered on this TV.")
                        }
                    }
                    SettingsSection.METADATA -> {
                        item(key = "status") {
                            InfoCard("Metadata status", state.metadataStatus)
                            Text(if (state.hasPersonalKey) "${state.personalKeyCount} personal OMDb key(s) configured" else "Using the shared demo key",
                                color = AccentCyan, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
                        }
                        item(key = "key") {
                            Text("Personal OMDb keys", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Enter up to five authorized keys, separated by commas or spaces. Stored on this TV. Invalid keys fall back in order; quota limits are respected.", color = TextSecondary, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = metadataKey, onValueChange = { metadataKey = it }, singleLine = true,
                                label = { Text("Enter OMDb keys") }, visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            TvPlayerActionButton("Save personal keys", null, enabled = metadataKey.isNotBlank()) {
                                if (viewModel.saveMetadataKey(metadataKey)) metadataKey = ""
                            }
                            if (state.hasPersonalKey) TvPlayerActionButton("Use demo key", null) { viewModel.saveMetadataKey("") }
                        }
                        item(key = "retry") { TvPlayerActionButton("Retry metadata", null) { viewModel.retryMetadata() } }
                        item(key = "cache") {
                            InfoCard("Refresh cached content", "Clear downloaded metadata and artwork to rebuild them. Watchlist, history, watched labels and settings are preserved. Useful offline metadata will need to download again.")
                            Spacer(Modifier.height(8.dp))
                            TvPlayerActionButton(if (state.isClearingCache) "Clearing caches…" else "Clear metadata & artwork caches", null,
                                enabled = !state.isClearingCache) { viewModel.clearCaches() }
                        }
                        state.maintenanceMessage?.let { message -> item(key = "metadata_feedback") { Feedback(message) } }
                    }
                    SettingsSection.PRIVACY -> {
                        item(key = "filtering") { SettingToggleItem("Ad request filtering", "Block requests matched by the app's ad rules.", state.isDomainValidationEnabled, viewModel::toggleDomainValidation) }
                        item(key = "tracking") { SettingToggleItem("Tracker blocking", "Filter requests matched by known tracker rules.", state.isTrackerBlockingEnabled, viewModel::toggleTrackerBlocking) }
                        item(key = "navigation") { SettingToggleItem("Keep the player page contained", "Restrict the provider's main page to its original host.", state.isRedirectIsolationEnabled, viewModel::toggleRedirectIsolation) }
                        item(key = "privacy_note") { InfoCard("Protection has limits", "Secure connections and valid certificates are required. Filtering covers requests exposed by WebView; it cannot remove every advertisement. Source exceptions disable filtering only for that source.") }
                        item(key = "filter_update") {
                            TvPlayerActionButton(if (state.isUpdatingFilters) "Updating filter lists…" else "Update filter lists", null,
                                enabled = !state.isUpdatingFilters) { viewModel.updateFiltersOnline() }
                            state.updateStatusMessage?.let { Feedback(it) }
                        }
                        item(key = "stats") {
                            InfoCard("Requests blocked", "Ads: ${state.totalThreatsBlocked}    Trackers: ${state.totalTrackersBlocked}    Navigation: ${state.totalRedirectsBlocked}")
                            Spacer(Modifier.height(8.dp))
                            TvPlayerActionButton("Reset blocking counts", null) { viewModel.resetStatistics() }
                        }
                        if (state.exemptSources.isNotEmpty()) {
                            item(key = "exceptions_header") { Text("Source exceptions", color = TextPrimary, fontSize = 20.sp) }
                            items(state.exemptSources.sorted(), key = { "exception:$it" }) { source ->
                                TvPlayerActionButton("Restore protection · $source", null) { viewModel.removeSourceExemption(source) }
                            }
                            item(key = "clear_exceptions") { TvPlayerActionButton("Restore protection for all sources", null) { viewModel.clearSourceExceptions() } }
                        }
                    }
                    SettingsSection.HELP -> {
                        item(key = "remote") { InfoCard("Your remote", "Browse: arrows move focus; OK selects.\nPlayer: arrows move the pointer; OK clicks; hold an arrow to move faster.\nBack leaves provider fullscreen first, opens app controls next, then returns to the title. Menu opens controls directly.") }
                        item(key = "tips") {
                            TvPlayerActionButton(if (tipsReset) "Tips will appear next time" else "Show first-use tips again", null) {
                                context.getSharedPreferences("tv_remote_help", Context.MODE_PRIVATE).edit().putBoolean("seen", false).apply()
                                tipsReset = true
                            }
                        }
                        item(key = "recovery") { InfoCard("If a source will not play", "Open controls with Back. Try Retry this source, then Change source. A loaded webpage does not prove that its video is playing. Provider subtitles and seeking stay inside the provider player.") }
                    }
                    SettingsSection.HEALTH -> {
                        item(key = "live") { InfoCard("Live memory · updates every 2 seconds", live) }
                        item(key = "record_help") { InfoCard("Record real browsing", "Start recording, then browse Home, Movies, TV, Search or Watchlist for up to 60 seconds. Return here to stop and freeze a report. Recording also stops when the app is backgrounded. Settings and playback frames are excluded; no input or video FPS is measured.") }
                        item(key = "record") { TvPlayerActionButton(if (recording) "Stop recording" else "Record browsing performance", null) {
                            if (recording) com.nowshowing.tv.diagnostics.BrowsingRecorder.stop()
                            else {
                                val activity = context.findActivity()
                                if (activity != null) { com.nowshowing.tv.diagnostics.BrowsingRecorder.start(activity.window); reportMessage = "Recording started. Navigate to a collection, then return here." }
                                else reportMessage = "Frame recording unavailable in this window."
                            }
                        } }
                        item(key = "run_health") { TvPlayerActionButton(if (state.isRefreshingDiagnostics) "Capturing report…" else "Run health check / Freeze report", null,
                            enabled = !state.isRefreshingDiagnostics) {
                            com.nowshowing.tv.diagnostics.BrowsingRecorder.stop()
                            viewModel.refreshDiagnostics()
                        } }
                        item(key = "snapshot_help") { InfoCard("Photo report", "The saved report is a frozen local snapshot. Every page repeats its report ID, build and original capture time. No network probe or playback test runs. Unknown measurements stay unavailable. Run again to replace it.") }
                        item(key = "view_report") { TvPlayerActionButton("View last frozen report", null, modifier = Modifier.focusRequester(reportFocus), enabled = state.healthReport != null) { showReport = true } }
                        item(key = "delete_report") { TvPlayerActionButton("Delete saved report", null, enabled = state.healthReport != null && !state.isRefreshingDiagnostics) { viewModel.deleteReport() } }
                        item(key = "copy_report") { TvPlayerActionButton("Copy report", null, enabled = state.reportReady) {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("NowShowing diagnostics", state.diagnosticText))
                            reportMessage = "Report copied."
                        } }
                        if (reportMessage.isNotBlank()) item(key = "report_feedback") { Feedback(reportMessage) }
                    }
                    SettingsSection.ABOUT -> {
                        item(key = "about") { InfoCard("NowShowing TV", "Created by Ismail Hossain\nA standalone movie and series discovery app for Android TV.\nVersion ${BuildConfig.VERSION_NAME} · Code ${BuildConfig.VERSION_CODE}\nVariant ${BuildConfig.BUILD_TYPE}\nCommit ${BuildConfig.BUILD_COMMIT}") }
                        listOf(
                            "GitHub profile" to "https://github.com/smile-plzz",
                            "Source code" to "https://github.com/smile-plzz/NowShowing-Stream-Android-TV",
                            "Releases & downloads" to "https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases",
                            "Report an issue" to "https://github.com/smile-plzz/NowShowing-Stream-Android-TV/issues"
                        ).forEach { (label, url) ->
                            item(key = label) {
                                InfoCard(label, url)
                                TvPlayerActionButton("Open $label", null) {
                                    reportMessage = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))); "Opened $label." }
                                        .getOrElse { "No browser is available. The full address remains visible above." }
                                }
                            }
                        }
                        item(key = "limits") { InfoCard("Playback & troubleshooting", "Metadata: OMDb API. Playback: Android System WebView and external providers; availability varies. Page loading does not prove playback. Web progress/completion is unavailable. Use Back → Retry or Change source. Diagnostics are in App health.") }
                        item(key = "dependencies") { InfoCard("Built with", "Kotlin, Android Jetpack Compose, Coil, Retrofit, OkHttp, Gson, SQLite and AndroidX Media3. Source includes build dependencies and project documentation. Content and metadata belong to their respective owners.") }
                        if (reportMessage.isNotBlank()) item(key = "link_feedback") { Feedback(reportMessage) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Column(Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(12.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(text, color = TextSecondary, fontSize = 16.sp, lineHeight = 24.sp)
    }
}

@Composable
private fun Feedback(message: String) {
    Text(message, color = AccentCyan, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SettingToggleItem(title: String, description: String, enabled: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(Modifier.fillMaxWidth().background(if (focused) FocusBorder else SurfaceDark, RoundedCornerShape(12.dp))
        .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else CardBorder, RoundedCornerShape(12.dp))
        .semantics { stateDescription = if (enabled) "On" else "Off" }
        .clickable(interactionSource = interaction, indication = null, role = Role.Switch, onClick = onToggle)
        .padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = if (focused) Color.Black else TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(description, color = if (focused) Color.Black.copy(alpha = 0.8f) else TextSecondary, fontSize = 16.sp, lineHeight = 22.sp)
        }
        Spacer(Modifier.width(20.dp))
        Text(if (enabled) "ON" else "OFF", color = if (focused) Color.Black else if (enabled) AccentCyan else TextMuted,
            fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
