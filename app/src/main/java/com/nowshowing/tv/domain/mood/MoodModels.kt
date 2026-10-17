package com.nowshowing.tv.domain.mood

data class MoodItem(
    val name: String,
    val emoji: String,
    val label: String,
    val description: String
)

data class MoodRule(
    val genres: List<String>,
    val keywords: List<String>
)

data class RankedMovie(
    val imdbID: String,
    val title: String,
    val poster: String,
    val year: String,
    val type: String,
    val matchScore: Int,
    val rating: Float
)
