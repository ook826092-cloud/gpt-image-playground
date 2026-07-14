package com.gptimage.playground.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 全局间距 token。
 *
 * 统一所有屏幕的间距，避免 4/8/12/16 混用导致视觉无序。
 * iOS HIG 推荐 8pt 网格，这里以 4dp 为基础单位。
 */
object Spacing {
    val xs = 4.dp     // 元素内紧密间距（图标与文字）
    val sm = 8.dp     // 同一组件内元素间距
    val md = 12.dp    // 同一卡片内分组间距
    val lg = 16.dp    // 卡片之间、Section 间距
    val xl = 20.dp    // 大区块间距
    val xxl = 24.dp   // 页面级 padding / 顶部分组间距
    val xxxl = 32.dp  // 强调空隙
}

/** 屏幕级 horizontal padding（防止内容贴边）。 */
object ContentPadding {
    val screen = 16.dp
    val card = 16.dp
    val tight = 12.dp
}

/** iOS 风组件高度。 */
object AppHeight {
    val buttonPrimary = 50.dp   // iOS 主要 CTA 按钮
    val buttonSecondary = 44.dp // iOS 标准按钮
    val tabBar = 60.dp          // 浮动 tab bar 总高（含安全区再加）
    val input = 50.dp
    val chip = 32.dp
    val listRow = 56.dp          // iOS Settings list row
    val sectionHeader = 32.dp
}

/** iOS 风组件圆角（这里再次集中导出，方便直接 import）。 */
object AppRadius {
    val small = 10.dp
    val medium = 14.dp
    val large = 20.dp
    val pill = 50  // 胶囊形
}

/** 阴影 elevation token（iOS 风用很轻的阴影 + 浅色边）。 */
object AppElevation {
    val none = 0.dp
    val card = 1.dp    // 卡片用极浅阴影 + 0.5dp outline
    val raised = 3.dp  // 浮动元素（输入框 focus、tab bar）
    val modal = 8.dp   // 弹窗、底部抽屉
}
