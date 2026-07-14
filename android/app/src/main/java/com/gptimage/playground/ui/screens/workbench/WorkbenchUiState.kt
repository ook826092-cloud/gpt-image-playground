package com.gptimage.playground.ui.screens.workbench

import android.net.Uri
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.model.ImageBackground
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageModeration
import com.gptimage.playground.data.model.ImageOutputFormat
import com.gptimage.playground.data.model.ImageQuality

data class WorkbenchUiState(
    val prompt: String = "",
    val model: ImageModelDefinition? = null,
    val availableModels: List<ImageModelDefinition> = emptyList(),
    val advancedExpanded: Boolean = false,
    val count: Int = 1,
    val size: String? = null,
    val quality: ImageQuality? = null,
    val outputFormat: ImageOutputFormat? = null,
    val background: ImageBackground? = null,
    val moderation: ImageModeration? = null,
    val referenceImages: List<ReferenceImageUi> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,
    val lastResult: HistoryItem? = null,
    val providerConfigured: Boolean = false
)

data class ReferenceImageUi(
    val uri: Uri,
    val name: String,
    val mimeType: String
)
