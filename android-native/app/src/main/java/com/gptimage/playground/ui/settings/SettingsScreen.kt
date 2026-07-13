package com.gptimage.playground.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gptimage.playground.R
import com.gptimage.playground.data.model.AppLanguage
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageQuality
import com.gptimage.playground.data.model.ProviderInstance
import com.gptimage.playground.data.model.ProviderKind
import com.gptimage.playground.data.model.ThemeModeConfig
import com.gptimage.playground.util.LocaleHelper
import androidx.activity.ComponentActivity

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val onLanguageChange = { lang: AppLanguage ->
        viewModel.setLanguage(lang)
        // Persist to the synchronous locale cache and recreate so the new
        // language applies app-wide (mirrors the web app's appLanguage switch).
        LocaleHelper.saveLanguageCode(context, lang.code)
        (context as? ComponentActivity)?.recreate()
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )

            AppearanceSection(state, viewModel, onLanguageChange)
            ProvidersSection(state, viewModel)
            DefaultsSection(state, viewModel)
            AboutSection()

            Spacer(Modifier.size(24.dp))
        }
    }

    if (state.providerDialogOpen) {
        state.editingProvider?.let { draft ->
            ProviderEditDialog(
                draft = draft,
                onDismiss = viewModel::closeProviderDialog,
                onSave = viewModel::saveProvider
            )
        }
    }
}

@Composable
private fun SectionCard(titleRes: Int, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun AppearanceSection(
    state: SettingsUiState,
    vm: SettingsViewModel,
    onLanguageChange: (AppLanguage) -> Unit
) {
    SectionCard(titleRes = R.string.settings_appearance) {
        // Language
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_language))
            SingleChoiceSegmentedButtonRow {
                AppLanguage.values().forEachIndexed { index, lang ->
                    SegmentedButton(
                        selected = state.config.resolvedLanguage == lang,
                        onClick = { onLanguageChange(lang) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.values().size)
                    ) { Text(lang.nativeName) }
                }
            }
        }
        // Theme
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_theme))
            SingleChoiceSegmentedButtonRow {
                val modes = listOf(
                    ThemeModeConfig.Light to R.string.settings_theme_light,
                    ThemeModeConfig.Dark to R.string.settings_theme_dark,
                    ThemeModeConfig.System to R.string.settings_theme_system
                )
                modes.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = state.config.resolvedThemeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                    ) { Text(stringResource(labelRes)) }
                }
            }
        }
    }
}

@Composable
private fun ProvidersSection(state: SettingsUiState, vm: SettingsViewModel) {
    SectionCard(titleRes = R.string.settings_providers) {
        if (state.config.providerInstances.isEmpty()) {
            Text(
                stringResource(R.string.common_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.config.providerInstances.forEach { provider ->
                ProviderRow(
                    provider = provider,
                    isSelected = state.config.selectedProviderInstanceId == provider.id,
                    onSelect = { vm.selectProvider(provider.id) },
                    onToggle = { vm.toggleProviderEnabled(provider.id) },
                    onEdit = { vm.openEditProvider(provider) },
                    onDelete = { vm.deleteProvider(provider.id) }
                )
            }
        }
        OutlinedButton(
            onClick = vm::openAddProvider,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.provider_add))
        }
    }
}

@Composable
private fun ProviderRow(
    provider: ProviderInstance,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(provider.name, fontWeight = FontWeight.Medium)
                Text(
                    provider.modelId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    provider.effectiveBaseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Switch(checked = provider.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.common_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
            }
        }
    }
}

@Composable
private fun DefaultsSection(state: SettingsUiState, vm: SettingsViewModel) {
    SectionCard(titleRes = R.string.workbench_advanced) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.workbench_quality))
            SingleChoiceSegmentedButtonRow {
                val opts = listOf(ImageQuality.Auto, ImageQuality.Low, ImageQuality.Medium, ImageQuality.High)
                opts.forEachIndexed { index, q ->
                    SegmentedButton(
                        selected = state.config.resolvedQuality == q,
                        onClick = { vm.setDefaultQuality(q) },
                        shape = SegmentedButtonDefaults.itemShape(index, opts.size)
                    ) { Text(q.name.take(3)) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.workbench_format))
            SingleChoiceSegmentedButtonRow {
                val formats = listOf(ImageOutputFormat.Png, ImageOutputFormat.Jpeg, ImageOutputFormat.Webp)
                formats.forEachIndexed { index, f ->
                    SegmentedButton(
                        selected = state.config.resolvedFormat == f,
                        onClick = { vm.setDefaultFormat(f) },
                        shape = SegmentedButtonDefaults.itemShape(index, formats.size)
                    ) { Text(f.name) }
                }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    SectionCard(titleRes = R.string.settings_about) {
        Text(
            stringResource(R.string.settings_about_desc),
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.settings_about_version))
            Text(
                "3.0.0-native",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProviderEditDialog(
    draft: ProviderInstance,
    onDismiss: () -> Unit,
    onSave: (ProviderInstance) -> Unit
) {
    var name by remember(draft.id) { mutableStateOf(draft.name) }
    var kind by remember(draft.id) { mutableStateOf(draft.kind) }
    var apiKey by remember(draft.id) { mutableStateOf(draft.apiKey) }
    var baseUrl by remember(draft.id) { mutableStateOf(draft.baseUrl) }
    var modelId by remember(draft.id) { mutableStateOf(draft.modelId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.provider_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.provider_name)) },
                    singleLine = true,
                    isError = name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.provider_kind))
                    SingleChoiceSegmentedButtonRow {
                        ProviderKind.values().forEachIndexed { index, k ->
                            SegmentedButton(
                                selected = kind == k,
                                onClick = { kind = k },
                                shape = SegmentedButtonDefaults.itemShape(index, ProviderKind.values().size)
                            ) { Text(kindLabel(k)) }
                        }
                    }
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.provider_api_key)) },
                    singleLine = true,
                    isError = apiKey.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.provider_base_url)) },
                    placeholder = { Text(stringResource(R.string.provider_base_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text(stringResource(R.string.provider_model_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && apiKey.isNotBlank()) {
                        onSave(draft.copy(name = name, kind = kind, apiKey = apiKey, baseUrl = baseUrl, modelId = modelId))
                    }
                },
                enabled = name.isNotBlank() && apiKey.isNotBlank()
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun kindLabel(kind: ProviderKind): String = when (kind) {
    ProviderKind.OpenAICompatible -> stringResource(R.string.provider_kind_openai)
    ProviderKind.Gemini -> stringResource(R.string.provider_kind_gemini)
}
