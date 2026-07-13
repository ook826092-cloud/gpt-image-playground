package com.gptimage.playground.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gptimage.playground.di.AppContainer
import com.gptimage.playground.di.AppViewModelFactory
import com.gptimage.playground.ui.history.HistoryScreen
import com.gptimage.playground.ui.history.HistoryViewModel
import com.gptimage.playground.ui.settings.SettingsScreen
import com.gptimage.playground.ui.settings.SettingsViewModel
import com.gptimage.playground.ui.workbench.WorkbenchScreen
import com.gptimage.playground.ui.workbench.WorkbenchViewModel

/**
 * Root composable: a Scaffold with a bottom navigation bar and a NavHost that
 * hosts the three top-level screens (Workbench, History, Settings).
 */
@Composable
fun PlaygroundApp(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember(container) { AppViewModelFactory(container) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopDestination.values().forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.Start.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopDestination.Workbench.route) {
                val vm: WorkbenchViewModel = viewModel(factory = factory)
                WorkbenchScreen(vm)
            }
            composable(TopDestination.History.route) {
                val vm: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(vm)
            }
            composable(TopDestination.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(vm)
            }
        }
    }
}
