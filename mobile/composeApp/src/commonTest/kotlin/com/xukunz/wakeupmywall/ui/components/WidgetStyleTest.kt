package com.xukunz.wakeupmywall.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetStyleTest {

    @Test
    fun `solid is opaque and glass stays translucent enough to show the wallpaper`() {
        assertEquals(1f, WidgetStyle.Solid.surfaceAlpha())
        assertTrue(WidgetStyle.Glass.surfaceAlpha() < 1f, "Glass 必须透出壁纸")
        assertTrue(WidgetStyle.Minimal.surfaceAlpha() < WidgetStyle.Glass.surfaceAlpha())
    }

    @Test
    fun `only minimal drops the border`() {
        assertTrue(WidgetStyle.Glass.showsBorder())
        assertTrue(WidgetStyle.Solid.showsBorder())
        assertFalse(WidgetStyle.Minimal.showsBorder())
    }
}
