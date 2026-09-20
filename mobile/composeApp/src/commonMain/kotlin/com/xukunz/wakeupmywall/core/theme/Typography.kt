package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字号与字重令牌。概念图还原只允许通过这些令牌取值，
 * `DesignTokenDisciplineTest` 会拦下 Composable 里的裸 `sp` / `dp` / `Color(0x...)`。
 */
object AppTypography {
    /**
     * StandBy 模式的超大时钟（权威规格 D）。显式给足 lineHeight：120sp 时不指定行高，
     * 行盒会小于字形墨迹范围，实测数字顶部会被裁掉。
     */
    val hugeClock = TextStyle(fontSize = 104.sp, lineHeight = 124.sp, fontWeight = FontWeight.Light)
    val display = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Light)
    val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val metricValue = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
    val metricUnit = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
}
