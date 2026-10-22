package com.nowshowing.tv.data.model

// Gson can bypass Kotlin constructors and populate non-null declarations with null.
@Suppress("SENSELESS_COMPARISON")
fun OmdbTitleDetail.isUsable(): Boolean = response == "True" &&
    imdbID != null && imdbID.matches(Regex("tt[0-9]{7,10}")) &&
    title != null && title.isNotBlank() && year != null && type in setOf("movie", "series", "episode")

