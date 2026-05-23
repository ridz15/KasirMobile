package com.moncake94.pos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val Colors = lightColorScheme(
    primary = Color(0xFF8B4D2E),
    onPrimary = Color.White,
    secondary = Color(0xFF3E6B4F),
    onSecondary = Color.White,
    tertiary = Color(0xFFB35C44),
    background = Color(0xFFFFF9F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFE7D2),
    onBackground = Color(0xFF251914),
    onSurface = Color(0xFF251914),
    outline = Color(0xFF9B887E)
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(letterSpacing = 0.sp),
        displayMedium = base.displayMedium.copy(letterSpacing = 0.sp),
        displaySmall = base.displaySmall.copy(letterSpacing = 0.sp),
        headlineLarge = base.headlineLarge.copy(letterSpacing = 0.sp),
        headlineMedium = base.headlineMedium.copy(letterSpacing = 0.sp),
        headlineSmall = base.headlineSmall.copy(letterSpacing = 0.sp),
        titleLarge = base.titleLarge.copy(letterSpacing = 0.sp),
        titleMedium = base.titleMedium.copy(letterSpacing = 0.sp),
        titleSmall = base.titleSmall.copy(letterSpacing = 0.sp),
        bodyLarge = base.bodyLarge.copy(letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.sp)
    )
}

@Composable
fun MoncakeTheme(content: @Composable () -> Unit) {
    val ignored = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = Colors,
        typography = AppTypography,
        content = content
    )
}
