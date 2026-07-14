package com.gptimage.playground.ui.screens.workbench

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.ServiceLocator
import com.gptimage.playground.data.model.AppConfig
import com.gptimage.playground.data.model.EditRequest
import com.gptimage.playground.data.model.GenerationRequest
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.model.ImageModelCatalog
import com.gptimage.playground.data.model.ImageModelDefinition
import com.gptimage.playground.data.model.ImageProviders
import com.gptimage.playground.data.model.ProviderCredentials
import com.gptimage.playground.data.model.ReferenceImage as DataReferenceImage
import com.gptimage.playground.data.network.ProviderException
import com.gptimage.playground.data.repository.GenerationOutcome as RepoGenerationOutcome
import com.gptimage.playground.data.repository.GenerationStreamEvent
import com.gptimage.playground.data.repository.ImageGenerationRepository
import com.gptimage.playground.data.repository.ReferenceImages
import com.gptimage.playground.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

class WorkbenchViewModel(
    application: Application,
    private val settings: SettingsRepository,
    private val generationRepository: ImageGenerationRepository
) : AndroidViewModel(application) {

    private val local = MutableStateFlow(WorkbenchUiState())

    private val maskData: DataReferenceImage?
        get() = state.value.maskSavedBytes?.let { bytes ->
            DataReferenceImage(
                name = "generated-mask.png",
                mimeType = "image/png",
                data = bytes
            )
        }

    companion object {
        /** 笔刷半径下限（像素），与 Web 端一致。 */
        const val MIN_BRUSH_SIZE = 5
        /** 笔刷半径上限（像素），与 Web 端一致。 */
        const val MAX_BRUSH_SIZE = 100
    }

    /** 当前流式生成的 Job，用于 [cancelGenerate] 主动取消。 */
    private var generateJob: Job? = null

    private val configFlow: StateFlow<AppConfig> = settings.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    val state: StateFlow<WorkbenchUiState> = combine(local, configFlow) { current, config ->
        // 合并内置 + 自定义模型（自定义来自用户配置，加载时已归一化）
        val availableModels = com.gptimage.playground.data.model.CustomImageModels
            .mergeWithBuiltin(config.customImageModels)
        val resolvedModel = current.model ?: availableModels.firstOrNull { it.id == config.defaultModelId }
            ?: availableModels.firstOrNull()
        val providerForModel = resolvedModel?.provider ?: ImageProviders.OPENAI
        val providerConfigured = config.credentialsFor(providerForModel).isConfigured
        // 仅当模型 supportsStreaming 且 provider == OPENAI 时才允许 streamingEnabled 生效；
        // 其他情况强制为 false（避免 UI 显示流式开关但实际回退到非流式的不一致体验）
        val effectiveStreamingEnabled = current.streamingEnabled &&
            resolvedModel?.supportsStreaming == true &&
            providerForModel == ImageProviders.OPENAI
        current.copy(
            availableModels = availableModels,
            model = resolvedModel,
            providerConfigured = providerConfigured,
            streamingEnabled = effectiveStreamingEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WorkbenchUiState()
    )

    fun updatePrompt(value: String) = local.update { it.copy(prompt = value) }

    fun selectModel(model: ImageModelDefinition) = local.update {
        it.copy(model = model, size = model.defaultSize ?: model.sizePresets?.square)
    }

    fun setAdvancedExpanded(expanded: Boolean) = local.update { it.copy(advancedExpanded = expanded) }

    fun setCount(count: Int) = local.update {
        it.copy(count = count.coerceIn(1, 4))
    }

    fun setSize(size: String?) = local.update { it.copy(size = size) }

    fun setQuality(quality: String?) = local.update { it.copy(quality = quality) }

    fun setOutputFormat(format: String?) = local.update { it.copy(outputFormat = format) }

    fun setBackground(background: String?) = local.update { it.copy(background = background) }

    fun setModeration(moderation: String?) = local.update { it.copy(moderation = moderation) }

    fun setStreamingEnabled(enabled: Boolean) = local.update { it.copy(streamingEnabled = enabled) }

    fun addReference(uri: Uri, name: String, mimeType: String) = local.update {
        it.copy(referenceImages = it.referenceImages + ReferenceImageUi(uri, name, mimeType))
    }

    fun removeReferenceAt(index: Int) = local.update {
        val updated = it.referenceImages.toMutableList()
        if (index in updated.indices) updated.removeAt(index)
        // 参考图变化时清掉 mask state：mask 必须与第一张参考图尺寸一致
        it.copy(
            referenceImages = updated,
            maskSourceBitmap = null,
            maskSourceWidth = 0,
            maskSourceHeight = 0,
            maskDrawnPoints = emptyList(),
            maskSavedBytes = null,
            maskSaved = false,
            maskEditorVisible = false
        )
    }

    fun clearReferences() = local.update {
        it.copy(
            referenceImages = emptyList(),
            maskSourceBitmap = null,
            maskSourceWidth = 0,
            maskSourceHeight = 0,
            maskDrawnPoints = emptyList(),
            maskSavedBytes = null,
            maskSaved = false,
            maskEditorVisible = false
        )
    }

    fun useHistoryItemAsReference(item: HistoryItem) {
        val file = java.io.File(item.imagePath)
        if (file.exists()) {
            val uri = Uri.fromFile(file)
            local.update {
                it.copy(
                    referenceImages = it.referenceImages + ReferenceImageUi(
                        uri = uri,
                        name = file.name,
                        mimeType = guessMimeType(file)
                    )
                )
            }
        }
    }

    fun sendToEdit(item: HistoryItem) {
        useHistoryItemAsReference(item)
        local.update { it.copy(prompt = item.prompt) }
    }

    // ------------------------------------------------------------------
    // Mask 蒙版编辑
    // ------------------------------------------------------------------

    /**
     * 切换蒙版编辑器可见性。展开前会异步加载第一张参考图的 Bitmap + 尺寸。
     * 如果当前模型不支持 mask（[ImageModelDefinition.supportsMask]）或没有参考图，则忽略。
     */
    fun setMaskEditorVisible(visible: Boolean) {
        val current = state.value
        if (visible) {
            val model = current.model
            if (model?.supportsMask != true) return
            if (current.referenceImages.isEmpty()) return
            val targetUri = current.referenceImages.first().uri
            // 异步加载源图 Bitmap
            viewModelScope.launch {
                val bitmap = loadReferenceBitmap(current.referenceImages.first())
                if (bitmap != null) {
                    // 加载过程中用户可能已删除/替换了参考图，校验后再更新 UI
                    val stillValid = state.value.referenceImages.firstOrNull()?.uri == targetUri
                    if (stillValid) {
                        local.update {
                            it.copy(
                                maskEditorVisible = true,
                                maskSourceBitmap = bitmap,
                                maskSourceWidth = bitmap.width,
                                maskSourceHeight = bitmap.height
                            )
                        }
                    }
                } else {
                    local.update {
                        it.copy(error = "无法读取参考图，蒙版编辑器无法打开")
                    }
                }
            }
        } else {
            local.update { it.copy(maskEditorVisible = false) }
        }
    }

    fun setMaskBrushSize(size: Int) = local.update {
        it.copy(maskBrushSize = size.coerceIn(MIN_BRUSH_SIZE, MAX_BRUSH_SIZE))
    }

    /**
     * 添加一个笔触点位。坐标必须基于源图原始像素空间（由 UI 层做换算）。
     * 添加点位会自动把 [WorkbenchUiState.maskSaved] 置为 false（点位变化后旧 mask 失效）。
     */
    fun addMaskPoint(x: Float, y: Float) = local.update {
        it.copy(
            maskDrawnPoints = it.maskDrawnPoints + DrawnPoint(x, y, it.maskBrushSize.toFloat()),
            maskSaved = false,
            maskSavedBytes = null
        )
    }

    /**
     * 在两点间按笔刷半径 1/4 步长插值，避免拖动太快出现离散点。
     * 对齐 Web 端 drawLine 算法。
     */
    fun addMaskLine(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val brush = state.value.maskBrushSize.toFloat()
        val dx = toX - fromX
        val dy = toY - fromY
        val dist = hypot(dx, dy)
        val step = max(1f, brush / 4f)
        val angle = atan2(dy, dx)
        val newPoints = mutableListOf<DrawnPoint>()
        var i = step
        while (i < dist) {
            val x = fromX + cos(angle) * i
            val y = fromY + sin(angle) * i
            newPoints += DrawnPoint(x, y, brush)
            i += step
        }
        newPoints += DrawnPoint(toX, toY, brush)
        if (newPoints.isNotEmpty()) {
            local.update {
                it.copy(
                    maskDrawnPoints = it.maskDrawnPoints + newPoints,
                    maskSaved = false,
                    maskSavedBytes = null
                )
            }
        }
    }

    /** 撤销最近一笔（删除最后一个点位）。Web 端未实现，Android 端补齐。 */
    fun undoLastMaskPoint() = local.update {
        if (it.maskDrawnPoints.isEmpty()) return@update it
        val updated = it.maskDrawnPoints.dropLast(1)
        it.copy(
            maskDrawnPoints = updated,
            maskSaved = false,
            maskSavedBytes = null
        )
    }

    fun clearMask() = local.update {
        it.copy(
            maskDrawnPoints = emptyList(),
            maskSavedBytes = null,
            maskSaved = false
        )
    }

    /**
     * 把当前 [WorkbenchUiState.maskDrawnPoints] 生成 PNG 字节并保存到 [WorkbenchUiState.maskSavedBytes]。
     * 算法与 Web 端 `generateAndSaveMask` 一致：
     *   1. 创建黑底 Bitmap（与源图同尺寸）
     *   2. 用 [PorterDuff.Mode.CLEAR] 在 [Canvas.saveLayer] 离屏图层上画圆，把笔触区域挖空为透明
     *   3. 压缩为 PNG
     * OpenAI `/images/edits` 接受「透明像素 = 要重绘区域」的 mask，与该算法匹配。
     */
    fun saveMask() {
        val current = state.value
        val w = current.maskSourceWidth
        val h = current.maskSourceHeight
        val points = current.maskDrawnPoints
        if (w <= 0 || h <= 0 || points.isEmpty()) return

        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.BLACK)
                // PorterDuff.Mode.CLEAR 必须配合 saveLayer 才能正确挖空（否则会把整个画布清掉）
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
                canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
                points.forEach { p ->
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                canvas.restore()
                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                bmp.recycle()
                baos.toByteArray()
            }
            local.update {
                it.copy(maskSavedBytes = bytes, maskSaved = true)
            }
        }
    }

    private suspend fun loadReferenceBitmap(ref: ReferenceImageUi): Bitmap? =
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                context.contentResolver.openInputStream(ref.uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } catch (e: Throwable) {
                null
            }
        }

    fun generate() {
        val current = state.value
        val model = current.model ?: return
        if (current.prompt.isBlank()) return
        if (!current.providerConfigured) return

        // 提交前校验：mask 编辑模式下若用户画了点位但还没保存，阻止提交
        if (current.maskEditorVisible && current.maskDrawnPoints.isNotEmpty() && !current.maskSaved) {
            local.update { it.copy(error = "提交前请先保存已绘制的蒙版") }
            return
        }

        // 创建一个新的对话回合，记录用户当前 prompt + 参考图快照 + 模型标签
        val turn = ChatTurn(
            id = "turn-${System.currentTimeMillis()}",
            prompt = current.prompt,
            referenceImageUris = current.referenceImages.map { it.uri },
            modelLabel = model.label,
            createdAt = System.currentTimeMillis(),
            status = TurnStatus.GENERATING,
            resultItem = null,
            errorMessage = null,
            streamingPreviewBitmap = null,
            streamingPartialIndex = 0,
            streamingStartedAt = 0L
        )
        local.update { it.copy(turns = it.turns + turn) }

        // 仅 OpenAI 系列且 model.supportsStreaming 时走流式；其他情况一律走非流式
        val useStreaming = current.streamingEnabled &&
            model.supportsStreaming &&
            model.provider == ImageProviders.OPENAI

        if (useStreaming) {
            startStreamingGenerate(current, model)
        } else {
            startNonStreamingGenerate(current, model)
        }
    }

    /**
     * 重试一个失败/取消的回合：把该 turn 的 prompt + 参考图作为「草稿」回填到输入区，然后触发新一轮 generate。
     * 注意：因为参考图快照只存了 Uri，重试时直接 add 回 referenceImages，如果用户期间清除了缓存可能加载失败。
     */
    fun retryTurn(turn: ChatTurn) {
        if (state.value.isGenerating) return
        local.update {
            it.copy(
                prompt = turn.prompt,
                referenceImages = turn.referenceImageUris.map { uri ->
                    ReferenceImageUi(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "reference", "image/png")
                }
            )
        }
        generate()
    }

    /** 清空所有对话回合与最近的错误/结果镜像，重置为初始状态（保留 prompt 草稿与高级参数）。 */
    fun clearTurns() {
        local.update {
            it.copy(
                turns = emptyList(),
                lastResult = null,
                error = null,
                isGenerating = false,
                isStreaming = false,
                streamingPreview = null,
                streamingPartialIndex = 0,
                streamingStartedAt = 0L
            )
        }
        generateJob?.cancel()
        generateJob = null
    }

    /**
     * 更新 turns 列表中的最后一项。如果 turns 为空则 no-op。
     * 用于流式 / 非流式生成过程中实时把 partial / completed / failure 同步到对应的 turn 气泡。
     */
    private fun updateLastTurn(transform: (ChatTurn) -> ChatTurn) {
        local.update { state ->
            val turns = state.turns
            if (turns.isEmpty()) {
                state
            } else {
                val lastIndex = turns.lastIndex
                val updated = transform(turns[lastIndex])
                state.copy(turns = turns.toMutableList().apply { set(lastIndex, updated) })
            }
        }
    }

    /**
     * 取消正在进行的流式生成。同时设置 [WorkbenchUiState.isStreaming] = false、[WorkbenchUiState.isGenerating] = false。
     * 如果当前没有进行中的流式任务，则无操作。
     */
    fun cancelGenerate() {
        generateJob?.cancel()
        generateJob = null
        local.update {
            it.copy(
                isGenerating = false,
                isStreaming = false,
                streamingPreview = null,
                streamingPartialIndex = 0,
                streamingStartedAt = 0L
            )
        }
        updateLastTurn { turn ->
            if (turn.status == TurnStatus.GENERATING) {
                turn.copy(
                    status = TurnStatus.CANCELED,
                    streamingPreviewBitmap = null,
                    streamingPartialIndex = 0,
                    streamingStartedAt = 0L
                )
            } else turn
        }
    }

    fun clearError() = local.update { it.copy(error = null) }

    private fun startNonStreamingGenerate(current: WorkbenchUiState, model: ImageModelDefinition) {
        viewModelScope.launch {
            local.update { it.copy(isGenerating = true, error = null) }
            updateLastTurn { it.copy(streamingStartedAt = System.currentTimeMillis()) }
            val config = configFlow.value
            val credentials = config.credentialsFor(model.provider)

            val outcome = if (current.referenceImages.isEmpty()) {
                generateTextToImage(current, model, credentials)
            } else {
                editImage(current, model, credentials)
            }

            when (outcome) {
                is RepoGenerationOutcome.Success -> {
                    local.update {
                        it.copy(isGenerating = false, lastResult = outcome.item, error = null)
                    }
                    updateLastTurn {
                        it.copy(
                            status = TurnStatus.SUCCESS,
                            resultItem = outcome.item,
                            streamingPreviewBitmap = null,
                            streamingPartialIndex = 0,
                            streamingStartedAt = 0L
                        )
                    }
                }
                is RepoGenerationOutcome.Failure -> {
                    val message = errorMessage(outcome.error)
                    local.update { it.copy(isGenerating = false, error = message) }
                    updateLastTurn {
                        it.copy(
                            status = TurnStatus.ERROR,
                            errorMessage = message,
                            streamingPreviewBitmap = null,
                            streamingPartialIndex = 0,
                            streamingStartedAt = 0L
                        )
                    }
                }
            }
        }
    }

    private fun startStreamingGenerate(current: WorkbenchUiState, model: ImageModelDefinition) {
        // 取消之前未完成的流式任务（理论上不应有，但保险起见）
        generateJob?.cancel()
        val config = configFlow.value
        val credentials = config.credentialsFor(model.provider)
        val startedAt = System.currentTimeMillis()

        val streamFlow = if (current.referenceImages.isEmpty()) {
            val request = GenerationRequest(
                model = model,
                prompt = current.prompt,
                n = current.count,
                size = current.size,
                quality = current.quality,
                outputFormat = current.outputFormat,
                background = current.background,
                moderation = current.moderation,
                providerOptions = model.providerOptions
            )
            generationRepository.generateStream(request, credentials, model.provider)
        } else {
            // 编辑流式需要先加载参考图
            // 注意：参考图加载在 IO 上下文里做，避免主线程阻塞
            // 这里用一个临时 flow 包装：先加载参考图，再发起 editStream
            kotlinx.coroutines.flow.flow {
                val references = loadReferenceImages(current.referenceImages)
                if (references.isEmpty()) {
                    emit(GenerationStreamEvent.Failure(
                        ProviderException(ProviderException.Kind.BAD_REQUEST, "无法读取参考图")
                    ))
                    return@flow
                }
                val request = EditRequest(
                    model = model,
                    prompt = current.prompt,
                    referenceImages = references,
                    mask = maskData,
                    n = current.count,
                    size = current.size,
                    quality = current.quality,
                    providerOptions = model.providerOptions
                )
                generationRepository.editStream(request, credentials, model.provider).collect { emit(it) }
            }
        }

        generateJob = viewModelScope.launch {
            local.update {
                it.copy(
                    isGenerating = true,
                    isStreaming = true,
                    error = null,
                    streamingPreview = null,
                    streamingPartialIndex = 0,
                    streamingStartedAt = startedAt
                )
            }
            updateLastTurn {
                it.copy(
                    status = TurnStatus.GENERATING,
                    streamingStartedAt = startedAt
                )
            }

            try {
                streamFlow.collect { event ->
                    when (event) {
                        is GenerationStreamEvent.Partial -> {
                            val bitmap = decodeBase64ToBitmap(event.b64Json)
                            local.update {
                                it.copy(
                                    streamingPreview = bitmap,
                                    streamingPartialIndex = event.partialImageIndex
                                )
                            }
                            updateLastTurn {
                                it.copy(
                                    streamingPreviewBitmap = bitmap,
                                    streamingPartialIndex = event.partialImageIndex
                                )
                            }
                        }

                        is GenerationStreamEvent.Completed -> {
                            // 每张图完成都会写盘 + 写 Room，这里只需把最新 item 推到 UI
                            local.update {
                                it.copy(lastResult = event.item)
                            }
                            updateLastTurn {
                                it.copy(resultItem = event.item)
                            }
                        }

                        is GenerationStreamEvent.Failure -> {
                            val message = errorMessage(event.error)
                            local.update { it.copy(error = message) }
                            updateLastTurn {
                                it.copy(
                                    status = TurnStatus.ERROR,
                                    errorMessage = message,
                                    streamingPreviewBitmap = null,
                                    streamingPartialIndex = 0
                                )
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 用户主动取消，不做错误处理
                throw e
            } catch (e: Throwable) {
                local.update {
                    it.copy(error = e.message ?: "流式生成失败")
                }
                updateLastTurn {
                    it.copy(
                        status = TurnStatus.ERROR,
                        errorMessage = e.message ?: "流式生成失败",
                        streamingPreviewBitmap = null,
                        streamingPartialIndex = 0
                    )
                }
            } finally {
                local.update {
                    it.copy(
                        isGenerating = false,
                        isStreaming = false,
                        streamingPreview = null,
                        streamingPartialIndex = 0,
                        streamingStartedAt = 0L
                    )
                }
                // 流式 collect 正常结束 + 没有 ERROR/CANCELED → 标记为 SUCCESS（保留 resultItem 如果有）
                updateLastTurn { turn ->
                    if (turn.status == TurnStatus.GENERATING) {
                        turn.copy(
                            status = TurnStatus.SUCCESS,
                            streamingPreviewBitmap = null,
                            streamingPartialIndex = 0,
                            streamingStartedAt = 0L
                        )
                    } else turn
                }
                generateJob = null
            }
        }
    }

    private suspend fun decodeBase64ToBitmap(b64: String): android.graphics.Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Throwable) {
                null
            }
        }

    private suspend fun generateTextToImage(
        current: WorkbenchUiState,
        model: ImageModelDefinition,
        credentials: ProviderCredentials
    ): RepoGenerationOutcome {
        val request = GenerationRequest(
            model = model,
            prompt = current.prompt,
            n = current.count,
            size = current.size,
            quality = current.quality,
            outputFormat = current.outputFormat,
            background = current.background,
            moderation = current.moderation,
            providerOptions = model.providerOptions
        )
        return generationRepository.generate(request, credentials, model.provider)
    }

    private suspend fun editImage(
        current: WorkbenchUiState,
        model: ImageModelDefinition,
        credentials: ProviderCredentials
    ): RepoGenerationOutcome {
        val references = loadReferenceImages(current.referenceImages)
        if (references.isEmpty()) {
            return RepoGenerationOutcome.Failure(
                ProviderException(ProviderException.Kind.BAD_REQUEST, "无法读取参考图")
            )
        }
        val request = EditRequest(
            model = model,
            prompt = current.prompt,
            referenceImages = references,
            mask = maskData,
            n = current.count,
            size = current.size,
            quality = current.quality,
            providerOptions = model.providerOptions
        )
        return generationRepository.edit(request, credentials, model.provider)
    }

    private suspend fun loadReferenceImages(uris: List<ReferenceImageUi>): List<DataReferenceImage> =
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            uris.mapNotNull { ref ->
                try {
                    val tempFile = File(context.cacheDir, "ref_${System.currentTimeMillis()}_${ref.name}")
                    context.contentResolver.openInputStream(ref.uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@mapNotNull null
                    ReferenceImages.fromFile(tempFile)
                } catch (e: Throwable) {
                    null
                }
            }
        }

    private fun errorMessage(error: ProviderException): String = when (error.kind) {
        ProviderException.Kind.NETWORK -> "网络异常，请检查网络后重试"
        ProviderException.Kind.AUTH -> "API Key 无效或未配置"
        ProviderException.Kind.RATE_LIMIT -> "请求过于频繁，请稍后重试"
        ProviderException.Kind.BAD_REQUEST -> "请求参数有误：${error.message}"
        ProviderException.Kind.SERVER -> "供应商服务异常，请稍后再试"
        ProviderException.Kind.PARSE -> "返回数据格式异常"
        ProviderException.Kind.UNKNOWN -> error.message ?: "未知错误"
    }

    private fun guessMimeType(file: java.io.File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "png" -> "image/png"
            else -> "image/png"
        }
    }
}

class WorkbenchViewModelFactory(
    private val locator: ServiceLocator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkbenchViewModel(
            application = locator.application,
            settings = locator.settingsRepository,
            generationRepository = locator.imageGenerationRepository
        ) as T
    }
}
