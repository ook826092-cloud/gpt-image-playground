package com.gptimage.playground.ui.theme

import androidx.compose.ui.graphics.Color

// ------------------------------------------------------------------
// 品牌色（青蓝 + 橙 accent）
// 这些值会落地到 colorScheme，建立品牌识别
// ------------------------------------------------------------------
val BrandPrimary = Color(0xFF4A90E2)        // 亮蓝（主 CTA、选中态）
val BrandPrimaryDark = Color(0xFF2E6FBF)    // 深蓝（按下态、渐变末端）
val BrandSecondary = Color(0xFF24C8DB)      // 青（渐变起端、次要 accent）
val BrandAccent = Color(0xFFFF8A4C)         // 橙（强调、徽标）

// ------------------------------------------------------------------
// 浅色 scheme — iOS 风：纯白卡片 + 浅灰分组背景 + 品牌蓝主色
// ------------------------------------------------------------------
val LightPrimary = BrandPrimary                 // 用品牌亮蓝作主色
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE3F0FB)  // 极浅蓝容器
val LightOnPrimaryContainer = Color(0xFF0D3B66)
val LightSecondary = Color(0xFF0E8FA3)          // 青色系，但更可读
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCFF5FA)
val LightOnSecondaryContainer = Color(0xFF002021)
val LightTertiary = BrandAccent                // 橙作 tertiary
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFE4D1)
val LightOnTertiaryContainer = Color(0xFF2A1500)
val LightError = Color(0xFFFF3B30)              // iOS 系统红
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFE0DE)
val LightOnErrorContainer = Color(0xFF410002)
val LightBackground = Color(0xFFF2F4F8)         // iOS 分组背景灰
val LightOnBackground = Color(0xFF1C1C1E)       // iOS label 色
val LightSurface = Color(0xFFFFFFFF)            // 纯白卡片
val LightOnSurface = Color(0xFF1C1C1E)
val LightSurfaceVariant = Color(0xFFEEF0F4)
val LightOnSurfaceVariant = Color(0xFF6B6F76)   // iOS secondary label
val LightOutline = Color(0xFFD1D5DC)            // iOS separator
val LightOutlineVariant = Color(0xFFE5E7EB)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF7F8FA)
val LightSurfaceContainer = Color(0xFFF2F4F8)
val LightSurfaceContainerHigh = Color(0xFFEBEDF2)
val LightSurfaceContainerHighest = Color(0xFFE5E7EB)
// iOS 风额外色：毛玻璃半透明层
val LightScrim = Color(0xFF000000).copy(alpha = 0.32f)

// ------------------------------------------------------------------
// 深色 scheme — iOS 风：深黑背景 + 浅色文字 + 品牌亮蓝
// ------------------------------------------------------------------
val DarkPrimary = Color(0xFF6BA4E8)             // 浅品牌蓝（深色背景上更亮）
val DarkOnPrimary = Color(0xFF0D3B66)
val DarkPrimaryContainer = Color(0xFF1A4A7A)
val DarkOnPrimaryContainer = Color(0xFFD6E7FA)
val DarkSecondary = Color(0xFF4CD9DE)
val DarkOnSecondary = Color(0xFF00373A)
val DarkSecondaryContainer = Color(0xFF0F5A60)
val DarkOnSecondaryContainer = Color(0xFFA8F0F5)
val DarkTertiary = Color(0xFFFFB07A)
val DarkOnTertiary = Color(0xFF3A1F00)
val DarkTertiaryContainer = Color(0xFF5A3300)
val DarkOnTertiaryContainer = Color(0xFFFFE4D1)
val DarkError = Color(0xFFFF453A)                // iOS 深色红
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkBackground = Color(0xFF000000)            // iOS 纯黑背景
val DarkOnBackground = Color(0xFFF2F2F7)          // iOS 浅色 label
val DarkSurface = Color(0xFF1C1C1E)               // iOS 系统深灰卡片
val DarkOnSurface = Color(0xFFF2F2F7)
val DarkSurfaceVariant = Color(0xFF2C2C2E)
val DarkOnSurfaceVariant = Color(0xFFAEAEB2)      // iOS secondary label dark
val DarkOutline = Color(0xFF48484A)               // iOS separator dark
val DarkOutlineVariant = Color(0xFF2C2C2E)
val DarkSurfaceContainerLowest = Color(0xFF000000)
val DarkSurfaceContainerLow = Color(0xFF1C1C1E)
val DarkSurfaceContainer = Color(0xFF2C2C2E)
val DarkSurfaceContainerHigh = Color(0xFF3A3A3C)
val DarkSurfaceContainerHighest = Color(0xFF48484A)
val DarkScrim = Color(0xFF000000).copy(alpha = 0.5f)
