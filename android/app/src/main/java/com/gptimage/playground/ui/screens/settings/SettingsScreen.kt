package com.gptimage.playground.ui.screens.settings

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.CustomImageModel
import com.gptimage.playground.data.model.CustomImageModelCapabilities
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ThemeMode
import com.gptimage.playground.ui.i18n.LocalStrings
import com.gptimage.playground.ui.theme.AppCorner
import com.gptimage.playground.ui.theme.ContentPadding
import com.gptimage.playground.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory((LocalContext.current.applicationContext as PlaygroundApp).locator)
    )
) {
    val strings = LocalStrings.current
    val config by viewModel.config.collectAsState()
    val saved by viewModel.savedEvent.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val cs = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        if (saved) {
            snackbarHostState.showSnackbar(strings.settingsSaved)
            viewModel.consumeSavedEvent()
        }
    }
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeErrorMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // iOS 风自定义顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.navSettings,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = ContentPadding.screen,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                // —— 供应商与模型 ——
                item { SettingsSectionHeader(strings.settingsProviders) }
                item {
                    CardContainer {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ImageProviders.ALL.forEachIndexed { idx, provider ->
                                ProviderRow(
                                    providerId = provider,
                                    apiKey = config.credentialsFor(provider).apiKey,
                                    baseUrl = config.credentialsFor(provider).baseUrl,
                                    onSave = { key, url ->
                                        viewModel.setProviderCredentials(provider, key, url)
                                    }
                                )
                                if (idx != ImageProviders.ALL.lastIndex) {
                                    HorizontalDivider(
                                        color = cs.outlineVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // —— 默认模型 ——
                item { SettingsSectionHeader(strings.settingsDefaultModel) }
                item {
                    CardContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ContentPadding.card),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            ModelSelector(
                                models = viewModel.allModels(),
                                current = config.defaultModelId,
                                onSelect = { viewModel.setDefaultModel(it) }
                            )
                        }
                    }
                }

                // —— 自定义模型 ——
                item { SettingsSectionHeader(strings.customModelsSection) }
                item {
                    CardContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ContentPadding.card),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            if (config.customImageModels.isEmpty()) {
                                Text(
                                    text = strings.customModelsEmpty,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            } else {
                                config.customImageModels.forEach { model ->
                                    CustomModelRow(
                                        model = model,
                                        onEdit = {
                                            LocalCallbackHolder.editRequest?.invoke(model)
                                        },
                                        onDelete = { viewModel.deleteCustomModel(model.id) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TonalPillButton(
                                    text = strings.customModelsAdd,
                                    leadingIcon = Icons.Outlined.Add,
                                    onClick = { LocalCallbackHolder.addRequest?.invoke() }
                                )
                            }
                        }
                    }
                }

                // —— 外观 ——
                item { SettingsSectionHeader(strings.settingsAppearance) }
                item {
                    CardContainer {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ThemeRow(current = config.themeMode, onSelect = viewModel::setThemeMode)
                            HorizontalDivider(
                                color = cs.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            LanguageRow(current = config.language, onSelect = viewModel::setLanguage)
                        }
                    }
                }

                // —— 关于 ——
                item { SettingsSectionHeader(strings.settingsAbout) }
                item { AboutCard() }

                // 浮动 tab bar 占位
                item {
                    Spacer(
                        Modifier
                            .navigationBarsPadding()
                            .size(72.dp)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    }

    // 自定义模型编辑对话框
    var editingModel by remember { mutableStateOf<CustomImageModel?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var isEditingExisting by remember { mutableStateOf(false) }

    CustomModelsCallbacksBridge(
        onAddRequest = {
            editingModel = null
            isEditingExisting = false
            showEditor = true
        },
        onEditRequest = { model ->
            editingModel = model
            isEditingExisting = true
            showEditor = true
        }
    )

    if (showEditor) {
        CustomModelEditorDialog(
            initial = editingModel,
            isEditing = isEditingExisting,
            onDismiss = { showEditor = false },
            onSave = { model ->
                viewModel.upsertCustomModel(model)
                showEditor = false
            }
        )
    }
}

@Composable
private fun CustomModelsCallbacksBridge(
    onAddRequest: () -> Unit,
    onEditRequest: (CustomImageModel) -> Unit
) {
    LocalCallbackHolder.addRequest = onAddRequest
    LocalCallbackHolder.editRequest = onEditRequest
}

private object LocalCallbackHolder {
    var addRequest: (() -> Unit)? = null
    var editRequest: ((CustomImageModel) -> Unit)? = null
}

// =====================================================================
// iOS Settings 风：分组标题（小字 + 大写风格 secondary label）
// =====================================================================
@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 0.dp)
    )
}

// =====================================================================
// iOS Settings 风：白底圆角卡片容器
// =====================================================================
@Composable
private fun CardContainer(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorner.card)
            .background(cs.surface)
            .border(0.5.dp, cs.outlineVariant, AppCorner.card)
    ) {
        content()
    }
}

