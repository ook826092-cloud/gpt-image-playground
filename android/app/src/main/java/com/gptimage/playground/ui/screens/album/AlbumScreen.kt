package com.gptimage.playground.ui.screens.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.ui.i18n.LocalStrings
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

    var activeTab by remember { mutableStateOf(0) }
    // 详情 BottomSheet 目标项
    var detailTarget by remember { mutableStateOf<HistoryItem?>(null) }
    // 全屏缩放目标项（path + contentDescription）
    var zoomTarget by remember { mutableStateOf<HistoryItem?>(null) }
    // 删除确认对话框目标项
    var deleteTarget by remember { mutableStateOf<HistoryItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(strings.navAlbum) },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = strings.settingsProviders)
                }
            }
        )
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(strings.albumImages) },
                icon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(strings.albumVideos) },
                icon = { Icon(Icons.Outlined.VideoLibrary, contentDescription = null) }
            )
        }
        when (activeTab) {
            0 -> {
                if (images.isEmpty()) {
                    AlbumEmptyState(message = strings.albumNoImages)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            1 -> AlbumEmptyState(message = strings.albumNoVideos)
        }
    }

    // 详情 BottomSheet
    detailTarget?.let { item ->
        ImageDetailSheet(
            item = item,
            albumViewModel = viewModel,
            onDismiss = { detailTarget = null },
            onOpenZoom = {
                zoomTarget = item
            },
            onSendToWorkbench = { historyItem, sendToEdit ->
                detailTarget = null
                onSendToWorkbench(historyItem, sendToEdit)
            },
            onDeleteRequest = { historyItem ->
                deleteTarget = historyItem
            }
        )
    }

    // 全屏缩放查看器
    zoomTarget?.let { item ->
        ZoomableImageDialog(
            imagePath = item.imagePath,
            contentDescription = item.prompt,
            onDismiss = { zoomTarget = null }
        )
    }

    // 删除确认对话框
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
                }) { Text(strings.commonDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.commonCancel) }
            }
        )
    }
}

@Composable
private fun AlbumImageCell(
    item: HistoryItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)),
        onClick = onClick
    ) {
        AsyncImage(
            model = File(item.imagePath),
            contentDescription = item.prompt,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AlbumEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
