package com.gptimage.playground.ui.screens.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.theme.AppCorner
import com.gptimage.playground.ui.theme.AppElevation
import com.gptimage.playground.ui.theme.AppExtra
import com.gptimage.playground.ui.theme.ContentPadding
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onNavigateToSettings: () -> Unit,
    onSendToWorkbench: (HistoryItem, Boolean) -> Unit,
    viewModel: AlbumViewModel = viewModel(
        factory = AlbumViewModelFactory((LocalContext.current.applicationContext as PlaygroundApp).locator)
    )
) {
    val strings = LocalStrings.current
    val images by viewModel.images.collectAsState()
    val cs = MaterialTheme.colorScheme

    var activeTab by remember { mutableStateOf(0) }
    var detailTarget by remember { mutableStateOf<HistoryItem?>(null) }
    var zoomTarget by remember { mutableStateOf<HistoryItem?>(null) }
    var deleteTarget by remember { mutableStateOf<HistoryItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // iOS 风：自定义顶栏（不用 TopAppBar，避免 M3 默认阴影）
            AlbumTopBar(
                title = strings.navAlbum,
                count = images.size,
                onOpenSettings = onNavigateToSettings
            )

            // iOS 风分段控件
            SegmentedTabBar(
                tabs = listOf(
                    SegmentedTab(strings.albumImages, Icons.Outlined.PhotoLibrary),
                    SegmentedTab(strings.albumVideos, Icons.Outlined.VideoLibrary)
                ),
                selectedIndex = activeTab,
                onSelect = { activeTab = it }
            )

            when (activeTab) {
                0 -> {
                    if (images.isEmpty()) {
                        AlbumEmptyState(
                            message = strings.albumNoImages,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(
                                horizontal = ContentPadding.screen,
                                vertical = 12.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(images, key = { it.id }) { item ->
                                AlbumImageCell(
                                    item = item,
                                    onClick = { detailTarget = item }
                                )
                            }
                        }
                    }
                }
                1 -> AlbumEmptyState(
                    message = strings.albumNoVideos,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
            // 浮动 tab bar 占位（让最后一行不被遮）
            Spacer(Modifier.navigationBarsPadding().height(72.dp))
        }
    }

    detailTarget?.let { item ->
        ImageDetailSheet(
            item = item,
            albumViewModel = viewModel,
            onDismiss = { detailTarget = null },
            onOpenZoom = { zoomTarget = item },
            onSendToWorkbench = { historyItem, sendToEdit ->
                detailTarget = null
                onSendToWorkbench(historyItem, sendToEdit)
            },
            onDeleteRequest = { historyItem ->
                deleteTarget = historyItem
            }
        )
    }

    zoomTarget?.let { item ->
        ZoomableImageDialog(
            imagePath = item.imagePath,
            contentDescription = item.prompt,
            onDismiss = { zoomTarget = null }
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.commonDelete) },
            text = { Text(strings.albumDeleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item)
                    deleteTarget = null
                    detailTarget = null
                }) { Text(strings.commonDelete, color = cs.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.commonCancel) }
            }
        )
    }
}

@Composable
private fun AlbumTopBar(
    title: String,
    count: Int,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (count > 0) {
                Text(
                    strings.albumCountFormat(count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TopBarIconButton(
            icon = Icons.Outlined.Settings,
            onClick = onOpenSettings,
            contentDescription = strings.settingsProviders
        )
    }
}

@Composable
private fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentDescription: String?
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}

private data class SegmentedTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * iOS 风 Segmented Control：浅灰容器 + 白色滑块 + 主题色文字
 */
@Composable
private fun SegmentedTabBar(
    tabs: List<SegmentedTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding.screen, vertical = 8.dp)
            .clip(AppCorner.card)
            .background(cs.surfaceVariant.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tabs.forEachIndexed { i, tab ->
            val selected = i == selectedIndex
            val bg = if (selected) cs.surface else Color.Transparent
            val fg = if (selected) cs.onSurface else cs.onSurfaceVariant
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(AppCorner.small)
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onSelect(i) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(tab.icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun AlbumImageCell(
    item: HistoryItem,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(AppElevation.card, AppCorner.card)
            .clip(AppCorner.card)
            .background(cs.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
    ) {
        AsyncImage(
            model = File(item.imagePath),
            contentDescription = item.prompt,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}

@Composable
private fun AlbumEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    val extra = AppExtra.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(AppCorner.pill)
                    .background(
                        Brush.horizontalGradient(listOf(extra.gradientStart, extra.gradientEnd)),
                        shape = AppCorner.pill
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.size(20.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