// =====================================================================
// Provider 区块（折叠为列表行 + 内嵌表单）
// =====================================================================
@Composable
private fun ProviderRow(
    providerId: String,
    apiKey: String,
    baseUrl: String,
    onSave: (String, String) -> Unit
) {
    val strings = LocalStrings.current
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var showKey by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val label = when (providerId) {
        ImageProviders.GOOGLE -> strings.settingsProviderGoogle
        ImageProviders.SENSENOVA -> strings.settingsProviderSensenova
        ImageProviders.SEEDREAM -> strings.settingsProviderSeedream
        ImageProviders.STABILITY -> strings.settingsProviderStability
        else -> strings.settingsProviderOpenai
    }
    val defaultBaseUrl = ImageProviders.defaultBaseUrl(providerId)

    Column(modifier = Modifier.fillMaxWidth()) {
        // 头部行：icon + provider 名称 + 已配置状态 + 展开箭头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { expanded = !expanded }
                .padding(horizontal = ContentPadding.card, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(AppCorner.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (apiKey.isNotBlank()) "•••• ••••" else strings.settingsNoProviderConfigured,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ContentPadding.card, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                IosOutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = strings.settingsApiKey,
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    onTrailingIconClick = { showKey = !showKey }
                )
                IosOutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = strings.settingsApiBaseUrl,
                    placeholder = defaultBaseUrl,
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        keyInput = ""
                        urlInput = ""
                    }) { Text(strings.settingsClearApiKey, color = MaterialTheme.colorScheme.error) }
                    PrimaryPillButton(
                        text = strings.settingsSave,
                        onClick = { onSave(keyInput, urlInput) }
                    )
                }
            }
        }
    }
}

// =====================================================================
// iOS 风单行输入框（用 OutlinedTextField + 自定义颜色）
// =====================================================================
@Composable
private fun IosOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) } },
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = visualTransformation,
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) { onTrailingIconClick?.invoke() }
                )
            }
        } else null,
        shape = AppCorner.card,
        modifier = modifier.fillMaxWidth()
    )
}

