package com.gptimage.playground.ui.screens.workbench

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.data.model.ImageBackgrounds
import com.gptimage.playground.data.model.ImageModelCatalog
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageModerations
import com.gptimage.playground.data.model.ImageOutputFormats
import com.gptimage.playground.data.model.ImageQualities
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.i18n.strings

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

    // 订阅「相册 → 用作参考图 / 发送到编辑」的跨页面传输。
    // 当用户在相册点对应按钮时，bus.pending 会变成非 null，
    // 这里在 Composable 第一次进入 composition + bus 有 pending 时消费一次。
    androidx.compose.runtime.LaunchedEffect(pendingReferenceBus) {
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

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(strings.navWorkbench) },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = strings.settingsProviders)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PromptField(
                value = state.prompt,
                onValueChange = viewModel::updatePrompt,
                onOpenTemplates = { showTemplatesDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            ModelPicker(
                models = state.availableModels,
                selected = state.model,
                onSelect = viewModel::selectModel,
                modifier = Modifier.fillMaxWidth()
            )

            AdvancedSection(
                expanded = state.advancedExpanded,
                onToggle = viewModel::setAdvancedExpanded,
                state = state,
                onCountChange = viewModel::setCount,
                onSizeChange = viewModel::setSize,
                onQualityChange = viewModel::setQuality,
                onFormatChange = viewModel::setOutputFormat,
                onBackgroundChange = viewModel::setBackground,
                onModerationChange = viewModel::setModeration,
                onStreamingToggle = viewModel::setStreamingEnabled,
                modifier = Modifier.fillMaxWidth()
            )

            ReferenceImagesSection(
                references = state.referenceImages,
                onAddClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemove = viewModel::removeReferenceAt,
                onClear = viewModel::clearReferences,
                modifier = Modifier.fillMaxWidth()
            )

            GenerateButton(
                enabled = state.prompt.isNotBlank() && state.providerConfigured && !state.isGenerating,
                isGenerating = state.isGenerating,
                isStreaming = state.isStreaming,
                isEdit = state.referenceImages.isNotEmpty(),
                onClick = viewModel::generate,
                onCancel = viewModel::cancelGenerate,
                modifier = Modifier.fillMaxWidth()
            )

            if (!state.providerConfigured) {
                ProviderNotConfiguredHint(
                    onOpenSettings = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.error?.let { err ->
                ErrorBanner(message = err, onDismiss = viewModel::clearError, modifier = Modifier.fillMaxWidth())
            }

            ResultPreview(
                item = state.lastResult,
                isStreaming = state.isStreaming,
                streamingPreview = state.streamingPreview,
                streamingPartialIndex = state.streamingPartialIndex,
                streamingStartedAt = state.streamingStartedAt,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.size(48.dp))
        }
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
}

@Composable
private fun PromptField(
    value: String,
    onValueChange: (String) -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onOpenTemplates) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(strings.templatesOpen)
            }
        }
        Spacer(Modifier.size(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 240.dp),
            placeholder = { Text(strings.workbenchPromptPlaceholder) },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(
    models: List<ImageModelDefinition>,
    selected: ImageModelDefinition?,
    onSelect: (ImageModelDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.label ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.workbenchModel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 用传入的合并后 models（内置 + 自定义），而不是 ImageModelCatalog.groupByProvider()。
            // 否则用户录入的自定义模型不会出现在工作台的下拉里。
            models.groupBy { it.provider }.forEach { (provider, modelsForProvider) ->
                if (modelsForProvider.isNotEmpty()) {
                    Text(
                        text = ImageProvidersLabel(provider),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    modelsForProvider.forEach { model ->
                        FilledTonalButton(
                            onClick = {
                                onSelect(model)
                                expanded = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(model.label)
                        }
                    }
                }
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

@Composable
private fun AdvancedSection(
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    state: WorkbenchUiState,
    onCountChange: (Int) -> Unit,
    onSizeChange: (String?) -> Unit,
    onQualityChange: (String?) -> Unit,
    onFormatChange: (String?) -> Unit,
    onBackgroundChange: (String?) -> Unit,
    onModerationChange: (String?) -> Unit,
    onStreamingToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val model = state.model
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.workbenchAdvanced, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { onToggle(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (expanded && model != null) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 流式预览开关：仅当模型 supportsStreaming 时显示
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
                        ChipGroup(
                            label = strings.workbenchQuality,
                            options = ImageQualities.ALL,
                            current = state.quality,
                            onChange = onQualityChange
                        )
                    }
                    if (model.supportsOutputFormat) {
                        ChipGroup(
                            label = strings.workbenchFormat,
                            options = ImageOutputFormats.ALL,
                            current = state.outputFormat,
                            onChange = onFormatChange
                        )
                    }
                    if (model.supportsBackground) {
                        ChipGroup(
                            label = strings.workbenchBackground,
                            options = ImageBackgrounds.ALL,
                            current = state.background,
                            onChange = onBackgroundChange
                        )
                    }
                    if (model.supportsModeration) {
                        ChipGroup(
                            label = strings.workbenchModeration,
                            options = ImageModerations.ALL,
                            current = state.moderation,
                            onChange = onModerationChange
                        )
                    }
                    CountSelector(current = state.count, onChange = onCountChange)
                }
            }
        }
    }
}

@Composable
private fun StreamingToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.workbenchStreamingTitle,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = strings.workbenchStreamingHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun SizeSection(
    model: ImageModelDefinition,
    current: String?,
    onChange: (String?) -> Unit
) {
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
                ElevatedFilterChip(
                    selected = current == value,
                    onClick = { onChange(if (current == value) null else value) },
                    label = { Text(label) }
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
                ElevatedFilterChip(
                    selected = current == option,
                    onClick = { onChange(if (current == option) null else option) },
                    label = { Text(option.replaceFirstChar { it.uppercase() }) }
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
                ElevatedFilterChip(
                    selected = current == count,
                    onClick = { onChange(count) },
                    label = { Text(count.toString()) }
                )
            }
        }
    }
}

@Composable
private fun ReferenceImagesSection(
    references: List<ReferenceImageUi>,
    onAddClick: () -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings.workbenchReferenceImages,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (references.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text(strings.workbenchClearReferences)
                }
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(references) { index, ref ->
                ReferenceImageCard(
                    reference = ref,
                    onRemove = { onRemove(index) }
                )
            }
            item {
                AddReferenceButton(onClick = onAddClick)
            }
        }
    }
}

