package com.gptimage.playground.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()

val AppTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    displayMedium = Default.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    displaySmall = Default.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = Default.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Default.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Default.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Default.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = Default.bodyLarge.copy(lineHeight = 22.sp),
    bodyMedium = Default.bodyMedium.copy(lineHeight = 20.sp),
    bodySmall = Default.bodySmall.copy(lineHeight = 16.sp),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = Default.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = Default.labelSmall.copy(fontWeight = FontWeight.Medium)
)

val AppFontFamily: FontFamily = FontFamily.Default
