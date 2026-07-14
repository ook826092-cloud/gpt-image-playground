package com.gptimage.playground.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gptimage.playground.PlaygroundApp
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.CustomImageModel
import com.gptimage.playground.data.model.CustomImageModelCapabilities
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageModelSizePresets
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ThemeMode
import com.gptimage.playground.ui.i18n.LocalStrings

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

    Scaffold(
        topBar = { TopAppBar(title = { Text(strings.navSettings) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(strings.settingsProviders)
            ImageProviders.ALL.forEach { provider ->
                ProviderSection(
                    providerId = provider,
                    apiKey = config.credentialsFor(provider).apiKey,
                    baseUrl = config.credentialsFor(provider).baseUrl,
                    onSave = { key, url ->
                        viewModel.setProviderCredentials(provider, key, url)
                    }
                )
            }

            HorizontalDivider()

            SectionHeader(strings.settingsDefaultModel)
            ModelSelector(
                models = viewModel.allModels(),
                current = config.defaultModelId,
                onSelect = { viewModel.setDefaultModel(it) }
            )

            HorizontalDivider()

            SectionHeader(strings.customModelsSection)
            CustomModelsSection(
                models = config.customImageModels,
                onAdd = { /* 由外部对话框处理 */ },
                onEdit = { /* 由外部对话框处理 */ },
                onDelete = { viewModel.deleteCustomModel(it.id) }
            )

            HorizontalDivider()

            SectionHeader(strings.settingsAppearance)
            ThemePicker(current = config.themeMode, onSelect = viewModel::setThemeMode)
            Spacer(Modifier.size(8.dp))
            LanguagePicker(current = config.language, onSelect = viewModel::setLanguage)

            HorizontalDivider()

            SectionHeader(strings.settingsAbout)
            AboutSection()

            Spacer(Modifier.size(64.dp))
        }
    }

    // 自定义模型编辑对话框（add + edit 共用一个 Composable）
    var editingModel by remember { mutableStateOf<CustomImageModel?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var isEditingExisting by remember { mutableStateOf(false) }

    // 监听 CustomModelsSection 的添加/编辑请求
    // 这里通过派生状态把请求转发到对话框
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

/**
 * 桥接：把 CustomModelsSection 的 add/edit 请求转发到上层对话框状态。
 *
 * 这里用一个简单的 Composable + remember 把回调保存到 composition 里，
 * 让 CustomModelsSection 内部能拿到。否则需要把回调链一路传下去。
 * 实际实现用一个 CompositionLocal 也行，但这里简化为 remember mutableState。
 */
@Composable
private fun CustomModelsCallbacksBridge(
    onAddRequest: () -> Unit,
    onEditRequest: (CustomImageModel) -> Unit
) {
    // 暴露给 CustomModelsSection 的回调保存在单例 object 里，简化跨 Composable 通信
    LocalCallbackHolder.addRequest = onAddRequest
    LocalCallbackHolder.editRequest = onEditRequest
}

private object LocalCallbackHolder {
    var addRequest: (() -> Unit)? = null
    var editRequest: ((CustomImageModel) -> Unit)? = null
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ProviderSection(
    providerId: String,
    apiKey: String,
    baseUrl: String,
    onSave: (String, String) -> Unit
) {
    val strings = LocalStrings.current
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var showKey by remember { mutableStateOf(false) }

    val label = when (providerId) {
        ImageProviders.GOOGLE -> strings.settingsProviderGoogle
        ImageProviders.SENSENOVA -> strings.settingsProviderSensenova
        ImageProviders.SEEDREAM -> strings.settingsProviderSeedream
        ImageProviders.STABILITY -> strings.settingsProviderStability
        else -> strings.settingsProviderOpenai
    }
    val defaultBaseUrl = ImageProviders.defaultBaseUrl(providerId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text(strings.settingsApiKey) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) strings.settingsHideApiKey else strings.settingsShowApiKey)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text(strings.settingsApiBaseUrl) },
                placeholder = { Text(defaultBaseUrl) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    keyInput = ""
                    urlInput = ""
                }) { Text(strings.settingsClearApiKey) }
                Button(onClick = { onSave(keyInput, urlInput) }) {
                    Text(strings.settingsSave)
                }
            }
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<ImageModelDefinition>,
    current: String,
    onSelect: (String) -> Unit
) {
    val grouped = remember(models) { models.groupBy { it.provider } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grouped.forEach { (provider, modelsForProvider) ->
            Text(
                text = ImageProviders.label(provider),
                style = MaterialTheme.typography.labelMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                modelsForProvider.take(8).forEach { model ->
                    FilterChip(
                        selected = current == model.id,
                        onClick = { onSelect(model.id) },
                        label = { Text(model.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomModelsSection(
    models: List<CustomImageModel>,
    onAdd: () -> Unit,
    onEdit: (CustomImageModel) -> Unit,
    onDelete: (CustomImageModel) -> Unit
) {
    val strings = LocalStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (models.isEmpty()) {
                Text(
                    text = strings.customModelsEmpty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                models.forEach { model ->
                    CustomModelRow(
                        model = model,
                        onEdit = {
                            LocalCallbackHolder.editRequest?.invoke(model)
                        },
                        onDelete = { onDelete(model) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { LocalCallbackHolder.addRequest?.invoke() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(strings.customModelsAdd)
                }
            }
        }
    }
}

@Composable
private fun CustomModelRow(
    model: CustomImageModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.label?.takeIf { it.isNotBlank() } ?: model.id,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = ImageProviders.label(model.provider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomModelEditorDialog(
    initial: CustomImageModel?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomImageModel) -> Unit
) {
    val strings = LocalStrings.current

    // 表单字段
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
        title = { Text(if (isEditing) strings.customModelsEdit else strings.customModelsAdd) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = idInput,
                    onValueChange = { idInput = it },
                    label = { Text(strings.customModelsIdLabel) },
                    placeholder = { Text(strings.customModelsIdHint) },
                    singleLine = true,
                    enabled = !isEditing, // 编辑时 id 不可改
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text(strings.customModelsLabelField) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Provider 下拉
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = defaultSize,
                    onValueChange = { defaultSize = it },
                    label = { Text(strings.customModelsDefaultSize) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sizeSquare,
                    onValueChange = { sizeSquare = it },
                    label = { Text(strings.customModelsSizeSquare) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sizeLandscape,
                    onValueChange = { sizeLandscape = it },
                    label = { Text(strings.customModelsSizeLandscape) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sizePortrait,
                    onValueChange = { sizePortrait = it },
                    label = { Text(strings.customModelsSizePortrait) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                Text(
                    text = strings.customModelsCapabilities,
                    style = MaterialTheme.typography.titleSmall
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
            Button(onClick = {
                val rawId = idInput.trim()
                if (rawId.isEmpty()) return@Button  // 简单跳过空 ID
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
                    sizePresets = ImageModelSizePresets(
                        square = sizeSquare.trim().ifEmpty { null },
                        landscape = sizeLandscape.trim().ifEmpty { null },
                        portrait = sizePortrait.trim().ifEmpty { null }
                    ),
                    defaultSize = defaultSize.trim().ifEmpty { null }
                )
                onSave(model)
            }) { Text(strings.customModelsSave) }
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
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemePicker(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(8.dp))
        Text(strings.settingsTheme, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = current == ThemeMode.LIGHT,
            onClick = { onSelect(ThemeMode.LIGHT) },
            label = { Text(strings.settingsThemeLight) }
        )
        FilterChip(
            selected = current == ThemeMode.DARK,
            onClick = { onSelect(ThemeMode.DARK) },
            label = { Text(strings.settingsThemeDark) }
        )
        FilterChip(
            selected = current == ThemeMode.SYSTEM,
            onClick = { onSelect(ThemeMode.SYSTEM) },
            label = { Text(strings.settingsThemeSystem) }
        )
    }
}

@Composable
private fun LanguagePicker(current: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(8.dp))
        Text(strings.settingsLanguage, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = current == AppLanguage.SYSTEM,
            onClick = { onSelect(AppLanguage.SYSTEM) },
            label = { Text(strings.settingsThemeSystem) }
        )
        FilterChip(
            selected = current == AppLanguage.SIMPLIFIED_CHINESE,
            onClick = { onSelect(AppLanguage.SIMPLIFIED_CHINESE) },
            label = { Text("简体中文") }
        )
        FilterChip(
            selected = current == AppLanguage.ENGLISH,
            onClick = { onSelect(AppLanguage.ENGLISH) },
            label = { Text("English") }
        )
    }
}

@Composable
private fun AboutSection() {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("1.0.0")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.appName, style = MaterialTheme.typography.titleMedium)
            Text(strings.settingsAboutDescription, style = MaterialTheme.typography.bodySmall)
            Text(strings.settingsAboutVersion(versionName ?: "1.0.0"), style = MaterialTheme.typography.bodySmall)
            Text(strings.settingsAboutOpenSource, style = MaterialTheme.typography.bodySmall)
        }
    }
}
