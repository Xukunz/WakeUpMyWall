package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字号与字重令牌。概念图还原只允许通过这些令牌取值，
 * `DesignTokenDisciplineTest` 会拦下 Composable 里的裸 `sp` / `dp` / `Color(0x...)`。
 */
object AppTypography {
    val display = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Light)
    val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val metricValue = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
    val metricUnit = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
}
