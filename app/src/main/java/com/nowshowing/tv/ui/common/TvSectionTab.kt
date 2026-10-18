package com.nowshowing.tv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.ui.theme.*

/** One focus target per tab; lazy parents keep every destination reachable on narrow TVs. */
@Composable
fun TvSectionTab(label: String, isSelected: Boolean, modifier: Modifier = Modifier,
    icon: ImageVector? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val foreground = if (focused) Color.Black else if (isSelected) AccentCyan else TextSecondary
    Row(modifier.heightIn(min = 48.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(if (focused) FocusBorder else if (isSelected) SurfaceVariant else SurfaceDark)
        .border(if (focused) 2.dp else 1.dp,
            if (focused) Color.White else if (isSelected) AccentCyan.copy(alpha = 0.6f) else CardBorder,
            RoundedCornerShape(10.dp))
        .semantics { selected = isSelected }
        .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(it, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(label, color = foreground, fontSize = 15.sp,
            fontWeight = if (focused || isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}
