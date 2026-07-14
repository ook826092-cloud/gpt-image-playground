package com.gptimage.playground.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.gptimage.playground.data.model.ImageModelCatalog
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        if (saved) {
            snackbarHostState.showSnackbar(strings.settingsSaved)
            viewModel.consumeSavedEvent()
        }
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
            ImageModelCatalog.groupByProvider().forEach { (provider, _) ->
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
                current = config.defaultModelId,
                onSelect = { viewModel.setDefaultModel(it) }
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
                    androidx.compose.material3.TextButton(onClick = { showKey = !showKey }) {
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
                androidx.compose.material3.TextButton(onClick = {
                    keyInput = ""
                    urlInput = ""
                }) { Text(strings.settingsClearApiKey) }
                androidx.compose.material3.Button(onClick = { onSave(keyInput, urlInput) }) {
                    Text(strings.settingsSave)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(current: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ImageModelCatalog.groupByProvider().forEach { (provider, models) ->
            Text(
                text = ImageProviders.label(provider),
                style = MaterialTheme.typography.labelMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                models.take(4).forEach { model ->
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
            Text(strings.settingsAboutVersion(versionName), style = MaterialTheme.typography.bodySmall)
            Text(strings.settingsAboutOpenSource, style = MaterialTheme.typography.bodySmall)
        }
    }
}
