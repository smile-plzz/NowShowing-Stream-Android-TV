package com.nowshowing.tv.ui.mood

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.domain.mood.MoodItem
import com.nowshowing.tv.domain.mood.MoodRecommendationEngine
import com.nowshowing.tv.ui.common.TvFocusableCard
import com.nowshowing.tv.ui.common.TvTopBar
import com.nowshowing.tv.ui.theme.AccentCyan
import com.nowshowing.tv.ui.theme.AccentGold
import com.nowshowing.tv.ui.theme.BgDark
import com.nowshowing.tv.ui.theme.FocusBorder
import com.nowshowing.tv.ui.theme.SurfaceDark
import com.nowshowing.tv.ui.theme.SurfaceVariant
import com.nowshowing.tv.ui.theme.TextMuted
import com.nowshowing.tv.ui.theme.TextPrimary

@Composable
fun MoodScreen(
    viewModel: MoodViewModel,
    onNavigate: (String) -> Unit,
    onTitleClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        TvTopBar(currentRoute = "mood", onNavigate = onNavigate)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) {
            // Header Row: Title & "Surprise Me" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "How are you feeling?",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.statusMessage,
                        color = TextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Surprise Me Pill Button
                SurpriseMeButton(onClick = { viewModel.surpriseMe() })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Mood Palette Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MoodRecommendationEngine.MOODS, key = { it.name }) { mood ->
                    MoodPill(
                        mood = mood,
                        isSelected = uiState.selectedMood?.name == mood.name,
                        onClick = { viewModel.selectMood(mood) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Grid
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else if (uiState.recommendations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pick a mood above to generate precision recommendations.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.recommendations, key = { it.imdbID }) { movie ->
                        TvFocusableCard(
                            title = movie.title,
                            posterUrl = movie.poster,
                            year = movie.year,
                            rating = if (movie.rating > 0f) movie.rating.toString() else "",
                            badge = if (movie.matchScore > 1) "${movie.matchScore}x Match" else null,
                            onClick = { onTitleClick(movie.imdbID, movie.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodPill(
    mood: MoodItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> FocusBorder
        isSelected -> SurfaceVariant
        else -> SurfaceDark
    }

    val textColor = when {
        isFocused -> Color.Black
        isSelected -> AccentCyan
        else -> TextPrimary
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = if (isFocused) Color.White else if (isSelected) AccentCyan else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mood.emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = mood.name,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SurpriseMeButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) FocusBorder else AccentGold)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Surprise Me",
            tint = Color.Black,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Surprise Me",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
