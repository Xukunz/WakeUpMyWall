package com.xukunz.wakeupmywall.ui.components

import androidx.compose.ui.unit.Dp
import com.xukunz.wakeupmywall.core.theme.AppSizes

/**
 * 响应式断点（Phase 1 全局约束 + 风险 R7）：窄屏禁止强排 5 列。
 * Task 14 会在此基础上扩展（Rail 宽度、卡片密度等），本任务先落主区列数决策。
 */
object Breakpoints {
    val fiveColumnMinWidth = AppSizes.fiveColumnMinWidth
    val threeColumnMinWidth = AppSizes.threeColumnMinWidth

    fun columns(available: Dp): Int = when {
        available >= fiveColumnMinWidth -> 5
        available >= threeColumnMinWidth -> 3
        else -> 2
    }

    /**
     * 只有概念图的 5 列布局能在固定高度里放得下；降到 3 列或 2 列时卡片会换行堆叠，
     * 必须允许纵向滚动，否则内容会被裁掉（实测踩到）。
     */
    fun requiresVerticalScroll(available: Dp): Boolean = columns(available) < 5
}
