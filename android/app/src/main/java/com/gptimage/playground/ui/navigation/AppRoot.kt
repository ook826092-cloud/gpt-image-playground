package com.gptimage.playground.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.screens.album.AlbumScreen
import com.gptimage.playground.ui.screens.settings.SettingsScreen
import com.gptimage.playground.ui.screens.workbench.WorkbenchScreen

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val strings = LocalStrings.current

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppDestination.ALL.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon() else destination.unselectedIcon(),
                                contentDescription = null
                            )
                        },
                        label = { Text(destination.label(strings)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.WORKBENCH.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(AppDestination.WORKBENCH.route) {
                WorkbenchScreen(
                    onNavigateToSettings = { navController.navigate(AppDestination.SETTINGS.route) }
                )
            }
            composable(AppDestination.ALBUM.route) {
                AlbumScreen(
                    onNavigateToSettings = { navController.navigate(AppDestination.SETTINGS.route) }
                )
            }
            composable(AppDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}
