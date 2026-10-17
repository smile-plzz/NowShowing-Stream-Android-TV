package com.nowshowing.tv.ui.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nowshowing.tv.BuildConfig
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.ui.navigation.Screen
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var metadataKey by remember { mutableStateOf("") }
    var reportMessage by remember { mutableStateOf("") }
    var resetTips by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        TvTopBar(currentRoute = "settings", onNavigate = onNavigate)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp)
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Settings & Help",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "NowShowing TV ${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item(key = "autoplay") {
                SettingToggleItem("Autoplay next episode", "Default on. Applies only where verified video completion is supported. Web providers currently require manual Next episode.",
                    uiState.autoplay, viewModel::toggleAutoplay)
                Spacer(Modifier.height(16.dp))
            }
            item(key = "metadata") {
                Text("Metadata & recovery", color = TextPrimary, fontSize = 20.sp)
                Text(uiState.metadataStatus, color = TextMuted, fontSize = 16.sp)
                Text("Use your own authorized OMDb key. Stored locally on this TV; APK keys are not secret. No key pooling or quota multiplication.", color = TextMuted, fontSize = 15.sp)
                OutlinedTextField(value = metadataKey, onValueChange = { metadataKey = it },
                    label = { Text("Your OMDb key (blank restores demo key)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                TvPlayerActionButton("Save metadata key", null) { viewModel.saveMetadataKey(metadataKey); metadataKey = "" }
                TvPlayerActionButton("Retry metadata", null) { viewModel.retryMetadata() }
                TvPlayerActionButton(if (uiState.isClearingCache) "Clearing caches…" else "Clear metadata & image caches", null) { viewModel.clearCaches() }
                Text("Cache clearing preserves watchlist, history, manual watched labels and settings.", color = TextMuted, fontSize = 15.sp)
                uiState.maintenanceMessage?.let { Text(it, color = AccentCyan, fontSize = 16.sp) }
                Spacer(Modifier.height(20.dp))
            }
            item(key = "about") {
                Text("About NowShowing TV", color = TextPrimary, fontSize = 20.sp)
                Text("By Ismail Hossain · Standalone Android TV app. Movie and series playback uses external web providers. Native demo streams are diagnostic only.", color = TextMuted, fontSize = 16.sp)
                Text("Source & releases: github.com/smile-plzz/NowShowing-Stream-Android-TV", color = TextMuted, fontSize = 15.sp)
                Spacer(Modifier.height(16.dp))
            }
            item(key = "diagnostics") {
                Text("Device & metadata diagnostics", color = TextPrimary, fontSize = 20.sp)
                Text("Report excludes viewing history, URLs, keys and device serial numbers. Includes device model, certificate and recent metadata error categories.", color = TextMuted, fontSize = 15.sp)
                TvPlayerActionButton("Refresh diagnostics", null) { viewModel.refreshDiagnostics() }
                TvPlayerActionButton("Copy diagnostic report", null) {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NowShowing diagnostics", uiState.diagnosticText))
                    reportMessage = "Report copied. It is also shown below."
                }
                TvPlayerActionButton("Share diagnostic report", null) {
                    reportMessage = runCatching {
                        context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, uiState.diagnosticText)
                        }, "Share diagnostics"))
                        "Choose a share destination."
                    }.getOrElse { "No sharing app available. Use Copy diagnostic report or photograph the report below." }
                }
                if (reportMessage.isNotBlank()) Text(reportMessage, color = AccentCyan, fontSize = 16.sp)
                SelectionContainer { Text(uiState.diagnosticText, color = TextPrimary, fontSize = 16.sp) }
                Spacer(Modifier.height(20.dp))
            }
            item(key = "remote_help") {
                Text("Remote help", color = TextPrimary, fontSize = 20.sp)
                Text("Arrows move the pointer · OK clicks · Hold arrows to move faster. Back exits fullscreen, then opens player controls.",
                    color = TextMuted, fontSize = 16.sp)
                TvPlayerActionButton(if (resetTips) "Tips will appear next time" else "Show first-use tips again", null) {
                    context.getSharedPreferences("tv_remote_help", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("seen", false).apply()
                    resetTips = true
                }
                Spacer(Modifier.height(16.dp))
            }
            item(key = "playback_diagnostics") {
                Text("Playback diagnostics", color = TextPrimary, fontSize = 20.sp)
                Text("These demo videos test the TV’s native player. They do not play the movie you selected.",
                    color = TextMuted, fontSize = 15.sp)
                SourceRegistry.NATIVE_SOURCES.forEach { source ->
                    TvPlayerActionButton(source.name, null) {
                        onNavigate(Screen.Player.createRoute(source.id, 1, 1, "diagnostic"))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
            // Statistics Overview Card
            item(key = "stats_card") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBox(label = "Ad requests blocked", count = uiState.totalThreatsBlocked, color = AccentCyan)
                    StatBox(label = "Trackers Blocked", count = uiState.totalTrackersBlocked, color = AccentGold)
                    StatBox(label = "Navigation blocked", count = uiState.totalRedirectsBlocked, color = Color(0xFFFF5252))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Core Controls Section
            item(key = "controls_section") {
                Text(
                    text = "Protection Controls",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleItem(
                    title = "Ad request filtering",
                    description = "Filters requests matched by bundled ad rules",
                    isEnabled = uiState.isDomainValidationEnabled,
                    onToggle = { viewModel.toggleDomainValidation() }
                )

                SettingToggleItem(
                    title = "Telemetry & Tracker Blocking",
                    description = "Blocks third-party analytics beacons and background tracking endpoints",
                    isEnabled = uiState.isTrackerBlockingEnabled,
                    onToggle = { viewModel.toggleTrackerBlocking() }
                )

                SettingToggleItem(
                    title = "Keep the player page contained",
                    description = "Restricts embedded player page navigation to its original host",
                    isEnabled = uiState.isRedirectIsolationEnabled,
                    onToggle = { viewModel.toggleRedirectIsolation() }
                )

                Text("Secure connections are required for embedded players. Certificate errors are never bypassed.",
                    color = TextMuted, fontSize = 15.sp)

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Maintenance & Update Actions
            item(key = "maintenance_section") {
                Text(
                    text = "Filter Lists & Maintenance",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TvSettingsButton(
                        label = if (uiState.isUpdatingFilters) "Updating…" else "Update Filter Lists Online",
                        icon = Icons.Default.Refresh,
                        isLoading = uiState.isUpdatingFilters,
                        onClick = { viewModel.updateFiltersOnline() }
                    )

                    TvSettingsButton(
                        label = "Reset Statistics",
                        icon = Icons.Default.Close,
                        onClick = { viewModel.resetStatistics() }
                    )
                }

                if (!uiState.updateStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.updateStatusMessage ?: "",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Source Exceptions
            if (uiState.exemptSources.isNotEmpty()) {
                item(key = "exemptions_section") {
                    Text(
                        text = "Exempt Video Sources (Protection Disabled)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(uiState.exemptSources.toList()) { source ->
                    ExemptSourceRow(
                        name = source,
                        onRemove = { viewModel.removeSourceExemption(source) }
                    )
                }

                item(key = "clear_all_exemptions") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TvSettingsButton(
                        label = "Clear All Source Exceptions",
                        icon = Icons.Default.Close,
                        onClick = { viewModel.clearSourceExceptions() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, count: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) FocusBorder else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isFocused) Color.Black else TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = if (isFocused) Color.Black.copy(alpha = 0.7f) else TextMuted,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(modifier = Modifier.scale(1.2f)) {
            Switch(
                checked = isEnabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentCyan,
                    checkedTrackColor = AccentCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = SurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun TvSettingsButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .shadow(
                elevation = if (isFocused) 12.dp else 2.dp,
                shape = RoundedCornerShape(8.dp),
                spotColor = if (isFocused) FocusGlow else Color.Transparent
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) FocusBorder else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = if (isFocused) Color.Black else AccentCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) Color.Black else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = if (isFocused) Color.Black else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExemptSourceRow(
    name: String,
    onRemove: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) FocusBorder else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onRemove)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = if (isFocused) Color.Black else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Click OK to Re-enable Protection",
            color = if (isFocused) Color.Black else AccentGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
