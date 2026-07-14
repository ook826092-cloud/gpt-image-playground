package com.gptimage.playground.ui.screens.workbench

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.model.ImageBackgrounds
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageModerations
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ImageQualities
import com.gptimage.playground.ui.components.AppButton
import com.gptimage.playground.ui.components.AppButtonStyle
import com.gptimage.playground.ui.components.AppChip
import com.gptimage.playground.ui.components.AppIconButton
import com.gptimage.playground.ui.components.AppSectionHeader
import com.gptimage.playground.ui.components.AppTextField
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.i18n.Strings
import com.gptimage.playground.ui.theme.AppCorner
import com.gptimage.playground.ui.theme.AppExtra
import com.gptimage.playground.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkbenchScreen(
    onNavigateToSettings: () -> Unit,
    pendingReferenceBus: PendingReferenceBus? = null,
    viewModel: WorkbenchViewModel = viewModel(
        factory = WorkbenchViewModelFactory((LocalContext.current.applicationContext as PlaygroundApp).locator)
    )
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val context = LocalContext.current
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showMaskSheet by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 订阅「相册 → 用作参考图 / 发送到编辑」的跨页面传输
    LaunchedEffect(pendingReferenceBus) {
        val bus = pendingReferenceBus ?: return@LaunchedEffect
        bus.pending.collect { pending ->
            if (pending != null) {
                if (pending.sendToEdit) {
                    viewModel.sendToEdit(pending.item)
                } else {
                    viewModel.useHistoryItemAsReference(pending.item)
                }
                bus.consume()
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        uris.forEach { uri ->
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "reference"
            val mimeType = context.contentResolver.getType(uri) ?: "image/png"
            viewModel.addReference(uri, name, mimeType)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            WorkbenchTopBar(
                onOpenTemplates = { showTemplatesDialog = true },
                onOpenSettings = onNavigateToSettings,
                onClearAll = { showClearConfirm = true },
                canClear = state.turns.isNotEmpty()
            )

            // 聊天列表区
            val listState = rememberLazyListState()
            if (state.turns.isEmpty()) {
                EmptyChatState(modifier = Modifier.weight(1f).fillMaxWidth())
            } else {
                // 自动滚到底部，让最新气泡可见
                LaunchedEffect(state.turns.size, state.turns.lastOrNull()?.status) {
                    if (state.turns.isNotEmpty()) {
                        listState.animateScrollToItem(state.turns.lastIndex)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.turns, key = { it.id }) { turn ->
                        ChatTurnItem(
                            turn = turn,
                            isLatest = turn.id == state.turns.lastOrNull()?.id,
                            onRetry = { viewModel.retryTurn(turn) },
                            onCancel = viewModel::cancelGenerate,
                            onShare = { item -> shareImage(context, File(item.imagePath)) },
                            onUseAsReference = { item -> viewModel.useHistoryItemAsReference(item) },
                            onSendToEdit = { item -> viewModel.sendToEdit(item) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // 输入区
            InputBar(
                prompt = state.prompt,
                onPromptChange = viewModel::updatePrompt,
                onSend = viewModel::generate,
                onCancel = viewModel::cancelGenerate,
                isGenerating = state.isGenerating,
                isStreaming = state.isStreaming,
                canSend = state.prompt.isNotBlank() && state.providerConfigured && !state.isGenerating,
                model = state.model,
                references = state.referenceImages,
                onAddReference = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemoveReference = viewModel::removeReferenceAt,
                onClearReferences = viewModel::clearReferences,
                onOpenAdvanced = { showAdvancedSheet = true },
                onOpenMaskEditor = { viewModel.setMaskEditorVisible(true); showMaskSheet = true },
                canEditMask = state.model?.supportsMask == true && state.referenceImages.isNotEmpty(),
                providerConfigured = state.providerConfigured,
                onOpenSettings = onNavigateToSettings
            )
        }
    }

    if (showAdvancedSheet) {
        AdvancedParamsSheet(
            state = state,
            onDismiss = { showAdvancedSheet = false },
            onSelectModel = viewModel::selectModel,
            onCountChange = viewModel::setCount,
            onSizeChange = viewModel::setSize,
            onQualityChange = viewModel::setQuality,
            onFormatChange = viewModel::setOutputFormat,
            onBackgroundChange = viewModel::setBackground,
            onModerationChange = viewModel::setModeration,
            onStreamingToggle = viewModel::setStreamingEnabled
        )
    }

    if (showMaskSheet && state.maskEditorVisible) {
        MaskEditorSheet(
            state = state,
            onDismiss = {
                viewModel.setMaskEditorVisible(false)
                showMaskSheet = false
            },
            onBrushSizeChange = viewModel::setMaskBrushSize,
            onAddPoint = viewModel::addMaskPoint,
            onAddLine = viewModel::addMaskLine,
            onClearMask = viewModel::clearMask,
            onUndoLastPoint = viewModel::undoLastMaskPoint,
            onSaveMask = viewModel::saveMask
        )
    } else if (showMaskSheet && !state.maskEditorVisible) {
        // 异步加载源图完成后 maskEditorVisible 才变 true，但 sheet 已 dismiss 时不应再打开
        showMaskSheet = false
    }

    if (showTemplatesDialog) {
        PromptTemplatesDialog(
            currentPrompt = state.prompt,
            onApplyTemplate = { prompt ->
                viewModel.updatePrompt(prompt)
                showTemplatesDialog = false
            },
            onDismiss = { showTemplatesDialog = false }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(strings.chatClearAllConfirm) },
            text = { Text(strings.chatClearAllConfirmBody) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearTurns()
                    showClearConfirm = false
                }) { Text(strings.commonConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(strings.commonCancel) }
            }
        )
    }
}

// =====================================================================
// Top bar
// =====================================================================

@Composable
private fun WorkbenchTopBar(
    onOpenTemplates: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearAll: () -> Unit,
    canClear: Boolean
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            strings.appName,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (canClear) {
            IconButton(onClick = onClearAll) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = strings.chatClearAll
                )
            }
        }
        IconButton(onClick = onOpenTemplates) {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = strings.templatesOpen
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = strings.settingsProviders
            )
        }
    }
}

// =====================================================================
// Empty state
// =====================================================================

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val extra = AppExtra.current
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(AppCorner.pill)
                .background(
                    Brush.horizontalGradient(listOf(extra.gradientStart, extra.gradientEnd)),
                    shape = AppCorner.pill
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            strings.chatEmptyTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            strings.chatEmptySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// =====================================================================
// Chat turn item — 一个回合（用户气泡 + 助手气泡）
// =====================================================================

@Composable
private fun ChatTurnItem(
    turn: ChatTurn,
    isLatest: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onShare: (HistoryItem) -> Unit,
    onUseAsReference: (HistoryItem) -> Unit,
    onSendToEdit: (HistoryItem) -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 用户气泡（右对齐）
        UserBubble(prompt = turn.prompt, referenceUris = turn.referenceImageUris, modelLabel = turn.modelLabel)

        // 助手气泡（左对齐）
        AssistantBubble(
            turn = turn,
            isLatest = isLatest,
            onRetry = onRetry,
            onCancel = onCancel,
            onShare = onShare,
            onUseAsReference = onUseAsReference,
            onSendToEdit = onSendToEdit
        )
    }
}

@Composable
private fun UserBubble(
    prompt: String,
    referenceUris: List<android.net.Uri>,
    modelLabel: String
) {
    val extra = AppExtra.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // 参考图缩略图行
        if (referenceUris.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                referenceUris.forEach { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(AppCorner.small)
                    )
                }
            }
        }
        Surface(
            color = extra.userBubble,
            shape = bubbleShape(alignedRight = true),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extra.onUserBubble
                )
                if (modelLabel.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        modelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = extra.onUserBubble.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantBubble(
    turn: ChatTurn,
    isLatest: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onShare: (HistoryItem) -> Unit,
    onUseAsReference: (HistoryItem) -> Unit,
    onSendToEdit: (HistoryItem) -> Unit
) {
    val strings = LocalStrings.current
    val extra = AppExtra.current

    Surface(
        color = extra.systemBubble,
        shape = bubbleShape(alignedRight = false),
        modifier = Modifier.widthIn(max = 320.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (turn.status) {
                TurnStatus.GENERATING -> AssistantGeneratingContent(turn, onCancel)
                TurnStatus.SUCCESS -> AssistantResultContent(
                    turn = turn,
                    onShare = onShare,
                    onUseAsReference = onUseAsReference,
                    onSendToEdit = onSendToEdit
                )
                TurnStatus.ERROR -> AssistantErrorContent(turn, onRetry)
                TurnStatus.CANCELED -> Text(
                    strings.chatTurnCanceled,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AssistantGeneratingContent(turn: ChatTurn, onCancel: () -> Unit) {
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (turn.streamingStartedAt > 0L) strings.chatStreaming else strings.chatGenerating,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (turn.streamingPartialIndex > 0) {
            Text(
                strings.workbenchStreamingPartialFormat(turn.streamingPartialIndex),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    // 流式 partial 预览
    if (turn.streamingPreviewBitmap != null) {
        Spacer(Modifier.height(8.dp))
        Image(
            bitmap = turn.streamingPreviewBitmap.asImageBitmap(),
            contentDescription = strings.workbenchStreamingPreviewTitle,
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppCorner.small),
            contentScale = ContentScale.Fit
        )
    } else if (turn.streamingStartedAt > 0L) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(AppCorner.small)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                strings.chatStreamingWaiting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (turn.streamingStartedAt > 0L) {
        val elapsedMs = System.currentTimeMillis() - turn.streamingStartedAt
        val seconds = (elapsedMs / 1000).coerceAtLeast(0)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.workbenchStreamingElapsed(seconds.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) { Text(strings.workbenchStop) }
        }
    }
}

@Composable
private fun AssistantResultContent(
    turn: ChatTurn,
    onShare: (HistoryItem) -> Unit,
    onUseAsReference: (HistoryItem) -> Unit,
    onSendToEdit: (HistoryItem) -> Unit
) {
    val strings = LocalStrings.current
    val item = turn.resultItem
    if (item != null) {
        AsyncImage(
            model = File(item.imagePath),
            contentDescription = item.prompt,
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppCorner.small),
            contentScale = ContentScale.FillWidth
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.prompt,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        // 操作按钮行
        FlowRowActions(
            onShare = { onShare(item) },
            onUseAsReference = { onUseAsReference(item) },
            onSendToEdit = { onSendToEdit(item) }
        )
    } else {
        Text(
            strings.workbenchNoResult,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FlowRowActions(
    onShare: () -> Unit,
    onUseAsReference: () -> Unit,
    onSendToEdit: () -> Unit
) {
    val strings = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionChip(text = strings.chatTurnShare, onClick = onShare, modifier = Modifier.weight(1f))
        ActionChip(text = strings.chatTurnUseAsReference, onClick = onUseAsReference, modifier = Modifier.weight(1f))
        ActionChip(text = strings.chatTurnSendToEdit, onClick = onSendToEdit, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(AppCorner.pill)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() },
        shape = AppCorner.pill,
        color = cs.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AssistantErrorContent(turn: ChatTurn, onRetry: () -> Unit) {
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            strings.chatTurnError,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(
        turn.errorMessage ?: "",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onRetry) { Text(strings.chatTurnRetry) }
}

// =====================================================================
// Input bar (chat-style)
// =====================================================================

@Composable
private fun InputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    isGenerating: Boolean,
    isStreaming: Boolean,
    canSend: Boolean,
    model: ImageModelDefinition?,
    references: List<ReferenceImageUi>,
    onAddReference: () -> Unit,
    onRemoveReference: (Int) -> Unit,
    onClearReferences: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onOpenMaskEditor: () -> Unit,
    canEditMask: Boolean,
    providerConfigured: Boolean,
    onOpenSettings: () -> Unit
) {
    val strings = LocalStrings.current
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cs.surface,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, cs.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .imePadding()
        ) {
            // Chips 行：模型 / 参考图 / 高级 / 蒙版 / provider 状态
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    AppChip(
                        label = model?.label ?: strings.chatModelChip,
                        selected = false,
                        onClick = onOpenAdvanced
                    )
                }
                item {
                    AppChip(
                        label = strings.chatReferencesChip(references.size),
                        selected = references.isNotEmpty(),
                        onClick = {
                            if (references.isEmpty()) onAddReference()
                            // 已有参考图时点击 chip 不展开（缩略图在下方），仅作状态指示
                        }
                    )
                }
                item {
                    AppChip(
                        label = strings.chatAdvancedChip,
                        selected = false,
                        onClick = onOpenAdvanced
                    )
                }
                if (canEditMask) {
                    item {
                        AppChip(
                            label = strings.chatMaskChip,
                            selected = false,
                            onClick = onOpenMaskEditor
                        )
                    }
                }
                if (!providerConfigured) {
                    item {
                        AppChip(
                            label = strings.chatProviderMissing,
                            selected = true,
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            // 参考图缩略图行（如有）
            if (references.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    itemsIndexed(references) { index, ref ->
                        ReferenceThumbnail(
                            uri = ref.uri,
                            onRemove = { onRemoveReference(index) }
                        )
                    }
                    item {
                        AddReferencePill(onClick = onAddReference)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Prompt + 发送按钮
            Row(verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AppCorner.card)
                        .background(cs.surfaceVariant.copy(alpha = 0.3f))
                        .border(
                            width = 0.5.dp,
                            color = cs.outlineVariant,
                            shape = AppCorner.card
                        )
                        .heightIn(min = 50.dp, max = 140.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (prompt.isEmpty()) {
                            Text(
                                strings.chatInputPlaceholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = cs.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = prompt,
                            onValueChange = onPromptChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(cs.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // 发送 / 停止按钮（圆形）
                val sendEnabled = if (isGenerating) false else canSend
                val gradient = Brush.verticalGradient(
                    listOf(AppExtra.current.gradientStart, AppExtra.current.gradientEnd)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (sendEnabled) Modifier.background(gradient, CircleShape)
                            else Modifier.background(cs.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) {
                            if (isGenerating) onCancel() else if (canSend) onSend()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        Icon(
                            Icons.Outlined.Stop,
                            contentDescription = strings.workbenchStop,
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = strings.chatSend,
                            tint = if (sendEnabled) Color.White else cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceThumbnail(uri: android.net.Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(AppCorner.small)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun AddReferencePill(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(AppCorner.small)
            .background(cs.surfaceVariant.copy(alpha = 0.4f))
            .border(0.5.dp, cs.outlineVariant, AppCorner.small)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            tint = cs.primary
        )
    }
}

// =====================================================================
// Advanced params sheet
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedParamsSheet(
    state: WorkbenchUiState,
    onDismiss: () -> Unit,
    onSelectModel: (ImageModelDefinition) -> Unit,
    onCountChange: (Int) -> Unit,
    onSizeChange: (String?) -> Unit,
    onQualityChange: (String?) -> Unit,
    onFormatChange: (String?) -> Unit,
    onBackgroundChange: (String?) -> Unit,
    onModerationChange: (String?) -> Unit,
    onStreamingToggle: (Boolean) -> Unit
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppCorner.sheet,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            AppSectionHeader(strings.workbenchModel)
            // 模型列表（按 provider 分组）
            state.availableModels.groupBy { it.provider }.forEach { (provider, modelsForProvider) ->
                if (modelsForProvider.isNotEmpty()) {
                    Text(
                        text = ImageProvidersLabel(provider),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    modelsForProvider.forEach { model ->
                        ModelRow(
                            label = model.label,
                            selected = state.model?.id == model.id,
                            onClick = { onSelectModel(model); onDismiss() }
                        )
                    }
                }
            }

            val model = state.model
            if (model != null) {
                AppSectionHeader(strings.workbenchAdvanced)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (model.supportsStreaming) {
                        StreamingToggleRow(
                            enabled = state.streamingEnabled,
                            onToggle = onStreamingToggle
                        )
                    }
                    if (model.supportsCustomSize || model.sizePresets != null) {
                        SizeSection(model = model, current = state.size, onChange = onSizeChange)
                    }
                    if (model.supportsQuality) {
                        ChipGroup(strings.workbenchQuality, ImageQualities.ALL, state.quality, onQualityChange)
                    }
                    if (model.supportsOutputFormat) {
                        ChipGroup(strings.workbenchFormat, ImageOutputFormats.ALL, state.outputFormat, onFormatChange)
                    }
                    if (model.supportsBackground) {
                        ChipGroup(strings.workbenchBackground, ImageBackgrounds.ALL, state.background, onBackgroundChange)
                    }
                    if (model.supportsModeration) {
                        ChipGroup(strings.workbenchModeration, ImageModerations.ALL, state.moderation, onModerationChange)
                    }
                    CountSelector(current = state.count, onChange = onCountChange)
                }
            }
        }
    }
}

@Composable
private fun ModelRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) cs.primary else cs.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StreamingToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.workbenchStreamingTitle, style = MaterialTheme.typography.bodyMedium)
            Text(
                strings.workbenchStreamingHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        androidx.compose.material3.Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SizeSection(model: ImageModelDefinition, current: String?, onChange: (String?) -> Unit) {
    val strings = LocalStrings.current
    val sizes = buildList {
        model.sizePresets?.square?.let { add(strings.workbenchSizeSquare to it) }
        model.sizePresets?.landscape?.let { add(strings.workbenchSizeLandscape to it) }
        model.sizePresets?.portrait?.let { add(strings.workbenchSizePortrait to it) }
        model.defaultSize?.let { add(strings.workbenchSizeDefault to it) }
    }.distinctBy { it.second }
    if (sizes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(strings.workbenchSize, style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sizes) { (label, value) ->
                AppChip(
                    label = label,
                    selected = current == value,
                    onClick = { onChange(if (current == value) null else value) }
                )
            }
        }
    }
}

@Composable
private fun ChipGroup(
    label: String,
    options: List<String>,
    current: String?,
    onChange: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                AppChip(
                    label = option.replaceFirstChar { it.uppercase() },
                    selected = current == option,
                    onClick = { onChange(if (current == option) null else option) }
                )
            }
        }
    }
}

@Composable
private fun CountSelector(current: Int, onChange: (Int) -> Unit) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(strings.workbenchCount, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { count ->
                AppChip(
                    label = count.toString(),
                    selected = current == count,
                    onClick = { onChange(count) }
                )
            }
        }
    }
}

@Composable
private fun ImageProvidersLabel(provider: String): String = when (provider) {
    com.gptimage.playground.data.model.ImageProviders.GOOGLE -> "Google"
    com.gptimage.playground.data.model.ImageProviders.SENSENOVA -> "SenseNova"
    com.gptimage.playground.data.model.ImageProviders.SEEDREAM -> "Seedream"
    com.gptimage.playground.data.model.ImageProviders.STABILITY -> "Stability AI"
    else -> "OpenAI"
}

// =====================================================================
// Mask editor sheet
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaskEditorSheet(
    state: WorkbenchUiState,
    onDismiss: () -> Unit,
    onBrushSizeChange: (Int) -> Unit,
    onAddPoint: (Float, Float) -> Unit,
    onAddLine: (Float, Float, Float, Float) -> Unit,
    onClearMask: () -> Unit,
    onUndoLastPoint: () -> Unit,
    onSaveMask: () -> Unit
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppCorner.sheet,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Brush,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    strings.workbenchMaskTitle,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (state.maskSaved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            strings.workbenchMaskSaved,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = strings.commonClose
                    )
                }
            }

            Text(
                strings.workbenchMaskHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val srcBitmap = state.maskSourceBitmap
            val srcW = state.maskSourceWidth
            val srcH = state.maskSourceHeight
            val hasSource = srcBitmap != null && srcW > 0 && srcH > 0
            if (!hasSource) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.workbenchMaskLoadingSource, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                MaskCanvas(
                    sourceBitmap = srcBitmap!!,
                    srcWidth = srcW,
                    srcHeight = srcH,
                    drawnPoints = state.maskDrawnPoints,
                    onAddPoint = onAddPoint,
                    onAddLine = onAddLine
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            strings.workbenchMaskBrushSize,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${state.maskBrushSize}px",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = state.maskBrushSize.toFloat(),
                        onValueChange = { onBrushSizeChange(it.toInt()) },
                        valueRange = WorkbenchViewModel.MIN_BRUSH_SIZE.toFloat()..
                            WorkbenchViewModel.MAX_BRUSH_SIZE.toFloat()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = strings.workbenchMaskUndo,
                        onClick = onUndoLastPoint,
                        style = AppButtonStyle.Secondary,
                        enabled = state.maskDrawnPoints.isNotEmpty(),
                        leadingIcon = Icons.Outlined.Undo,
                        modifier = Modifier.weight(1f)
                    )
                    AppButton(
                        text = strings.workbenchMaskClear,
                        onClick = onClearMask,
                        style = AppButtonStyle.Secondary,
                        enabled = state.maskDrawnPoints.isNotEmpty(),
                        leadingIcon = Icons.Outlined.Delete,
                        modifier = Modifier.weight(1f)
                    )
                    AppButton(
                        text = strings.workbenchMaskSave,
                        onClick = onSaveMask,
                        enabled = state.maskDrawnPoints.isNotEmpty() && !state.maskSaved,
                        leadingIcon = Icons.Outlined.Save,
                        modifier = Modifier.weight(1f)
                    )
                }
                AppButton(
                    text = strings.workbenchMaskClose,
                    onClick = onDismiss,
                    style = AppButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =====================================================================
// Mask canvas — 复用原算法
// =====================================================================

@Composable
private fun MaskCanvas(
    sourceBitmap: Bitmap,
    srcWidth: Int,
    srcHeight: Int,
    drawnPoints: List<DrawnPoint>,
    onAddPoint: (Float, Float) -> Unit,
    onAddLine: (Float, Float, Float, Float) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(srcWidth.toFloat() / srcHeight.toFloat())
            .clip(AppCorner.small)
    ) {
        val boxWidthPx = constraints.maxWidth.toFloat()
        val boxHeightPx = constraints.maxHeight.toFloat()

        val toSrcX: (Float) -> Float = { touchX ->
            if (boxWidthPx > 0f) touchX / boxWidthPx * srcWidth else 0f
        }
        val toSrcY: (Float) -> Float = { touchY ->
            if (boxHeightPx > 0f) touchY / boxHeightPx * srcHeight else 0f
        }

        Image(
            bitmap = sourceBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        var lastSrc: Offset? by remember { mutableStateOf(null) }

        val previewColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val sx = toSrcX(offset.x)
                            val sy = toSrcY(offset.y)
                            onAddPoint(sx, sy)
                            lastSrc = Offset(sx, sy)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val sx = toSrcX(change.position.x)
                            val sy = toSrcY(change.position.y)
                            val prev = lastSrc
                            if (prev != null) {
                                onAddLine(prev.x, prev.y, sx, sy)
                            } else {
                                onAddPoint(sx, sy)
                            }
                            lastSrc = Offset(sx, sy)
                        },
                        onDragEnd = { lastSrc = null },
                        onDragCancel = { lastSrc = null }
                    )
                }
        ) {
            drawnPoints.forEach { p ->
                val cx = p.x / srcWidth * size.width
                val cy = p.y / srcHeight * size.height
                val radiusPx = p.size / srcWidth * size.width
                drawCircle(
                    color = previewColor,
                    radius = radiusPx.coerceAtLeast(1f),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

// =====================================================================
// Bubble helpers
// =====================================================================

private fun bubbleShape(alignedRight: Boolean): androidx.compose.ui.graphics.Shape {
    return RoundedCornerShape(
        topStart = if (alignedRight) 18f else 4f,
        topEnd = if (alignedRight) 4f else 18f,
        bottomEnd = 18f,
        bottomStart = 18f
    )
}

// =====================================================================
// Share
// =====================================================================

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
