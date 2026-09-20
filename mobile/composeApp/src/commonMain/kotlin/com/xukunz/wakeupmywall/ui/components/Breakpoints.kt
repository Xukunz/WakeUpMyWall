package com.xukunz.wakeupmywall.ui.components

import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes

enum class LayoutWidth { Compact, Medium, Expanded }

/**
 * 响应式断点（Phase 1 全局约束 + 风险 R7）：窄屏禁止强排 5 列。
 * 阈值来自 `AppSizes` 令牌，`Int` 形态供纯逻辑调用方（无需 Compose 环境）使用。
 */
object Breakpoints {
    val fiveColumnMinWidth = AppSizes.fiveColumnMinWidth
    val threeColumnMinWidth = AppSizes.threeColumnMinWidth

    val compactMax: Int = threeColumnMinWidth.value.toInt() - 1
    val mediumMax: Int = fiveColumnMinWidth.value.toInt() - 1

    fun widthFor(availableDp: Int): LayoutWidth = when {
        availableDp <= compactMax -> LayoutWidth.Compact
        availableDp <= mediumMax -> LayoutWidth.Medium
        else -> LayoutWidth.Expanded
    }

    fun columnsFor(availableDp: Int): Int = when (widthFor(availableDp)) {
        LayoutWidth.Compact -> 2
        LayoutWidth.Medium -> 3
        LayoutWidth.Expanded -> 5
    }

    fun columns(available: Dp): Int = columnsFor(available.value.toInt())

    /**
     * Dashboard 的栅格与 Monitor 不同：概念图 B 最宽的一行只有 3 张卡（天气 / 日历 / 任务），
     * 5 列只出现在 Monitor 的指标行。主区再宽也不给 Dashboard 加到 5 列——
     * 否则 3 张卡会被塞进 5 个名额里，每张只剩 1/5 宽（2026-09-20 实测把任务卡挤成逐字换行）。
     *
     * 竖屏窄机（20:9 手机 412dp、21.1:9 折叠外屏 412dp）直接退成单列：天气卡里那四列逐时预报
     * 在 200dp 宽的卡片里排不下，两列会同时牺牲天气卡与日历卡的可读性。
     */
    fun dashboardColumns(available: Dp, portrait: Boolean = false): Int = when {
        portrait && available <= AppSizes.dashboardSingleColumnMaxWidth -> 1
        else -> minOf(columnsFor(available.value.toInt()), 3)
    }

    /** 竖屏窄机上顶行（身份卡 + 快捷动作）与底行不再并排。 */
    fun stacksRows(available: Dp): Boolean = available <= AppSizes.stackedRowsMaxWidth

    /**
     * 侧栏还是底栏：宽度够 + 横屏才用概念图的 72/28 侧栏；竖屏手机、折叠外屏、
     * 以及任何窄窗口都用底部常驻栏，避免主内容被挤到 200dp 宽。
     */
    fun usesSideRail(width: Dp, height: Dp): Boolean =
        width >= AppSizes.sideRailMinWidth && width > height

    fun usesInlineNav(available: Dp): Boolean = available <= AppSizes.inlineNavMaxWidth

    /**
     * 只有概念图的 5 列布局能在固定高度里放得下；降到 3 列或 2 列时卡片会换行堆叠，
     * 必须允许纵向滚动，否则内容会被裁掉（实测踩到）。
     */
    fun requiresVerticalScroll(available: Dp): Boolean = columns(available) < 5
}
