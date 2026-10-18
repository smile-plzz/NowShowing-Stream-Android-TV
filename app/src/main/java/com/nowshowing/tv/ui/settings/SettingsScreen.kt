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
    HELP("Help", "Remote controls and playback checks."),
    ABOUT("About", "App information and a report you can share for support.")
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigate: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
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
    LaunchedEffect(section) { if (section == SettingsSection.ABOUT) viewModel.refreshDiagnostics() }
    LaunchedEffect(restoreReportFocus) {
        if (restoreReportFocus) { withFrameNanos { }; reportFocus.requestFocus(); restoreReportFocus = false }
    }
    if (showReport) DiagnosticReportDialog(state.diagnosticText) { showReport = false; restoreReportFocus = true }

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
                        item(key = "demo_header") { InfoCard("Native playback checks", "These reference videos test the TV's native player. They do not play the movie you selected.") }
                        items(SourceRegistry.NATIVE_SOURCES, key = { "demo:${it.id}" }) { source ->
                            TvPlayerActionButton(source.name, null) { onNavigate(Screen.Player.createRoute(source.id, 1, 1, "diagnostic")) }
                        }
                    }
                    SettingsSection.ABOUT -> {
                        item(key = "about") { InfoCard("NowShowing TV", "By Ismail Hossain\nVersion ${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}\nA standalone Android TV app. Movie and series providers play inside Android System WebView.") }
                        item(key = "source") { Text("Source & releases\ngithub.com/smile-plzz/NowShowing-Stream-Android-TV", color = TextSecondary, fontSize = 16.sp) }
                        item(key = "report_intro") { InfoCard("Support report", "Includes app/device/WebView details, public certificate, cache sizes and recent metadata error categories. Excludes viewing history, provider URLs, API keys and device serial numbers. Review it before sharing.") }
                        item(key = "refresh_report") { TvPlayerActionButton(if (state.isRefreshingDiagnostics) "Refreshing report…" else "Refresh report", null,
                            enabled = !state.isRefreshingDiagnostics) { viewModel.refreshDiagnostics() } }
                        if (!state.reportReady && !state.isRefreshingDiagnostics) item(key = "report_unavailable") { Feedback(state.diagnosticText) }
                        item(key = "view_report") { TvPlayerActionButton("View diagnostic report", null, modifier = Modifier.focusRequester(reportFocus), enabled = state.reportReady) { showReport = true } }
                        item(key = "copy_report") { TvPlayerActionButton("Copy diagnostic report", null, enabled = state.reportReady) {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("NowShowing diagnostics", state.diagnosticText))
                            reportMessage = "Report copied."
                        } }
                        item(key = "share_report") { TvPlayerActionButton("Share diagnostic report", null, enabled = state.reportReady) {
                            reportMessage = runCatching {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.diagnosticText)
                                }, "Share diagnostics")); "Choose a share destination."
                            }.getOrElse { "No sharing app is available. View the report and photograph it, or use Copy." }
                        } }
                        if (reportMessage.isNotBlank()) item(key = "report_feedback") { Feedback(reportMessage) }
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

@Composable
private fun DiagnosticReportDialog(report: String, onClose: () -> Unit) {
    val close = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxWidth(0.92f).fillMaxHeight(0.9f)
            .background(BgDark, RoundedCornerShape(20.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Diagnostic report", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Use arrows to read each section. Back or Done closes the report.", color = TextSecondary, fontSize = 16.sp)
            TvPlayerActionButton("Done", null, modifier = Modifier.focusRequester(close), onClick = onClose)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(report.lines().chunked(3)) { lines ->
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Text(lines.joinToString("\n"), color = TextPrimary, fontSize = 16.sp, lineHeight = 24.sp,
                        modifier = Modifier.fillMaxWidth().border(2.dp, if (focused) AccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .focusable(interactionSource = interaction).padding(12.dp))
                }
            }
        }
        LaunchedEffect(Unit) { withFrameNanos { }; close.requestFocus() }
    }
}
