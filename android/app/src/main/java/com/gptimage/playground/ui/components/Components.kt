package com.gptimage.playground.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gptimage.playground.ui.theme.AppCorner
import com.gptimage.playground.ui.theme.AppElevation
import com.gptimage.playground.ui.theme.AppExtra
import com.gptimage.playground.ui.theme.ContentPadding

// =====================================================================
// AppCard — iOS 风「分组单元格」白卡：纯白底 + 极浅阴影 + 0.5dp outline + 圆角
// =====================================================================

/**
 * iOS 风卡片容器。
 *
 * 视觉规则：
 * - 浅色：纯白底 + 1dp elevation + 极浅 outline
 * - 深色：1C1C1E 卡片底
 * - 圆角默认 medium(14dp)
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    corner: Shape = AppCorner.card,
    elevation: Dp = AppElevation.card,
    outline: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(ContentPadding.card),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val baseModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true)
        ) { onClick() }
    } else Modifier

    Surface(
        modifier = modifier.then(baseModifier),
        shape = corner,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = elevation,
        border = if (outline) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// =====================================================================
// AppButton — 主要 CTA 用品牌渐变；次要按钮用 surface + outline
// =====================================================================

enum class AppButtonStyle {
    /** 主要 CTA：品牌青→蓝渐变，文字白色 */
    Primary,
    /** 次要按钮：浅色底 + outline */
    Secondary,
    /** 文字按钮（无背景） */
    Text,
    /** tonal：填充 primaryContainer */
    Tonal
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    height: Dp = 50.dp
) {
    val cs = MaterialTheme.colorScheme
    val extra = AppExtra.current
    val containerColor = when (style) {
        AppButtonStyle.Primary -> Color.Transparent // 渐变用 background
        AppButtonStyle.Secondary -> cs.surface
        AppButtonStyle.Text -> Color.Transparent
        AppButtonStyle.Tonal -> cs.primaryContainer
    }
    val contentColor = when (style) {
        AppButtonStyle.Primary -> Color.White
        AppButtonStyle.Secondary -> cs.primary
        AppButtonStyle.Text -> cs.primary
        AppButtonStyle.Tonal -> cs.onPrimaryContainer
    }
    val border: BorderStroke? = when (style) {
        AppButtonStyle.Secondary -> BorderStroke(0.5.dp, cs.outline)
        else -> null
    }
    val shape = AppCorner.pill

    val clickModifier = if (enabled) Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = contentColor)
    ) { onClick() } else Modifier.alpha(0.4f)

    val backgroundModifier = if (style == AppButtonStyle.Primary) {
        Modifier.background(
            brush = Brush.horizontalGradient(listOf(extra.gradientStart, extra.gradientEnd)),
            shape = shape
        )
    } else if (containerColor != Color.Transparent) {
        Modifier.background(containerColor, shape)
    } else Modifier

    Box(
        modifier = modifier
            .then(clickModifier)
            .then(backgroundModifier)
            .border(border ?: BorderStroke(0.dp, Color.Transparent), shape)
            .height(height)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge)
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(trailingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// =====================================================================
// AppIconButton — 圆形图标按钮（用于卡片右侧操作）
// =====================================================================

@Composable
fun AppIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    background: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// =====================================================================
// AppTextField — iOS 风单行/多行输入框
// 浅色：白底圆角 + 浅 outline；focus 时 outline 变 primary
// =====================================================================

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    minHeight: Dp = 50.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (focused) cs.primary else cs.outlineVariant
    val shape = AppCorner.card

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = cs.surface,
        border = BorderStroke(if (focused) 1.5.dp else 0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = minHeight)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = cs.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = textStyle.copy(color = cs.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(cs.primary),
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                val clickMod = if (onTrailingIconClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onTrailingIconClick() } else Modifier
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).then(clickMod)
                )
            }
        }
    }
}

// =====================================================================
// AppChip — 胶囊形选择 chip
// =====================================================================

@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (selected) cs.primary else cs.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (selected) cs.onPrimary else cs.onSurface
    val border = if (selected) null else BorderStroke(0.5.dp, cs.outlineVariant)

    Box(
        modifier = modifier
            .clip(AppCorner.pill)
            .background(bg)
            .then(if (border != null) Modifier.border(border, AppCorner.pill) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = fg, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// =====================================================================
// AppSectionHeader — iOS Settings 风「分组标题」（小字 + secondary label）
// =====================================================================

@Composable
fun AppSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailing?.invoke()
    }
}

// =====================================================================
// AppListRow — iOS Settings 风列表行（leading icon + label + trailing）
// =====================================================================

@Composable
fun AppListRow(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    val cs = MaterialTheme.colorScheme
    val clickMod = if (onClick != null) Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true)
    ) { onClick() } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickMod)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(AppCorner.small)
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(leadingIcon, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
        if (trailingText != null) {
            Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
        }
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
