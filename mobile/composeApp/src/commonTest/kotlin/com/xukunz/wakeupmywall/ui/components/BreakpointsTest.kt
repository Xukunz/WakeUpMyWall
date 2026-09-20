package com.xukunz.wakeupmywall.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class BreakpointsTest {

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