@Composable
private fun ReferenceImageCard(
    reference: ReferenceImageUi,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = reference.uri,
            contentDescription = reference.name,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun AddReferenceButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.AddPhotoAlternate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = strings().workbenchAddReference,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GenerateButton(
    enabled: Boolean,
    isGenerating: Boolean,
    isStreaming: Boolean,
    isEdit: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    // 流式进行中显示「停止」按钮，否则显示「生成」按钮
    if (isStreaming) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 流式进行中仍保留一个不可点击的「生成中」状态指示，便于用户看清当前在生成
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(strings.workbenchStreaming)
            }
            Button(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Outlined.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.workbenchStop)
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(strings.workbenchGenerating)
            } else {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isEdit) strings.commonEdit else strings.workbenchGenerate)
            }
        }
    }
}

@Composable
private fun ProviderNotConfiguredHint(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = strings.workbenchProviderNotConfigured,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onOpenSettings) {
                Text(strings.workbenchOpenSettings)
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = strings.commonClose)
            }
        }
    }
}

@Composable
private fun ResultPreview(
    item: com.gptimage.playground.data.model.HistoryItem?,
    isStreaming: Boolean,
    streamingPreview: android.graphics.Bitmap?,
    streamingPartialIndex: Int,
    streamingStartedAt: Long,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    if (isStreaming) {
        // 流式预览：优先展示 partial bitmap；如果有 lastResult 也保留 completed 的提示
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.workbenchStreamingPreviewTitle,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (streamingPartialIndex > 0) {
                        Text(
                            text = strings.workbenchStreamingPartialFormat(streamingPartialIndex),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                if (streamingPreview != null) {
                    Image(
                        bitmap = streamingPreview.asImageBitmap(),
                        contentDescription = strings.workbenchStreamingPreviewTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.workbenchStreamingWaiting,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (streamingStartedAt > 0L) {
                    val elapsedMs = System.currentTimeMillis() - streamingStartedAt
                    val seconds = (elapsedMs / 1000).coerceAtLeast(0)
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = strings.workbenchStreamingElapsed(seconds.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    if (item == null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(strings.workbenchNoResult, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(strings.workbenchResult, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.size(8.dp))
                AsyncImage(
                    model = java.io.File(item.imagePath),
                    contentDescription = item.prompt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.size(8.dp))
                Text(item.prompt, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
