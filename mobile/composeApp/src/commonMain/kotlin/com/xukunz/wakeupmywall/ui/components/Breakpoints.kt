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
     * 只有概念图的 5 列布局能在固定高度里放得下；降到 3 列或 2 列时卡片会换行堆叠，
     * 必须允许纵向滚动，否则内容会被裁掉（实测踩到）。
     */
    fun requiresVerticalScroll(available: Dp): Boolean = columns(available) < 5
}
