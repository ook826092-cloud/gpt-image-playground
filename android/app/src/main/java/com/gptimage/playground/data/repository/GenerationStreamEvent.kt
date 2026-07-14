package com.gptimage.playground.data.repository

import com.gptimage.playground.data.model.HistoryItem
import com.gptimage.playground.data.network.ProviderException

/**
 * 流式生成/编辑事件。在 [ImageGenerationRepository.generateStream] / [editStream] 中产生，
 * 供 [com.gptimage.playground.ui.screens.workbench.WorkbenchViewModel] 消费。
 *
 * 与底层 [com.gptimage.playground.data.network.StreamEvent] 的区别：
 *  - 这里的事件已经过持久化处理：[Completed] 携带已写入存储 + Room 的 [HistoryItem]
 *  - [Partial] 直接转发上游的 b64_json，由 ViewModel 负责解码为 [android.graphics.Bitmap]
 *  - [Failure] 携带分类好的 [ProviderException]
 */
sealed interface GenerationStreamEvent {

    /** 部分图像预览。`partialImageIndex` 通常为 1/2/3。 */
    data class Partial(
        val b64Json: String,
        val imageIndex: Int,
        val partialImageIndex: Int
    ) : GenerationStreamEvent

    /** 一张图像生成完成，已写入存储 + Room。 */
    data class Completed(
        val item: HistoryItem
    ) : GenerationStreamEvent

    /** 流终止（上游显式错误，或本地异常）。 */
    data class Failure(
        val error: ProviderException
    ) : GenerationStreamEvent
}
