package com.nowshowing.tv.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.nowshowing.tv.ui.theme.AccentCyan
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.ui.theme.TextPrimary

@Composable
fun TvContentRow(
    title: String,
    items: List<OmdbTitleDetail>,
    onItemClick: (OmdbTitleDetail) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    if (items.isEmpty() && !isLoading) return

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(com.nowshowing.tv.ui.theme.AccentCyan)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (items.isEmpty() && isLoading) {
                items(6) { ShimmerBox(Modifier.width(150.dp).height(270.dp)) }
            }
            items(items, key = { it.imdbID }) { movie ->
                TvFocusableCard(
                    focusKey = "$title:${movie.imdbID}",
                    title = movie.title,
                    posterUrl = if (movie.poster != "N/A") movie.poster ?: "" else "",
                    year = movie.year,
                    rating = if (movie.imdbRating != "N/A") movie.imdbRating ?: "" else "",
                    onClick = { onItemClick(movie) }
                )
            }
        }
    }
}
