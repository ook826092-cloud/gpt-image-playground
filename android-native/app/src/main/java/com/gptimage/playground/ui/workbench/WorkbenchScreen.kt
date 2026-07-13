package com.gptimage.playground.ui.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gptimage.playground.R
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageQuality
import com.gptimage.playground.data.model.ProviderInstance

@Composable
fun WorkbenchScreen(
    viewModel: WorkbenchViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.infoMessage, state.lastError) {
        val msg = when {
            state.infoMessage == "PROMPT_EMPTY" -> context.getString(R.string.workbench_prompt_empty)
            state.infoMessage == "PROVIDER_NOT_CONFIGURED" -> context.getString(R.string.workbench_provider_not_configured)
            state.lastError != null -> state.lastError
            else -> null
        }
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissInfo()
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Prompt card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.prompt,
                        onValueChange = viewModel::onPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text(stringResource(R.string.workbench_prompt_hint)) },
                        trailingIcon = {
                            if (state.prompt.isNotEmpty()) {
                                TextButton(onClick = viewModel::clearPrompt) {
                                    Text(stringResource(R.string.workbench_clear_prompt))
                                }
                            }
                        }
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.toggleAdvanced() }) {
                            Icon(
                                imageVector = if (state.advancedVisible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.workbench_advanced))
                        }
                        Button(
                            onClick = viewModel::generate,
                            enabled = !state.isGenerating
                        ) {
                            if (state.isGenerating) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.workbench_generating))
                            } else {
                                Text(stringResource(R.string.workbench_generate))
                            }
                        }
                    }
                }
            }

            if (state.advancedVisible) {
                AdvancedOptions(
                    state = state,
                    onProviderChange = viewModel::onProviderChange,
                    onCountChange = viewModel::onCountChange,
                    onSizeChange = viewModel::onSizeChange,
                    onQualityChange = viewModel::onQualityChange,
                    onFormatChange = viewModel::onFormatChange
                )
            }

            HorizontalDivider()

            // Output
            Text(
                stringResource(R.string.workbench_output),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutputArea(state)
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AdvancedOptions(
    state: WorkbenchUiState,
    onProviderChange: (String) -> Unit,
    onCountChange: (Int) -> Unit,
    onSizeChange: (String) -> Unit,
    onQualityChange: (ImageQuality) -> Unit,
    onFormatChange: (ImageOutputFormat) -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProviderSelector(
                providers = state.availableProviders,
                selectedId = state.selectedProviderId,
                onSelect = onProviderChange
            )
            OptionRow(labelRes = R.string.workbench_count) {
                SingleChoiceSegmentedButtonRow {
                    listOf(1, 2, 4).forEachIndexed { index, n ->
                        SegmentedButton(
                            selected = state.count == n,
                            onClick = { onCountChange(n) },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) { Text(n.toString()) }
                    }
                }
            }
            OptionRow(labelRes = R.string.workbench_size) {
                SizeSelector(state.size, onSizeChange)
            }
            OptionRow(labelRes = R.string.workbench_quality) {
                SingleChoiceSegmentedButtonRow {
                    val options = listOf(ImageQuality.Auto, ImageQuality.Low, ImageQuality.Medium, ImageQuality.High)
                    options.forEachIndexed { index, q ->
                        SegmentedButton(
                            selected = state.quality == q,
                            onClick = { onQualityChange(q) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)
                        ) { Text(qualityLabel(q)) }
                    }
                }
            }
            OptionRow(labelRes = R.string.workbench_format) {
                SingleChoiceSegmentedButtonRow {
                    val formats = listOf(ImageOutputFormat.Png, ImageOutputFormat.Jpeg, ImageOutputFormat.Webp)
                    formats.forEachIndexed { index, f ->
                        SegmentedButton(
                            selected = state.outputFormat == f,
                            onClick = { onFormatChange(f) },
                            shape = SegmentedButtonDefaults.itemShape(index, formats.size)
                        ) { Text(f.name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSelector(
    providers: List<ProviderInstance>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = providers.firstOrNull { it.id == selectedId } ?: providers.firstOrNull()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.workbench_provider)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            providers.forEach { provider ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(provider.name, fontWeight = FontWeight.Medium)
                            Text(
                                provider.modelId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(provider.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SizeSelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sizes = listOf("1024x1024", "1024x1536", "1536x1024")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sizes.forEach { size ->
                DropdownMenuItem(text = { Text(size) }, onClick = { onSelect(size); expanded = false })
            }
        }
    }
}

@Composable
private fun OptionRow(labelRes: Int, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
        content()
    }
}

@Composable
private fun OutputArea(state: WorkbenchUiState) {
    val images = state.lastResult?.images.orEmpty()
    if (images.isEmpty()) {
        Surface(
            Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.workbench_no_output),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 600.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images) { image ->
                val data = when {
                    !image.base64.isNullOrBlank() ->
                        "data:image/${image.outputFormat.name.lowercase()};base64,${image.base64}"
                    !image.url.isNullOrBlank() -> image.url
                    else -> null
                }
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AsyncImage(
                        model = data,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun qualityLabel(q: ImageQuality): String = when (q) {
    ImageQuality.Auto -> stringResource(R.string.quality_auto)
    ImageQuality.Low -> stringResource(R.string.quality_low)
    ImageQuality.Medium -> stringResource(R.string.quality_medium)
    ImageQuality.High -> stringResource(R.string.quality_high)
}
