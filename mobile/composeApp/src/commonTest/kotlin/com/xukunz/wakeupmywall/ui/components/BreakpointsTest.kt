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
    fun `between six hundred and one thousand dp is medium with three columns`() {
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(600))
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(999))
        assertEquals(3, Breakpoints.columnsFor(800))
    }

    @Test
    fun `one thousand dp and above is expanded with concept columns`() {
        assertEquals(LayoutWidth.Expanded, Breakpoints.widthFor(1000))
        assertEquals(5, Breakpoints.columnsFor(1200))
    }

    @Test
    fun `boundary values are inclusive on the lower edge`() {
        assertEquals(LayoutWidth.Compact, Breakpoints.widthFor(599))
        assertEquals(LayoutWidth.Medium, Breakpoints.widthFor(600))
        assertEquals(LayoutWidth.Expanded, Breakpoints.widthFor(1000))
    }

    @Test
    fun `dp and int forms agree`() {
        listOf(320, 599, 600, 999, 1000, 1440).forEach { width ->
            assertEquals(Breakpoints.columnsFor(width), Breakpoints.columns(width.dp))
        }
    }

    @Test
    fun `wide screens keep the concept column count`() {
        assertEquals(5, Breakpoints.columns(1000.dp))
        assertEquals(5, Breakpoints.columns(1440.dp))
    }

    @Test
    fun `medium screens drop to three columns`() {
        assertEquals(3, Breakpoints.columns(600.dp))
        assertEquals(3, Breakpoints.columns(999.dp))
    }

    @Test
    fun `narrow screens use two columns and scroll`() {
        assertEquals(2, Breakpoints.columns(599.dp))
        assertEquals(2, Breakpoints.columns(320.dp))
    }

    @Test
    fun `narrow screens are allowed to scroll vertically`() {
        // 3 列也会堆叠变高，因此 600–999dp 同样需要滚动，只有 5 列布局不需要。
        assertEquals(true, Breakpoints.requiresVerticalScroll(599.dp))
        assertEquals(true, Breakpoints.requiresVerticalScroll(600.dp))
        assertEquals(true, Breakpoints.requiresVerticalScroll(999.dp))
        assertEquals(false, Breakpoints.requiresVerticalScroll(1000.dp))
    }
}
