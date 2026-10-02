package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Sources : Screen("sources")
    data object Settings : Screen("settings")
    data object VideoDetail : Screen("detail/{videoId}") {
        fun createRoute(videoId: Long): String = "detail/$videoId"
    }
    data object SourceVideos : Screen("source/{sourceId}") {
        fun createRoute(sourceId: Long): String = "source/$sourceId"
    }
    data object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: Long): String = "player/$videoId"
    }
}
