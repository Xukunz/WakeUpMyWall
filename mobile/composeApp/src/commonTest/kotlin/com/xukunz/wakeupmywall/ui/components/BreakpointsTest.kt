package com.xukunz.wakeupmywall.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class BreakpointsTest {

    @Test
    fun `below six hundred dp is compact with two columns`() {
        assertEquals(LayoutWidth.Compact, Breakpoints.widthFor(560))
        assertEquals(2, Breakpoints.columnsFor(560))
    }

    @Test
    fun `between six hundred and nine hundred dp is medium with three columns`() {
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(600))
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(899))
        assertEquals(3, Breakpoints.columnsFor(800))
    }

    @Test
    fun `nine hundred dp and above is expanded with concept columns`() {
        assertEquals(LayoutWidth.Expanded, Breakpoints.widthFor(900))
        assertEquals(5, Breakpoints.columnsFor(1200))
    }

    @Test
    fun `the wall screen main area reaches the concept five column grid`() {
        // 墙面屏 1280×720dp，72% 主区 = 921dp；概念图在这个宽度下排的就是 5 列指标卡。
        // 原先的 1000dp 阈值会把目标屏排除在概念栅格之外（2026-09-20 按真实成帧复核）。
        assertEquals(921, (1280 * 0.72f).toInt())
        assertEquals(5, Breakpoints.columnsFor(921))
        assertEquals(false, Breakpoints.requiresVerticalScroll(921.dp))
    }

    @Test
    fun `boundary values are inclusive on the lower edge`() {
        assertEquals(LayoutWidth.Compact, Breakpoints.widthFor(599))
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(600))
        assertEquals(LayoutWidth.Expanded, Breakpoints.widthFor(900))
    }

    @Test
    fun `dp and int forms agree`() {
        listOf(320, 599, 600, 899, 900, 921, 1440).forEach { width ->
            assertEquals(Breakpoints.columnsFor(width), Breakpoints.columns(width.dp))
        }
    }

    @Test
    fun `wide screens keep the concept column count`() {
        assertEquals(5, Breakpoints.columns(900.dp))
        assertEquals(5, Breakpoints.columns(1440.dp))
    }

    @Test
    fun `medium screens drop to three columns`() {
        assertEquals(3, Breakpoints.columns(600.dp))
        assertEquals(3, Breakpoints.columns(899.dp))
    }

    @Test
    fun `narrow screens use two columns and scroll`() {
        assertEquals(2, Breakpoints.columns(599.dp))
        assertEquals(2, Breakpoints.columns(320.dp))
    }

    @Test
    fun `narrow screens are allowed to scroll vertically`() {
        // 3 列也会堆叠变高，因此 600–899dp 同样需要滚动，只有 5 列布局不需要。
        assertEquals(true, Breakpoints.requiresVerticalScroll(599.dp))
        assertEquals(true, Breakpoints.requiresVerticalScroll(600.dp))
        assertEquals(true, Breakpoints.requiresVerticalScroll(899.dp))
        assertEquals(false, Breakpoints.requiresVerticalScroll(900.dp))
    }

    @Test
    fun `dashboard keeps three columns at the wall screen while monitor uses five`() {
        // 概念图 B 的最宽行是 3 张卡、概念图 C 的指标行是 5 张卡——两个栅格不能共用一个列数。
        assertEquals(3, Breakpoints.dashboardColumns(921.dp))
        assertEquals(3, Breakpoints.dashboardColumns(1440.dp))
        assertEquals(5, Breakpoints.columns(921.dp))

        // 窄屏两者一起降到 2 列。
        assertEquals(2, Breakpoints.dashboardColumns(599.dp))
        assertEquals(2, Breakpoints.columns(599.dp))
    }
}
