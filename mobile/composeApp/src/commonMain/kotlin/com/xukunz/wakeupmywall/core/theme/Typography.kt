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
    /**
     * Monitor 第三行（Network / Uptime）的行内读数。概念图里这两个数字明显小于
     * Dashboard 的天气温度（`metricValue`）：在 4 列等宽的卡片里用 32sp 时
     * `↓124.3 Mbps` 会折成两行、`3d 6h 24m` 也会断行，与概念图的一行排布不符
     * （2026-09-20 按 1280×720 真实成帧复核）。
     *
     * 18sp 不是拍脑袋：概念图里 `124.3 Mbps` 的数字高约 11.7dp、宽约 86dp，
     * 换算字形高对应 ~16sp；本项目的默认字体比概念图的窄体更宽（0.69em/字符 vs 0.53），
     * 取 18sp 才能既接近概念图的字号、又让 `↓124.3 Mbps` 在 152dp 的卡片内容宽里排成一行。
     */
    val metricReadout = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val metricUnit = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
}
