package com.nowshowing.tv.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nowshowing.tv.ui.browse.BrowseScreen
import com.nowshowing.tv.ui.browse.BrowseViewModel
import com.nowshowing.tv.ui.details.DetailsScreen
import com.nowshowing.tv.ui.details.DetailsViewModel
import com.nowshowing.tv.ui.home.HomeScreen
import com.nowshowing.tv.ui.home.HomeViewModel
import com.nowshowing.tv.ui.library.WatchlistScreen
import com.nowshowing.tv.ui.library.WatchlistViewModel
import com.nowshowing.tv.ui.mood.MoodScreen
import com.nowshowing.tv.ui.mood.MoodViewModel
import com.nowshowing.tv.ui.navigation.Screen
import com.nowshowing.tv.ui.player.PlayerScreen
import com.nowshowing.tv.ui.player.PlayerViewModel
import com.nowshowing.tv.ui.search.SearchScreen
import com.nowshowing.tv.ui.search.SearchViewModel
import com.nowshowing.tv.ui.settings.SettingsScreen
import com.nowshowing.tv.ui.settings.SettingsViewModel
import com.nowshowing.tv.ui.theme.NowShowingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep TV screen awake during viewing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            NowShowingTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    // Home Screen
                    composable(Screen.Home.route) {
                        val homeVm: HomeViewModel = viewModel()
                        HomeScreen(
                            viewModel = homeVm,
                            onNavigate = { route ->
                                if (route != Screen.Home.route) {
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onTitleClick = { imdbId, type ->
                                navController.navigate(Screen.Details.createRoute(imdbId, type))
                            },
                            onPlayClick = { imdbId, season, episode, type ->
                                navController.navigate(Screen.Player.createRoute(imdbId, season, episode, type))
                            }
                        )
                    }

                    // Movies Browse
                    composable(Screen.Movies.route) {
                        val browseVm: BrowseViewModel = viewModel()
                        BrowseScreen(
                            kind = "movies",
                            viewModel = browseVm,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            },
                            onTitleClick = { imdbId, type ->
                                navController.navigate(Screen.Details.createRoute(imdbId, type))
                            }
                        )
                    }

                    // TV Browse
                    composable(Screen.TV.route) {
                        val browseVm: BrowseViewModel = viewModel()
                        BrowseScreen(
                            kind = "tv",
                            viewModel = browseVm,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            },
                            onTitleClick = { imdbId, type ->
                                navController.navigate(Screen.Details.createRoute(imdbId, type))
                            }
                        )
                    }

                    // Mood Screen
                    composable(Screen.Mood.route) {
                        val moodVm: MoodViewModel = viewModel()
                        MoodScreen(
                            viewModel = moodVm,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            },
                            onTitleClick = { imdbId, type ->
                                navController.navigate(Screen.Details.createRoute(imdbId, type))
                            }
                        )
                    }

                    // Search Screen
                    composable(Screen.Search.route) {
                        val searchVm: SearchViewModel = viewModel()
                        SearchScreen(
                            viewModel = searchVm,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            },
                            onTitleClick = { imdbId, type ->
                                navController.navigate(Screen.Details.createRoute(imdbId, type))
                            }
                        )
                    }

                    // Watchlist Screen
                    composable(Screen.Watchlist.route) {
                        val watchlistVm: WatchlistViewModel = viewModel()
                        WatchlistScreen(
                            viewModel = watchlistVm,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            },
                            onTitleClick = { imdbId, type ->
                                navController.navigate(Screen.Details.createRoute(imdbId, type))
                            }
                        )
                    }

                    // Privacy & Content Blocking Settings Screen
                    composable(Screen.Settings.route) {
                        val settingsVm: SettingsViewModel = viewModel()
                        SettingsScreen(
                            viewModel = settingsVm,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // Title Details Screen
                    composable(
                        route = Screen.Details.route,
                        arguments = listOf(
                            navArgument("imdbId") { type = NavType.StringType },
                            navArgument("type") { type = NavType.StringType; defaultValue = "movie" }
                        )
                    ) { backStackEntry ->
                        val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
                        val type = backStackEntry.arguments?.getString("type") ?: "movie"
                        val detailsVm: DetailsViewModel = viewModel()

                        DetailsScreen(
                            imdbId = imdbId,
                            type = type,
                            viewModel = detailsVm,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            },
                            onPlay = { id, season, ep, t ->
                                navController.navigate(Screen.Player.createRoute(id, season, ep, t))
                            },
                            onSelectRecommended = { recId, recType ->
                                navController.navigate(Screen.Details.createRoute(recId, recType))
                            }
                        )
                    }

                    // Player Screen
                    composable(
                        route = Screen.Player.route,
                        arguments = listOf(
                            navArgument("imdbId") { type = NavType.StringType },
                            navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                            navArgument("episode") { type = NavType.IntType; defaultValue = 1 },
                            navArgument("type") { type = NavType.StringType; defaultValue = "movie" }
                        )
                    ) { backStackEntry ->
                        val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
                        val season = backStackEntry.arguments?.getInt("season") ?: 1
                        val episode = backStackEntry.arguments?.getInt("episode") ?: 1
                        val type = backStackEntry.arguments?.getString("type") ?: "movie"
                        val playerVm: PlayerViewModel = viewModel()

                        PlayerScreen(
                            imdbId = imdbId,
                            season = season,
                            episode = episode,
                            type = type,
                            viewModel = playerVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Release screen stay-awake flag when backgrounded/standby to conserve power and allow TV sleep
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        // Restore screen keep-on flag when in foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_MEDIA_STOP -> {
                super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

