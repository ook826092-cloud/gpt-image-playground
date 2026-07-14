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
    val streamingStartedAt: Long = 0L,
    /** 蒙版编辑器是否展开。仅当 [model] 的 [ImageModelDefinition.supportsMask] 为 true 且至少有一张参考图时才允许展开。 */
    val maskEditorVisible: Boolean = false,
    /** 已解码的第一张参考图 Bitmap，供蒙版编辑器 Canvas 叠加显示。null 表示尚未加载或参考图已变更。 */
    val maskSourceBitmap: Bitmap? = null,
    /** 第一张参考图的原始像素尺寸（width × height）。蒙版 PNG 必须与源图同尺寸，OpenAI 会校验。 */
    val maskSourceWidth: Int = 0,
    val maskSourceHeight: Int = 0,
    /** 用户在蒙版画布上累积的笔触点位（坐标基于源图原始像素空间，与 [maskSourceWidth]/[maskSourceHeight] 对齐）。 */
    val maskDrawnPoints: List<DrawnPoint> = emptyList(),
    /** 笔刷半径（像素），范围 5–100，默认 20（与 Web 端一致）。 */
    val maskBrushSize: Int = 20,
    /** 已生成并保存的蒙版 PNG 字节。null 表示尚未保存或已清除。提交时封装为 [com.gptimage.playground.data.model.ReferenceImage]。 */
    val maskSavedBytes: ByteArray? = null,
    /** 蒙版是否已保存（[maskSavedBytes] 非空）。提交前若有未保存的点位会阻止提交。 */
    val maskSaved: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WorkbenchUiState) return false
        // ByteArray 不能用默认 == 比较，需要 contentEquals；其余字段用 data class 生成的 equals
        return prompt == other.prompt &&
            model == other.model &&
            availableModels == other.availableModels &&
            advancedExpanded == other.advancedExpanded &&
            count == other.count &&
            size == other.size &&
            quality == other.quality &&
            outputFormat == other.outputFormat &&
            background == other.background &&
            moderation == other.moderation &&
            referenceImages == other.referenceImages &&
            isGenerating == other.isGenerating &&
            error == other.error &&
            lastResult == other.lastResult &&
            providerConfigured == other.providerConfigured &&
            streamingEnabled == other.streamingEnabled &&
            isStreaming == other.isStreaming &&
            streamingPreview == other.streamingPreview &&
            streamingPartialIndex == other.streamingPartialIndex &&
            streamingStartedAt == other.streamingStartedAt &&
            maskEditorVisible == other.maskEditorVisible &&
            maskSourceBitmap == other.maskSourceBitmap &&
            maskSourceWidth == other.maskSourceWidth &&
            maskSourceHeight == other.maskSourceHeight &&
            maskDrawnPoints == other.maskDrawnPoints &&
            maskBrushSize == other.maskBrushSize &&
            maskSaved == other.maskSaved &&
            maskSavedBytes.contentEquals(other.maskSavedBytes)
    }

    override fun hashCode(): Int {
        var result = prompt.hashCode()
        result = 31 * result + (model?.hashCode() ?: 0)
        result = 31 * result + availableModels.hashCode()
        result = 31 * result + advancedExpanded.hashCode()
        result = 31 * result + count
        result = 31 * result + (size?.hashCode() ?: 0)
        result = 31 * result + (quality?.hashCode() ?: 0)
        result = 31 * result + (outputFormat?.hashCode() ?: 0)
        result = 31 * result + (background?.hashCode() ?: 0)
        result = 31 * result + (moderation?.hashCode() ?: 0)
        result = 31 * result + referenceImages.hashCode()
        result = 31 * result + isGenerating.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (lastResult?.hashCode() ?: 0)
        result = 31 * result + providerConfigured.hashCode()
        result = 31 * result + streamingEnabled.hashCode()
        result = 31 * result + isStreaming.hashCode()
        result = 31 * result + (streamingPreview?.hashCode() ?: 0)
        result = 31 * result + streamingPartialIndex
        result = 31 * result + streamingStartedAt.hashCode()
        result = 31 * result + maskEditorVisible.hashCode()
        result = 31 * result + (maskSourceBitmap?.hashCode() ?: 0)
        result = 31 * result + maskSourceWidth
        result = 31 * result + maskSourceHeight
        result = 31 * result + maskDrawnPoints.hashCode()
        result = 31 * result + maskBrushSize
        result = 31 * result + maskSaved.hashCode()
        result = 31 * result + (maskSavedBytes?.contentHashCode() ?: 0)
        return result
    }
}

data class ReferenceImageUi(
    val uri: Uri,
    val name: String,
    val mimeType: String
)

/**
 * 蒙版画布上的单个笔触点位（密集采样的圆点）。坐标基于源图原始像素空间。
 *
 * 与 Web 端 `DrawnPoint` 一致：把每条笔触密集采样成点数组而非路径，便于重渲染 + 重新生成 PNG。
 */
data class DrawnPoint(
    val x: Float,
    val y: Float,
    val size: Float
)
