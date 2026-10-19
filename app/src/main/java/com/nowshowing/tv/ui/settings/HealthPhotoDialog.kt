package com.nowshowing.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import com.nowshowing.tv.ui.theme.*

@Composable
fun HealthPhotoDialog(report: HealthReport, onClose: () -> Unit) {
    var index by rememberSaveable(report.id) { mutableIntStateOf(0) }
    val done = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(BgDark).padding(horizontal = 36.dp, vertical = 20.dp)) {
            val compact = maxHeight < 430.dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
                Text("App health · Page ${index + 1} of ${report.pages.size}", color = TextPrimary,
                    fontSize = if (compact) 20.sp else 24.sp, fontWeight = FontWeight.Bold)
                Text("Report ${report.id} · ${report.version}\n${report.capturedAt}", color = AccentCyan, fontSize = 14.sp, lineHeight = 19.sp)
                Text(report.pages[index].title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(report.pages[index].body, color = TextPrimary, fontSize = if (compact) 17.sp else 21.sp,
                    lineHeight = if (compact) 23.sp else 29.sp, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Keep navigation enabled at both ends so focus never disappears during paging.
                    TvPlayerActionButton("Previous", null) { if (index > 0) index-- }
                    TvPlayerActionButton("Next", null) { if (index < report.pages.lastIndex) index++ }
                    TvPlayerActionButton("Done", null, modifier = Modifier.focusRequester(done), onClick = onClose)
                }
            }
        }
        LaunchedEffect(Unit) { withFrameNanos { }; done.requestFocus() }
    }
}
