package com.nowshowing.tv.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nowshowing.tv.ui.theme.AccentCyan
import com.nowshowing.tv.ui.theme.AccentGold
import com.nowshowing.tv.ui.theme.CardBorder
import com.nowshowing.tv.ui.theme.FocusBorder
import com.nowshowing.tv.ui.theme.FocusGlow
import com.nowshowing.tv.ui.theme.SurfaceDark
import com.nowshowing.tv.ui.theme.SurfaceVariant
import com.nowshowing.tv.ui.theme.TextMuted
import com.nowshowing.tv.ui.theme.TextPrimary

@Composable
fun TvFocusableCard(
    title: String,
    posterUrl: String,
    year: String = "",
    rating: String = "",
    badge: String? = null,
    progressFraction: Float? = null,
    width: Dp = 150.dp,
    height: Dp = 225.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusKey: String = "$title:$posterUrl"
) {
    val (returnRequester, rememberSelection) = rememberReturnFocusRequester(focusKey)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "card_scale"
    )

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .shadow(
                    elevation = if (isFocused) 14.dp else 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = if (isFocused) FocusGlow else Color.Black,
                    spotColor = if (isFocused) FocusGlow else Color.Black
                )
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(
                    BorderStroke(
                        width = if (isFocused) 2.5.dp else 1.dp,
                        color = if (isFocused) FocusBorder else CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .semantics { contentDescription = listOf(title, year, badge.orEmpty()).filter { it.isNotBlank() }.joinToString(", ") }
                .focusRequester(returnRequester)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { rememberSelection(); onClick() }
                )
        ) {
            // Stable artwork bounds, with no subcomposition per lazy-grid poster.
            var artworkFailed by remember(posterUrl) { mutableStateOf(false) }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (artworkFailed || posterUrl.isBlank() || posterUrl == "N/A") "Artwork unavailable" else "Loading artwork…",
                    color = TextMuted, fontSize = 14.sp)
                if (posterUrl.isNotBlank() && posterUrl != "N/A") {
                    coil.compose.AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(posterUrl).size(300, 450).crossfade(false).build(),
                        contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        onError = { artworkFailed = true })
                }
            }

            // Top badge (e.g. "Resume", "Movie", "Series")
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentCyan.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom gradient overlay with rating/year info if available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Progress bar if Continue Watching
            if (progressFraction != null && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = AccentCyan,
                    trackColor = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title text
        Text(
            text = title,
            color = if (isFocused) AccentCyan else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle row: Year & Rating
        if (year.isNotBlank() || rating.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (year.isNotBlank()) {
                    Text(
                        text = year,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                if (year.isNotBlank() && rating.isNotBlank()) {
                    Text(
                        text = " • ",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                if (rating.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = AccentGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = rating,
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Quiet placeholders avoid an animation clock for every poster on low-powered TVs. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
    )
}
