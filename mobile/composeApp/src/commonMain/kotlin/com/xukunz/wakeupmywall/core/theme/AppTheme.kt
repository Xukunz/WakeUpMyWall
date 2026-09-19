package com.xukunz.wakeupmywall.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun WakeUpMyWallTheme(
    accent: ThemeAccent = ThemeAccent.AuroraBlue,
    content: @Composable () -> Unit,
) {
    val palette = accent.palette()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = DarkSurface.background,
            onBackground = DarkSurface.textPrimary,
            surface = DarkSurface.card,
            onSurface = DarkSurface.textPrimary,
            outline = DarkSurface.outline,
        ),
        shapes = Shapes(
            medium = AppShapes.card,
            large = AppShapes.card,
            small = AppShapes.badge,
        ),
        typography = Typography(
            titleLarge = AppTypography.title,
            bodyMedium = AppTypography.body,
            labelSmall = AppTypography.label,
            displayLarge = AppTypography.display,
        ),
        content = content,
    )
}
