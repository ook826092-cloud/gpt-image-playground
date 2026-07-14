package com.gptimage.playground.ui.screens.album

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.gptimage.playground.ui.theme.AppCorner
import com.gptimage.playground.ui.theme.AppElevation
import com.gptimage.playground.ui.theme.ContentPadding
import com.gptimage.playground.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 详情 BottomSheet。展示图片元数据、prompt、费用估算，以及操作按钮组。
 *
 * 视觉规则对齐新 iOS 风：白底圆角卡片 + 极浅 outline + 圆形操作 icon。
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
    val cs = MaterialTheme.colorScheme

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
        shape = AppCorner.sheet,
        modifier = Modifier.fillMaxWidth()
    ) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentPadding.screen)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                DetailHeader(item = item, strings = strings, onClickImage = onOpenZoom)
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
                    onShare = { shareImage(context, File(item.imagePath)) },
                    onUseAsReference = { onSendToWorkbench(item, false) },
                    onSendToEdit = { onSendToWorkbench(item, true) },
                    onDelete = { onDeleteRequest(item) }
                )
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
    val cs = MaterialTheme.colorScheme
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if ((item.width ?: 1) >= (item.height ?: 1)) 1.5f else 0.75f)
                .shadow(AppElevation.card, AppCorner.card)
                .clip(AppCorner.card)
                .background(cs.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onClickImage() }
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
            color = cs.onSurfaceVariant
        )
    }
}

/**
 * iOS 风白底圆角卡片容器：纯白 + 0.5dp outline + 极浅阴影。
 */
@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(AppElevation.card, AppCorner.card)
            .clip(AppCorner.card)
            .background(cs.surface)
            .border(0.5.dp, cs.outlineVariant, AppCorner.card)
    ) {
        content()
    }
}

@Composable
private fun DetailPromptCard(
    item: HistoryItem,
    strings: Strings,
    onCopy: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    DetailCard {
        Column(modifier = Modifier.padding(ContentPadding.card)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.albumDetailPrompt.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) { onCopy() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = strings.albumDetailCopyPrompt, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = item.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface,
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
    val cs = MaterialTheme.colorScheme
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val createdAt = remember { dateFmt.format(Date(item.createdAt)) }
    val duration = item.durationMs?.let { CostUtils.formatDuration(it) } ?: "—"

    DetailCard {
        Column(modifier = Modifier.padding(ContentPadding.card)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(AppCorner.small)
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.size(8.dp))
                Text(strings.albumDetailMetadata.uppercase(), style = MaterialTheme.typography.labelMedium, color = cs.primary, fontWeight = FontWeight.SemiBold)
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
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurface,
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
    val cs = MaterialTheme.colorScheme
    val details = remember(item.id, item.outputTokens) { CostUtils.calculate(item) }
    DetailCard {
        Column(modifier = Modifier.padding(ContentPadding.card)) {
            Text(
                text = strings.albumDetailCost.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = cs.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.size(8.dp))
            if (details == null) {
                Text(
                    text = strings.albumDetailCostUnavailable,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            } else {
                Text(
                    text = CostUtils.formatPrecise(details.estimatedCostUsd),
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = strings.albumDetailCostBreakdown,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant
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
    val cs = MaterialTheme.colorScheme
    val actions: List<ActionItem> = listOf(
        ActionItem(Icons.Outlined.PhotoLibrary, strings.albumDetailSaveToGallery, onSaveToGallery),
        ActionItem(Icons.Outlined.Share, strings.albumDetailShare, onShare),
        ActionItem(Icons.Outlined.Photo, strings.albumDetailUseAsReference, onUseAsReference),
        ActionItem(Icons.Outlined.PlayArrow, strings.albumDetailSendToEdit, onSendToEdit),
        ActionItem(Icons.Outlined.Delete, strings.albumDetailDelete, onDelete, isDanger = true)
    )
    DetailCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            actions.forEachIndexed { idx, action ->
                ActionRow(
                    icon = action.icon,
                    label = action.label,
                    onClick = action.onClick,
                    isDanger = action.isDanger
                )
                if (idx != actions.lastIndex) {
                    HorizontalDivider(
                        color = cs.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = ContentPadding.card)
                    )
                }
            }
        }
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
    val cs = MaterialTheme.colorScheme
    val tint = if (isDanger) cs.error else cs.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = ContentPadding.card, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(AppCorner.small)
                .background(if (isDanger) cs.errorContainer else cs.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDanger) cs.error else cs.onSurface
        )
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
