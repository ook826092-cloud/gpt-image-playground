package com.gptimage.playground.ui.screens.album

import android.app.Application
import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gptimage.playground.ServiceLocator
import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AlbumViewModel(
    application: Application,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    val images: StateFlow<List<HistoryItem>> = historyRepository.all.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /** 「保存到相册」的一次性结果提示（成功/失败文案），UI 消费后清空。 */
    private val _saveToGalleryStatus = MutableStateFlow<String?>(null)
    val saveToGalleryStatus: StateFlow<String?> = _saveToGalleryStatus.asStateFlow()

    /** 「复制 prompt」的一次性结果提示。 */
    private val _copyPromptStatus = MutableStateFlow<String?>(null)
    val copyPromptStatus: StateFlow<String?> = _copyPromptStatus.asStateFlow()

    fun consumeSaveToGalleryStatus() { _saveToGalleryStatus.value = null }
    fun consumeCopyPromptStatus() { _copyPromptStatus.value = null }

    fun delete(item: HistoryItem) {
        viewModelScope.launch { historyRepository.delete(item) }
    }

    /**
     * 把 [item] 复制到系统相册（Pictures/GPT Image Playground/）。
     * - Android 10+ 走 MediaStore + scoped storage，无需权限
     * - Android 9 及以下回退到写入外部存储目录（已要求 WRITE_EXTERNAL_STORAGE 权限）
     */
    fun saveToGallery(item: HistoryItem) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching { saveImageToMediaStore(ctx, File(item.imagePath)) }
            }
            // 返回的不是文案本身；UI 会根据是否成功和 strings 决定文案。
            //   成功："__ok__"，失败：具体错误描述
            _saveToGalleryStatus.value = if (result.isSuccess) "__ok__"
                else (result.exceptionOrNull()?.message ?: "save failed")
        }
    }

    fun copyPrompt(item: HistoryItem) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("prompt", item.prompt))
            _copyPromptStatus.value = "__ok__"
        }
    }

    private fun saveImageToMediaStore(context: Context, source: File): Boolean {
        if (!source.exists()) error("source file not found")
        val resolver = context.contentResolver
        val extension = source.extension.lowercase().ifEmpty { "png" }
        val mimeType = when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "png" -> "image/png"
            else -> "image/png"
        }
        val displayName = "gpt-image-${System.currentTimeMillis()}.$extension"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GPT Image Playground")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert returned null")
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: error("cannot open output stream")
            val finalize = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, finalize, null, null)
            true
        } else {
            // Android 9 及以下：直接复制到公共 Pictures 目录
            @Suppress("DEPRECATION")
            val picturesRoot = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES
            )
            val targetDir = File(picturesRoot, "GPT Image Playground").apply { if (!exists()) mkdirs() }
            val target = File(targetDir, displayName)
            source.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
            true
        }
    }
}

class AlbumViewModelFactory(
    private val locator: ServiceLocator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumViewModel(
            application = locator.application,
            historyRepository = locator.historyRepository
        ) as T
    }
}
