package com.gptimage.playground.ui.screens.album

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.repository.CostUtils
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.i18n.Strings
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 详情 BottomSheet。展示图片元数据、prompt、费用估算，以及操作按钮组。
 *
 * 替代了原有的简陋 [AlbumItemActions]：
 * - 缩略图大图（点击进入全屏 ZoomableImageDialog）
 * - Prompt 卡片（带复制按钮）
 * - 元数据卡片（模型 / 尺寸 / 质量 / 格式 / 创建时间 / 耗时）
 * - 费用估算卡片（token 用量 + 总价 + 单项明细）
 * - 操作按钮组：保存到相册 / 分享 / 用作参考图 / 发送到编辑 / 删除
 *
 * @param onSendToWorkbench 用户点击「用作参考图」(sendToEdit=false) 或「发送到编辑」(sendToEdit=true)
 *                          时回调。AlbumScreen 应当导航到工作台并清空当前 BottomSheet。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailSheet(
    item: HistoryItem,
    albumViewModel: AlbumViewModel,
    onDismiss: () -> Unit,
    onOpenZoom: () -> Unit,
    onSendToWorkbench: (HistoryItem, Boolean) -> Unit,
    onDeleteRequest: (HistoryItem) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val saveStatus by albumViewModel.saveToGalleryStatus.collectAsState()
    val copyStatus by albumViewModel.copyPromptStatus.collectAsState()

    LaunchedEffect(saveStatus) {
        val raw = saveStatus ?: return@LaunchedEffect
        val msg = if (raw == "__ok__") strings.albumDetailSavedToGallery
            else "${strings.albumDetailSaveFailed}: $raw"
        snackbarHostState.showSnackbar(msg)
        albumViewModel.consumeSaveToGalleryStatus()
    }
    LaunchedEffect(copyStatus) {
        val raw = copyStatus ?: return@LaunchedEffect
        val msg = if (raw == "__ok__") strings.albumDetailPromptCopied
            else "${strings.albumDetailSaveFailed}: $raw"
        snackbarHostState.showSnackbar(msg)
        albumViewModel.consumeCopyPromptStatus()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DetailHeader(
                    item = item,
                    strings = strings,
                    onClickImage = onOpenZoom
                )
            }
            item { DetailPromptCard(item = item, strings = strings, onCopy = { albumViewModel.copyPrompt(item) }) }
            item { DetailMetadataCard(item = item, strings = strings) }
            item { DetailCostCard(item = item, strings = strings) }
            item {
                DetailActions(
                    item = item,
                    strings = strings,
                    onSaveToGallery = {
                        scope.launch { albumViewModel.saveToGallery(item) }
                    },
                    onShare = {
                        shareImage(context, File(item.imagePath))
                    },
                    onUseAsReference = {
                        onSendToWorkbench(item, false)
                    },
                    onSendToEdit = {
                        onSendToWorkbench(item, true)
                    },
                    onDelete = {
                        onDeleteRequest(item)
                    }
                )
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun DetailHeader(
    item: HistoryItem,
    strings: Strings,
    onClickImage: () -> Unit
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if ((item.width ?: 1) >= (item.height ?: 1)) 1.5f else 0.75f)
                .clip(RoundedCornerShape(16.dp)),
            onClick = onClickImage
        ) {
            AsyncImage(
                model = File(item.imagePath),
                contentDescription = item.prompt,
                modifier = Modifier.fillMaxWidth(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = strings.albumDetailZoomHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailPromptCard(
    item: HistoryItem,
    strings: Strings,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.albumDetailPrompt,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = strings.albumDetailCopyPrompt)
                }
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = item.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.heightIn(max = 200.dp)
            )
        }
    }
}

@Composable
private fun DetailMetadataCard(
    item: HistoryItem,
    strings: Strings
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val createdAt = remember { dateFmt.format(Date(item.createdAt)) }
    val duration = item.durationMs?.let { CostUtils.formatDuration(it) } ?: "—"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(strings.albumDetailMetadata, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.size(8.dp))
            MetadataRow(strings.albumDetailModel, item.modelLabel)
            MetadataRow(strings.albumDetailCreatedAt, createdAt)
            MetadataRow(strings.albumDetailDuration, duration)
            item.size?.let { MetadataRow(strings.albumDetailSize, it) }
            item.quality?.let { MetadataRow(strings.albumDetailQuality, it) }
            item.outputFormat?.let { MetadataRow(strings.albumDetailFormat, it) }
            val w = item.width
            val h = item.height
            if (w != null && h != null) {
                MetadataRow("WxH", "$w × $h")
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun DetailCostCard(
    item: HistoryItem,
    strings: Strings
) {
    val details = remember(item.id, item.outputTokens) { CostUtils.calculate(item) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = strings.albumDetailCost,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.size(8.dp))
            if (details == null) {
                Text(
                    text = strings.albumDetailCostUnavailable,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = CostUtils.formatPrecise(details.estimatedCostUsd),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = strings.albumDetailCostBreakdown,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
                MetadataRow(strings.albumDetailTokenTextIn, "${details.textInputTokens}")
                MetadataRow(strings.albumDetailTokenImageIn, "${details.imageInputTokens}")
                MetadataRow(strings.albumDetailTokenImageOut, "${details.imageOutputTokens}")
            }
        }
    }
}

@Composable
private fun DetailActions(
    item: HistoryItem,
    strings: Strings,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onUseAsReference: () -> Unit,
    onSendToEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val actions: List<ActionItem> = listOf(
        ActionItem(Icons.Outlined.PhotoLibrary, strings.albumDetailSaveToGallery, onSaveToGallery),
        ActionItem(Icons.Outlined.Share, strings.albumDetailShare, onShare),
        ActionItem(Icons.Outlined.Photo, strings.albumDetailUseAsReference, onUseAsReference),
        ActionItem(Icons.Outlined.PlayArrow, strings.albumDetailSendToEdit, onSendToEdit),
        ActionItem(Icons.Outlined.Delete, strings.albumDetailDelete, onDelete, isDanger = true)
    )
    actions.forEach { action ->
        ActionRow(
            icon = action.icon,
            label = action.label,
            onClick = action.onClick,
            isDanger = action.isDanger
        )
    }
}

private data class ActionItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val isDanger: Boolean = false
)

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun shareImage(context: android.content.Context, file: File) {
    if (!file.exists()) return
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
