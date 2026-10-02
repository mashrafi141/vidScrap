package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.detail.VideoDetailScreen
import com.example.ui.screens.detail.VideoDetailViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.player.PlayerViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.sources.SourcesScreen
import com.example.ui.screens.sources.SourcesViewModel
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassBorderStroke
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            title = "Library",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = Screen.Sources.route,
            title = "Sources",
            selectedIcon = Icons.Filled.VideoLibrary,
            unselectedIcon = Icons.Outlined.VideoLibrary
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Sources.route,
        Screen.Settings.route
    )

    Scaffold(
        containerColor = ObsidianBg,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    NavigationBar(
                        containerColor = DarkSurface.copy(alpha = 0.95f),
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, GlassBorderStroke, RoundedCornerShape(24.dp))
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title) },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = TextPrimary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = AccentPrimary.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    },
                    onSourceClick = { sourceId ->
                        navController.navigate(Screen.SourceVideos.createRoute(sourceId))
                    }
                )
            }

            composable(Screen.Sources.route) {
                val sourcesViewModel: SourcesViewModel = viewModel()
                SourcesScreen(
                    viewModel = sourcesViewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onSourceClick = { sourceId ->
                        navController.navigate(Screen.SourceVideos.createRoute(sourceId))
                    },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            composable(
                route = Screen.SourceVideos.route,
                arguments = listOf(navArgument("sourceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sourceId = backStackEntry.arguments?.getLong("sourceId") ?: 0L
                val sourceVideosViewModel: com.example.ui.screens.source.SourceVideosViewModel = viewModel()
                com.example.ui.screens.source.SourceVideosScreen(
                    sourceId = sourceId,
                    viewModel = sourceVideosViewModel,
                    onBackClick = { navController.popBackStack() },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    }
                )
            }

            composable(
                route = Screen.VideoDetail.route,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                val detailViewModel: VideoDetailViewModel = viewModel()
                VideoDetailScreen(
                    videoId = videoId,
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onPlayClick = { id ->
                        navController.navigate(Screen.Player.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                val playerViewModel: PlayerViewModel = viewModel()
                PlayerScreen(
                    videoId = videoId,
                    viewModel = playerViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
