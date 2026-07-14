package com.gptimage.playground.data.network

import com.gptimage.playground.data.model.ProviderUsage

/**
 * 流式图像生成事件。
 *
 * 仅 OpenAI 兼容上游（`/v1/images/generations` + `stream: true` + `partial_images: N`）支持；
 * Gemini / Stability / SenseNova / Seedream 均不支持流式，会走非流式 [com.gptimage.playground.data.repository.GenerationOutcome]。
 *
 * 上游 SSE 事件格式（两种均需支持）：
 * 1. 标准 SSE：`event: image_generation.partial_image\ndata: {...}\n\n` / `event: image_generation.completed\ndata: {...}\n\n`
 * 2. data-only：`data: {"type":"image_generation.partial_image", ...}\n\n`
 *
 * 同时兼容图像编辑流式（事件名为 `image_edit.partial_image` / `image_edit.completed`）。
 */
sealed class StreamEvent {

    /**
     * 部分图像预览。`partialImageIndex` 通常为 1/2/3，表示该图像的第几张预览。
     */
    data class PartialImage(
        val b64Json: String,
        val imageIndex: Int,
        val partialImageIndex: Int
    ) : StreamEvent()

    /**
     * 单张图像生成完成。每个 `n >= 1` 的请求会触发对应数量的 CompletedImage 事件。
     */
    data class CompletedImage(
        val b64Json: String,
        val imageIndex: Int,
        val outputFormat: String,
        val usage: ProviderUsage?
    ) : StreamEvent()

    /**
     * 流式错误（上游显式返回，或解析失败时使用）。终止整个流。
     */
    data class Error(
        val message: String,
        val kind: ProviderException.Kind = ProviderException.Kind.UNKNOWN
    ) : StreamEvent()
}
