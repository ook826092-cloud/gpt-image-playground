package com.gptimage.playground.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.gptimage.playground.data.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest
)

/**
 * 主题级的额外颜色（M3 colorScheme 不直接覆盖的部分）。
 * 通过 CompositionLocal 暴露给自定义组件（毛玻璃 scrim、品牌渐变等）。
 */
data class AppExtraColors(
    val scrim: Color,
    val brandPrimary: Color,
    val brandPrimaryDark: Color,
    val brandSecondary: Color,
    val brandAccent: Color,
    /** 用于聊天气泡的「对方/系统」气泡背景色 */
    val systemBubble: Color,
    /** 用于聊天气泡的「自己/用户」气泡背景色（与 brand 渐变匹配） */
    val userBubble: Color,
    val onUserBubble: Color,
    /** 品牌渐变起末 */
    val gradientStart: Color,
    val gradientEnd: Color,
    /** iOS 风「分组背景」灰（用于 list section 背景） */
    val groupedBackground: Color,
    /** iOS 风「分组单元格」白 */
    val groupedCell: Color
)

private val LightExtraColors = AppExtraColors(
    scrim = LightScrim,
    brandPrimary = BrandPrimary,
    brandPrimaryDark = BrandPrimaryDark,
    brandSecondary = BrandSecondary,
    brandAccent = BrandAccent,
    systemBubble = Color(0xFFE9ECF1),
    userBubble = BrandPrimary,
    onUserBubble = Color(0xFFFFFFFF),
    gradientStart = BrandSecondary,
    gradientEnd = BrandPrimary,
    groupedBackground = LightBackground,
    groupedCell = LightSurface
)

private val DarkExtraColors = AppExtraColors(
    scrim = DarkScrim,
    brandPrimary = DarkPrimary,
    brandPrimaryDark = BrandPrimaryDark,
    brandSecondary = DarkSecondary,
    brandAccent = DarkTertiary,
    systemBubble = Color(0xFF2C2C2E),
    userBubble = BrandPrimaryDark,
    onUserBubble = Color(0xFFFFFFFF),
    gradientStart = DarkSecondary,
    gradientEnd = DarkPrimary,
    groupedBackground = DarkBackground,
    groupedCell = DarkSurface
)

val LocalAppExtraColors = staticCompositionLocalOf { LightExtraColors }

@Composable
fun GptImagePlaygroundTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> DarkColors
        else -> LightColors
    }
    val extraColors = if (useDark) DarkExtraColors else LightExtraColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 透明状态栏 / 导航栏，配合 enableEdgeToEdge 让内容能延伸到系统栏后面
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
            // edge-to-edge 必须保留 fitsSystemWindows=false，否则 MainActivity.enableEdgeToEdge 会被抵消
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    CompositionLocalProvider(LocalAppExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** 在任意位置快速拿到当前 extraColors（不强制重组）。 */
object AppExtra {
    val current: AppExtraColors
        @Composable get() = LocalAppExtraColors.current
}
