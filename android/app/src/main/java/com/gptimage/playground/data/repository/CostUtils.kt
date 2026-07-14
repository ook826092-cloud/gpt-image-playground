package com.gptimage.playground.data.repository

import com.gptimage.playground.data.model.HistoryItem

/**
 * 费用估算工具。移植自 Web 项目 `src/lib/cost-utils.ts`。
 *
 * 仅对 OpenAI gpt-image 系列给出精确费率；Gemini Nano Banana 2 在预览期暂估 \$0；
 * SenseNova / Seedream / Stability 在 Web 项目里走 fallthrough（不精确），Android
 * 端为避免误导用户改为返回 `null`（详情面板会显示「费用数据不可用」而不是给出错误数字）。
 */
object CostUtils {

    /** gpt-image-1 每 token 单价（USD）。 */
    private const val GPT1_TEXT_PER_TOKEN = 0.000005        // $5.00 / 1M
    private const val GPT1_IMAGE_IN_PER_TOKEN = 0.00001      // $10.00 / 1M
    private const val GPT1_IMAGE_OUT_PER_TOKEN = 0.00004     // $40.00 / 1M

    /** gpt-image-1-mini 每 token 单价（USD）。 */
    private const val GPT1_MINI_TEXT_PER_TOKEN = 0.000002        // $2.00 / 1M
    private const val GPT1_MINI_IMAGE_IN_PER_TOKEN = 0.0000025    // $2.50 / 1M
    private const val GPT1_MINI_IMAGE_OUT_PER_TOKEN = 0.000008   // $8.00 / 1M

    /** gpt-image-1.5 每 token 单价（USD）。 */
    private const val GPT1_5_TEXT_PER_TOKEN = 0.000005        // $5.00 / 1M
    private const val GPT1_5_IMAGE_IN_PER_TOKEN = 0.000008    // $8.00 / 1M
    private const val GPT1_5_IMAGE_OUT_PER_TOKEN = 0.000032  // $32.00 / 1M

    /** gpt-image-2 每 token 单价（USD）。 */
    private const val GPT2_TEXT_PER_TOKEN = 0.000005         // $5.00 / 1M
    private const val GPT2_IMAGE_IN_PER_TOKEN = 0.000008     // $8.00 / 1M
    private const val GPT2_IMAGE_OUT_PER_TOKEN = 0.00003     // $30.00 / 1M

    /** Gemini Nano Banana 2 预览期暂估 $0。 */
    private const val GEMINI_NANO_BANANA_TEXT_PER_TOKEN = 0.0
    private const val GEMINI_NANO_BANANA_IMAGE_IN_PER_TOKEN = 0.0
    private const val GEMINI_NANO_BANANA_IMAGE_OUT_PER_TOKEN = 0.0

    data class ModelRates(
        val textInputPerToken: Double,
        val imageInputPerToken: Double,
        val imageOutputPerToken: Double
    ) {
        /** 每 1M token 的 USD 单价，用于详情展示。 */
        val textInputPerMillion: Double get() = textInputPerToken * 1_000_000
        val imageInputPerMillion: Double get() = imageInputPerToken * 1_000_000
        val imageOutputPerMillion: Double get() = imageOutputPerToken * 1_000_000
    }

    /**
     * 返回模型的 token 单价，找不到返回 `null`。
     * 返回 null 表示该 provider 不支持 token 计费或不精确。
     */
    fun ratesFor(modelId: String): ModelRates? = when (modelId) {
        "gpt-image-2" -> ModelRates(
            textInputPerToken = GPT2_TEXT_PER_TOKEN,
            imageInputPerToken = GPT2_IMAGE_IN_PER_TOKEN,
            imageOutputPerToken = GPT2_IMAGE_OUT_PER_TOKEN
        )
        "gpt-image-1.5" -> ModelRates(
            textInputPerToken = GPT1_5_TEXT_PER_TOKEN,
            imageInputPerToken = GPT1_5_IMAGE_IN_PER_TOKEN,
            imageOutputPerToken = GPT1_5_IMAGE_OUT_PER_TOKEN
        )
        "gpt-image-1-mini" -> ModelRates(
            textInputPerToken = GPT1_MINI_TEXT_PER_TOKEN,
            imageInputPerToken = GPT1_MINI_IMAGE_IN_PER_TOKEN,
            imageOutputPerToken = GPT1_MINI_IMAGE_OUT_PER_TOKEN
        )
        // Gemini 系列全部按 $0 估算
        "gemini-3.1-flash-image-preview",
        "gemini-3-pro-image-preview",
        "gemini-3.1-flash-lite-image" -> ModelRates(
            textInputPerToken = GEMINI_NANO_BANANA_TEXT_PER_TOKEN,
            imageInputPerToken = GEMINI_NANO_BANANA_IMAGE_IN_PER_TOKEN,
            imageOutputPerToken = GEMINI_NANO_BANANA_IMAGE_OUT_PER_TOKEN
        )
        // gpt-image-1 默认分支
        "gpt-image-1" -> ModelRates(
            textInputPerToken = GPT1_TEXT_PER_TOKEN,
            imageInputPerToken = GPT1_IMAGE_IN_PER_TOKEN,
            imageOutputPerToken = GPT1_IMAGE_OUT_PER_TOKEN
        )
        // 其他 provider（SenseNova / Seedream / Stability）按张/按次计费，
        // 与 token 模型不同，暂返回 null 表示「不可估算」。
        else -> null
    }

    data class CostDetails(
        val estimatedCostUsd: Double,
        val textInputTokens: Int,
        val imageInputTokens: Int,
        val imageOutputTokens: Int
    )

    /**
     * 根据已保存的 [HistoryItem] 的 token 用量估算费用。
     * 当模型不支持 token 计费或 token 数据缺失时返回 `null`。
     */
    fun calculate(item: HistoryItem): CostDetails? {
        val rates = ratesFor(item.model) ?: return null
        val textTokens = item.inputTextTokens ?: return null
        val imageTokens = item.inputImageTokens ?: 0
        val outputTokens = item.outputTokens ?: return null
        if (outputTokens < 0) return null

        val cost = textTokens * rates.textInputPerToken +
            imageTokens * rates.imageInputPerToken +
            outputTokens * rates.imageOutputPerToken

        // 4 位小数精度，与 Web 端对齐
        val rounded = Math.round(cost * 10000) / 10000.0
        return CostDetails(
            estimatedCostUsd = rounded,
            textInputTokens = textTokens,
            imageInputTokens = imageTokens,
            imageOutputTokens = outputTokens
        )
    }

    /** 把费用格式化为「$X.XX」短显示（2 位小数）。 */
    fun formatShort(usd: Double): String = "$${String.format("%.2f", usd)}"

    /** 把费用格式化为「$X.XXXX」精确显示（4 位小数）。 */
    fun formatPrecise(usd: Double): String = "$${String.format("%.4f", usd)}"

    /** 把耗时毫秒格式化为「1.2s」或「345ms」。 */
    fun formatDuration(ms: Long): String {
        return if (ms < 1000) "${ms}ms"
        else "${String.format("%.1f", ms / 1000.0)}s"
    }
}
