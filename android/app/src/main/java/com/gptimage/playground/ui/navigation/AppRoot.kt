package com.gptimage.playground.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.screens.album.AlbumScreen
import com.gptimage.playground.ui.screens.settings.SettingsScreen
import com.gptimage.playground.ui.screens.workbench.WorkbenchScreen
import com.gptimage.playground.ui.theme.AppHeight

/**
 * 应用根容器。
 *
 * 与传统 Scaffold + NavigationBar 不同，这里采用「浮动底部导航」：
 * 导航条悬浮在内容之上，与内容容器分离，
 * 让内容可以铺满整屏，仅底部留出 tab bar 高度 + 安全区。
 *
 * 这样布局的好处：
 * 1. iOS 视觉感更强（圆角 + 阴影 + 半透明）
 * 2. 内容区可单独控制滚动 padding，不被 navbar 挤压
 */
@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as PlaygroundApp
    val pendingReferenceBus = app.locator.pendingReferenceBus

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.WORKBENCH.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppDestination.WORKBENCH.route) {
                WorkbenchScreen(
                    onNavigateToSettings = { navController.navigate(AppDestination.SETTINGS.route) },
                    pendingReferenceBus = pendingReferenceBus
                )
            }
            composable(AppDestination.ALBUM.route) {
                AlbumScreen(
                    onNavigateToSettings = { navController.navigate(AppDestination.SETTINGS.route) },
                    onSendToWorkbench = { item, sendToEdit ->
                        pendingReferenceBus.request(item, sendToEdit)
                        navController.navigate(AppDestination.WORKBENCH.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(AppDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }

        // iOS 风浮动底部导航：浮在内容之上，不挤压内容
        FloatingTabBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            currentDestination = currentDestination,
            onSelect = { destination ->
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
private fun FloatingTabBar(
    modifier: Modifier = Modifier,
    currentDestination: androidx.navigation.NavDestination?,
    onSelect: (AppDestination) -> Unit
) {
    val strings = LocalStrings.current
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = cs.surface.copy(alpha = 0.95f),
            shadowElevation = 12.dp,
            border = BorderStroke(0.5.dp, cs.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppHeight.tabBar)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppDestination.ALL.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    TabItem(
                        destination = destination,
                        label = destination.label(strings),
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    destination: AppDestination,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val icon: ImageVector = if (selected) destination.selectedIcon() else destination.unselectedIcon()
    val tint = if (selected) cs.primary else cs.onSurfaceVariant

    Column(
        modifier = modifier
            .height(AppHeight.tabBar)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
