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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
                            text = "Security & Network Isolation",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Native stream validation, HTTPS enforcement, and redirect isolation for Android TV",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                    StatBox(label = "Malicious Hosts Intercepted", count = uiState.totalThreatsBlocked, color = AccentCyan)
                    StatBox(label = "Trackers Blocked", count = uiState.totalTrackersBlocked, color = AccentGold)
                    StatBox(label = "Dangerous Redirects Capped", count = uiState.totalRedirectsBlocked, color = Color(0xFFFF5252))
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
                    title = "Media Domain & Endpoint Validation",
                    description = "Validates remote streaming hosts and media manifests against verified filter lists",
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
                    title = "Redirect Depth Capping & Loop Defense",
                    description = "Restricts network redirect chains to 5 hops to prevent tracking loops",
                    isEnabled = uiState.isRedirectIsolationEnabled,
                    onToggle = { viewModel.toggleRedirectIsolation() }
                )

                SettingToggleItem(
                    title = "Strict HTTPS & Protocol Isolation",
                    description = "Enforces secure HTTPS for all remote media streams (exempts local RFC-1918 subnets)",
                    isEnabled = uiState.isHttpsEnforced,
                    onToggle = { viewModel.toggleHttpsEnforcement() }
                )

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
            .focusable(interactionSource = interactionSource)
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
                onCheckedChange = { onToggle() },
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
            .focusable(interactionSource = interactionSource)
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
            fontSize = 13.sp,
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
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onRemove)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = if (isFocused) Color.Black else TextPrimary,
            fontSize = 13.sp,
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
