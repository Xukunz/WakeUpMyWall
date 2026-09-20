package com.xukunz.wakeupmywall.ui.settings

import com.xukunz.wakeupmywall.core.theme.ThemeAccent
import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceStateTest {

    private val state = AppearanceState(
        accent = ThemeAccent.AuroraBlue,
        widgetStyle = WidgetStyle.Glass,
        wallpaperId = BuiltInWallpapers.DefaultId,
        transparency = 0.35f,
        fontScale = 1f,
        widgets = DashboardLayout.default,
    )

    @Test
    fun `toggle widget flips enabled flag`() {
        val updated = AppearanceReducer.toggleWidget(state, "weather")

        assertFalse(updated.widgets.first { it.id == "weather" }.enabled)
    }

    @Test
    fun `toggle widget keeps every other widget untouched`() {
        val updated = AppearanceReducer.toggleWidget(state, "weather")

        assertEquals(state.widgets.size, updated.widgets.size)
        assertEquals(
            state.widgets.filterNot { it.id == "weather" },
            updated.widgets.filterNot { it.id == "weather" },
        )
    }

    @Test
    fun `set accent replaces accent only`() {
        val updated = AppearanceReducer.setAccent(state, ThemeAccent.Emerald)

        assertEquals(ThemeAccent.Emerald, updated.accent)
        assertEquals(state.widgets, updated.widgets)
        assertEquals(state.wallpaperId, updated.wallpaperId)
    }

    @Test
    fun `set widget style replaces style only`() {
        val updated = AppearanceReducer.setWidgetStyle(state, WidgetStyle.Solid)

        assertEquals(WidgetStyle.Solid, updated.widgetStyle)
        assertEquals(state.accent, updated.accent)
    }

    @Test
    fun `set wallpaper validates against the bundled catalogue`() {
        assertEquals(BuiltInWallpapers.Minimal.id, AppearanceReducer.setWallpaper(state, BuiltInWallpapers.Minimal.id).wallpaperId)
        // 非法 id 保持原值，不抛异常（脏设置不能把界面打崩）。
        assertEquals(state.wallpaperId, AppearanceReducer.setWallpaper(state, "unknown").wallpaperId)
    }

    @Test
    fun `bundled catalogue only exposes wallpapers that ship an asset`() {
        // 概念图 F 行的 7 张缩略图里，只有用户提供的两张有真实母版；其余不进目录，
        // 否则 WallpaperBackground 会命中 "Unmapped built-in wallpaper"。
        assertEquals(listOf("aurora", "minimal"), BuiltInWallpapers.all.map { it.id })
    }

    @Test
    fun `default appearance uses the aurora master with glass and aurora blue`() {
        assertEquals(BuiltInWallpapers.DefaultId, state.wallpaperId)
        assertEquals(WidgetStyle.Glass, state.widgetStyle)
        assertEquals(ThemeAccent.AuroraBlue, state.accent)
        assertTrue(state.widgets.any { it.enabled })
    }
}