// =====================================================================
// 默认模型选择器：按 provider 分组，胶囊 chip
// =====================================================================
@Composable
private fun ModelSelector(
    models: List<ImageModelDefinition>,
    current: String,
    onSelect: (String) -> Unit
) {
    val grouped = remember(models) { models.groupBy { it.provider } }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        grouped.forEach { (provider, modelsForProvider) ->
            Text(
                text = ImageProviders.label(provider).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                modelsForProvider.forEach { model ->
                    FilterPill(
                        label = model.label,
                        selected = current == model.id,
                        onClick = { onSelect(model.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (selected) cs.primary else cs.surfaceVariant.copy(alpha = 0.4f)
    val fg = if (selected) cs.onPrimary else cs.onSurface
    val border = if (selected) null else 0.5.dp

    Box(
        modifier = Modifier
            .clip(AppCorner.pill)
            .background(bg)
            .then(if (border != null) Modifier.border(border, cs.outlineVariant, AppCorner.pill) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// =====================================================================
// 自定义模型列表行
// =====================================================================
@Composable
private fun CustomModelRow(
    model: CustomImageModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onEdit() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.label?.takeIf { it.isNotBlank() } ?: model.id,
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurface
            )
            Text(
                text = ImageProviders.label(model.provider),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onEdit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, tint = cs.error, modifier = Modifier.size(18.dp))
        }
    }
}

// =====================================================================
// 自定义模型编辑对话框（保持 AlertDialog，但应用 iOS 风）
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomModelEditorDialog(
    initial: CustomImageModel?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomImageModel) -> Unit
) {
    val strings = LocalStrings.current

    var idInput by remember { mutableStateOf(initial?.id?.removePrefix("custom:") ?: "") }
    var labelInput by remember { mutableStateOf(initial?.label ?: "") }
    var provider by remember { mutableStateOf(initial?.provider ?: ImageProviders.OPENAI) }
    var defaultSize by remember { mutableStateOf(initial?.defaultSize ?: "") }
    var sizeSquare by remember { mutableStateOf(initial?.sizePresets?.square ?: "") }
    var sizeLandscape by remember { mutableStateOf(initial?.sizePresets?.landscape ?: "") }
    var sizePortrait by remember { mutableStateOf(initial?.sizePresets?.portrait ?: "") }
    val caps = initial?.capabilities ?: CustomImageModelCapabilities()
    var supportsEditing by remember { mutableStateOf(caps.supportsEditing ?: false) }
    var supportsCustomSize by remember { mutableStateOf(caps.supportsCustomSize ?: false) }
    var supportsQuality by remember { mutableStateOf(caps.supportsQuality ?: false) }
    var supportsOutputFormat by remember { mutableStateOf(caps.supportsOutputFormat ?: false) }
    var supportsBackground by remember { mutableStateOf(caps.supportsBackground ?: false) }
    var supportsMask by remember { mutableStateOf(caps.supportsMask ?: false) }
    var supportsStreaming by remember { mutableStateOf(caps.supportsStreaming ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppCorner.dialog,
        title = { Text(if (isEditing) strings.customModelsEdit else strings.customModelsAdd) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                IosOutlinedTextField(
                    value = idInput,
                    onValueChange = { idInput = it },
                    label = strings.customModelsIdLabel,
                    placeholder = strings.customModelsIdHint,
                    singleLine = true,
                    enabled = !isEditing
                )
                IosOutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = strings.customModelsLabelField,
                    singleLine = true
                )
                var providerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = ImageProviders.label(provider),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.customModelsProvider) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        shape = AppCorner.card,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        ImageProviders.ALL.forEach { p ->
                            AssistChip(
                                onClick = {
                                    provider = p
                                    providerExpanded = false
                                },
                                label = { Text(ImageProviders.label(p)) },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                colors = AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
                IosOutlinedTextField(
                    value = defaultSize,
                    onValueChange = { defaultSize = it },
                    label = strings.customModelsDefaultSize,
                    singleLine = true
                )
                IosOutlinedTextField(
                    value = sizeSquare,
                    onValueChange = { sizeSquare = it },
                    label = strings.customModelsSizeSquare,
                    singleLine = true
                )
                IosOutlinedTextField(
                    value = sizeLandscape,
                    onValueChange = { sizeLandscape = it },
                    label = strings.customModelsSizeLandscape,
                    singleLine = true
                )
                IosOutlinedTextField(
                    value = sizePortrait,
                    onValueChange = { sizePortrait = it },
                    label = strings.customModelsSizePortrait,
                    singleLine = true
                )
                HorizontalDivider()
                Text(
                    text = strings.customModelsCapabilities.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                CapabilityRow(strings.customModelsSupportsEditing, supportsEditing) { supportsEditing = it }
                CapabilityRow(strings.customModelsSupportsCustomSize, supportsCustomSize) { supportsCustomSize = it }
                CapabilityRow(strings.customModelsSupportsQuality, supportsQuality) { supportsQuality = it }
                CapabilityRow(strings.customModelsSupportsOutputFormat, supportsOutputFormat) { supportsOutputFormat = it }
                CapabilityRow(strings.customModelsSupportsBackground, supportsBackground) { supportsBackground = it }
                CapabilityRow(strings.customModelsSupportsMask, supportsMask) { supportsMask = it }
                CapabilityRow(strings.customModelsSupportsStreaming, supportsStreaming) { supportsStreaming = it }
            }
        },
        confirmButton = {
            PrimaryPillButton(
                text = strings.customModelsSave,
                onClick = {
                    val rawId = idInput.trim()
                    if (rawId.isEmpty()) return@PrimaryPillButton
                    val model = CustomImageModel(
                        id = rawId,
                        provider = provider,
                        label = labelInput.trim().ifEmpty { null },
                        capabilities = CustomImageModelCapabilities(
                            supportsEditing = supportsEditing,
                            supportsCustomSize = supportsCustomSize,
                            supportsQuality = supportsQuality,
                            supportsOutputFormat = supportsOutputFormat,
                            supportsBackground = supportsBackground,
                            supportsMask = supportsMask,
                            supportsStreaming = supportsStreaming
                        ),
                        sizePresets = com.gptimage.playground.data.model.ImageModelSizePresets(
                            square = sizeSquare.trim().ifEmpty { null },
                            landscape = sizeLandscape.trim().ifEmpty { null },
                            portrait = sizePortrait.trim().ifEmpty { null }
                        ),
                        defaultSize = defaultSize.trim().ifEmpty { null }
                    )
                    onSave(model)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.commonCancel) }
        }
    )
}

@Composable
private fun CapabilityRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// =====================================================================
// 外观：主题/语言 picker（行式 + 右侧 chips）
// =====================================================================
@Composable
private fun ThemeRow(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding.card, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(AppCorner.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text(strings.settingsTheme, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.size(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            FilterPill(strings.settingsThemeLight, current == ThemeMode.LIGHT) { onSelect(ThemeMode.LIGHT) }
            FilterPill(strings.settingsThemeDark, current == ThemeMode.DARK) { onSelect(ThemeMode.DARK) }
            FilterPill(strings.settingsThemeSystem, current == ThemeMode.SYSTEM) { onSelect(ThemeMode.SYSTEM) }
        }
    }
}

@Composable
private fun LanguageRow(current: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding.card, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(AppCorner.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text(strings.settingsLanguage, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.size(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            FilterPill(strings.settingsThemeSystem, current == AppLanguage.SYSTEM) { onSelect(AppLanguage.SYSTEM) }
            FilterPill("简体中文", current == AppLanguage.SIMPLIFIED_CHINESE) { onSelect(AppLanguage.SIMPLIFIED_CHINESE) }
            FilterPill("English", current == AppLanguage.ENGLISH) { onSelect(AppLanguage.ENGLISH) }
        }
    }
}

// =====================================================================
// 关于卡片
// =====================================================================
@Composable
private fun AboutCard() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("1.0.0")
    }
    val cs = MaterialTheme.colorScheme
    val extra = com.gptimage.playground.ui.theme.AppExtra.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorner.card)
            .background(cs.surface)
            .border(0.5.dp, cs.outlineVariant, AppCorner.card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ContentPadding.card),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppCorner.pill)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(extra.gradientStart, extra.gradientEnd)),
                            shape = AppCorner.pill
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(12.dp))
                Text(strings.appName, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Text(strings.settingsAboutDescription, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Text(strings.settingsAboutVersion(versionName ?: "1.0.0"), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Text(strings.settingsAboutOpenSource, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

// =====================================================================
// iOS 风按钮：胶囊形 / Primary 品牌渐变 / Tonal 浅色
// =====================================================================
@Composable
private fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val cs = MaterialTheme.colorScheme
    val extra = com.gptimage.playground.ui.theme.AppExtra.current
    val shape = AppCorner.pill
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(extra.gradientStart, extra.gradientEnd)),
                shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
            }
            Text(text, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TonalPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val cs = MaterialTheme.colorScheme
    val shape = AppCorner.pill
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(cs.primaryContainer, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(6.dp))
            }
            Text(text, color = cs.onPrimaryContainer, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
