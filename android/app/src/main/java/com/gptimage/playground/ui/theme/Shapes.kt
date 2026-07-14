package com.gptimage.playground.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * iOS 风圆角策略。
 *
 * - small:  小元素（chip / button 内圆角）— 10dp
 * - medium: 卡片、输入框、列表项 — 14dp
 * - large:  弹窗、底部抽屉、大卡片 — 20dp
 * - extraLarge: 全屏弹层 — 28dp
 *
 * iOS 系统组件普遍使用 10~16dp 持续圆角（continuous corner），
 * Compose 暂不直接支持 continuous corner，这里用 RoundedCornerShape 近似。
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** 额外的、超出 M3 Shapes 范围的圆角（供自定义组件直接引用）。 */
object AppCorner {
    val pill = RoundedCornerShape(50) // 完全圆角（胶囊形）
    val small = RoundedCornerShape(10.dp)
    val card = RoundedCornerShape(14.dp)
    val large = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val dialog = RoundedCornerShape(20.dp)
    val bubble = RoundedCornerShape(18.dp)
}
