package com.gptimage.playground.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.gptimage.playground.ui.i18n.Strings

enum class AppDestination(val route: String) {
    WORKBENCH("workbench"),
    ALBUM("album"),
    SETTINGS("settings");

    fun label(strings: Strings): String = when (this) {
        WORKBENCH -> strings.navWorkbench
        ALBUM -> strings.navAlbum
        SETTINGS -> strings.navSettings
    }

    fun selectedIcon(): ImageVector = when (this) {
        WORKBENCH -> Icons.Rounded.AutoAwesome
        ALBUM -> Icons.Rounded.PhotoLibrary
        SETTINGS -> Icons.Rounded.Settings
    }

    fun unselectedIcon(): ImageVector = when (this) {
        WORKBENCH -> Icons.Outlined.AutoAwesome
        ALBUM -> Icons.Outlined.PhotoLibrary
        SETTINGS -> Icons.Outlined.Settings
    }

    companion object { val ALL: List<AppDestination> = listOf(WORKBENCH, ALBUM, SETTINGS) }
}
