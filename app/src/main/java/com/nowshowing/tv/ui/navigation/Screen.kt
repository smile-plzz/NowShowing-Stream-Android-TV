package com.nowshowing.tv.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Movies : Screen("movies")
    object TV : Screen("tv")
    object Mood : Screen("mood")
    object Search : Screen("search")
    object Watchlist : Screen("watchlist")
    object Settings : Screen("settings")

    object Details : Screen("details/{imdbId}/{type}") {
        fun createRoute(imdbId: String, type: String = "movie") = "details/$imdbId/$type"
    }

    object Player : Screen("player/{imdbId}/{season}/{episode}/{type}") {
        fun createRoute(imdbId: String, season: Int = 1, episode: Int = 1, type: String = "movie") =
            "player/$imdbId/$season/$episode/$type"
    }
}
