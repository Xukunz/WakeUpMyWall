package com.xukunz.wakeupmywall.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前强调色。Material3 的 colorScheme 只暴露语义槽位，拿不到"在线色"这类令牌，
 * 所以把整份 palette 一并下发；Phase 7 的 Appearance 切换时只需改这一个入口。
 */
val LocalAccentPalette = staticCompositionLocalOf { ThemeAccent.AuroraBlue.palette() }

@Composable
fun WakeUpMyWallTheme(
    accent: ThemeAccent = ThemeAccent.AuroraBlue,
    content: @Composable () -> Unit,
) {
    val palette = accent.palette()
    CompositionLocalProvider(LocalAccentPalette provides palette) {
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
}
