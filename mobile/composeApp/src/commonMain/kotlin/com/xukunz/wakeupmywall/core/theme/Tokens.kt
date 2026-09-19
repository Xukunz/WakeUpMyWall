package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeAccent { AuroraBlue, Emerald, Purple, Amber, Red, Dynamic }

data class AccentPalette(val accent: Color, val onAccent: Color, val onlineColor: Color)

/**
 * `onAccent` 必须与 `accent` 保持 ≥ 4.5:1 对比度（见 ThemeAccentTest），
 * `onlineColor` 用于在线状态点，必须与强调色可区分，避免"在线"与"主按钮"混淆。
 */
fun ThemeAccent.palette(): AccentPalette = when (this) {
    ThemeAccent.AuroraBlue -> AccentPalette(Color(0xFF4C8DFF), Color(0xFF06101F), Color(0xFF3DDC97))
    ThemeAccent.Emerald -> AccentPalette(Color(0xFF2FBF71), Color(0xFF03170B), Color(0xFF7BE495))
    ThemeAccent.Purple -> AccentPalette(Color(0xFF8B5CF6), Color(0xFF120726), Color(0xFF5EEAD4))
    ThemeAccent.Amber -> AccentPalette(Color(0xFFF5A524), Color(0xFF2A1A00), Color(0xFF7BE495))
    ThemeAccent.Red -> AccentPalette(Color(0xFFE5484D), Color(0xFF1F0407), Color(0xFF7BE495))
    ThemeAccent.Dynamic -> AccentPalette(Color(0xFF4C8DFF), Color(0xFF06101F), Color(0xFF3DDC97))
}

object Spacing {
    /** 分隔线与卡片描边的线宽。写进令牌，`ui/` 里就不允许出现裸 `1.dp`。 */
    val hairline = 1.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object DarkSurface {
    val background = Color(0xFF0B0F17)
    val card = Color(0xFF151B26)
    val outline = Color(0x33FFFFFF)
    val textPrimary = Color(0xFFE8EDF5)
    val textSecondary = Color(0xFF9AA6B8)
}
