package com.gptimage.playground.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level navigation destinations, mirrors the web app's primary tabs. */
enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Workbench(
        route = "workbench",
        labelRes = com.gptimage.playground.R.string.nav_workbench,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    History(
        route = "history",
        labelRes = com.gptimage.playground.R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    Settings(
        route = "settings",
        labelRes = com.gptimage.playground.R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    );

    companion object {
        val Start = Workbench
    }
}
