package com.gptimage.playground.ui.screens.workbench

import android.graphics.Bitmap
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
    val providerConfigured: Boolean = false,
    /** 用户是否启用流式预览。仅当 [model] 的 [ImageModelDefinition.supportsStreaming] 为 true 时此开关才会显示与生效。 */
    val streamingEnabled: Boolean = true,
    /** 是否正在流式生成中。与 [isGenerating] 区分：[isGenerating] 表示「任意生成中」（流式或非流式），本字段专门标记流式。 */
    val isStreaming: Boolean = false,
    /** 流式预览的最新部分图像 Bitmap。每次 [GenerationStreamEvent.Partial] 到达时更新。 */
    val streamingPreview: Bitmap? = null,
    /** 当前预览对应的部分图像索引（1/2/3）。 */
    val streamingPartialIndex: Int = 0,
    /** 流式生成开始时间戳，用于显示「已耗时 N 秒」。 */
    val streamingStartedAt: Long = 0L
)

data class ReferenceImageUi(
    val uri: Uri,
    val name: String,
    val mimeType: String
)
