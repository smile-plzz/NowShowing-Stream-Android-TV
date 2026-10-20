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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
    var showYears by remember { mutableStateOf(false) }
    var restoreYearFocus by remember { mutableStateOf(false) }
    val yearFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    if (showYears) {
        YearPicker(uiState.selectedYear, onDismiss = { restoreYearFocus = true; showYears = false }, onApply = {
            viewModel.selectYear(it); restoreYearFocus = true; showYears = false
        })
    }
    LaunchedEffect(showYears) {
        if (!showYears && restoreYearFocus) {
            yearFocus.requestFocus(); restoreYearFocus = false
        }
    }


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
                Column(Modifier.weight(1f)) {
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
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Surprise Me Pill Button
                Box(Modifier.focusRequester(yearFocus)) {
                    com.nowshowing.tv.ui.player.TvPlayerActionButton("Year: ${uiState.selectedYear ?: "All Years"}", null) { showYears = true }
                }
                Spacer(Modifier.width(12.dp))
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
            if (uiState.isLoading && uiState.recommendations.isEmpty()) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (uiState.selectedMood == null) "Choose a mood to discover something new." else uiState.statusMessage,
                            color = TextMuted, fontSize = 17.sp)
                        if (uiState.selectedMood != null) {
                            Spacer(Modifier.height(12.dp))
                            com.nowshowing.tv.ui.player.TvPlayerActionButton("Try this mood again", null) { viewModel.retry() }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
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
                            badge = null,
                            onClick = { onTitleClick(movie.imdbID, movie.type) },
                            focusKey = movie.imdbID
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
            text = mood.label,
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

@Composable
private fun YearPicker(selected: Int?, onDismiss: () -> Unit, onApply: (Int?) -> Unit) {
    val years = remember { listOf<Int?>(null) + (java.time.Year.now().value downTo 1888).toList() }
    val index = years.indexOf(selected).coerceAtLeast(0)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = index)
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.width(270.dp).background(SurfaceDark, RoundedCornerShape(16.dp)).padding(20.dp)) {
            Text("Release year", color = TextPrimary, fontSize = 20.sp)
            Text("OK selects · Back cancels", color = TextMuted, fontSize = 12.sp)
            androidx.compose.foundation.lazy.LazyColumn(state = list, modifier = Modifier.height(260.dp)) {
                items(years.size) { row ->
                    Box(if (row == index) Modifier.focusRequester(focus) else Modifier) {
                        com.nowshowing.tv.ui.player.TvPlayerActionButton(years[row]?.toString() ?: "All Years", null) {
                            onApply(years[row])
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
