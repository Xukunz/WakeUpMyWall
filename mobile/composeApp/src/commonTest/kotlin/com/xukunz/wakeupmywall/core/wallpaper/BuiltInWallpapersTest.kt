package com.xukunz.wakeupmywall.core.wallpaper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuiltInWallpapersTest {

    @Test
    fun `default wallpaper is the dark dusk lake master`() {
        // 概念图 F 行标的是 Dusk Lake（带对勾），2026-09-20 用户确认按概念图走。
        assertEquals("dusk_lake", BuiltInWallpapers.DefaultId)
        val default = BuiltInWallpapers.byId(BuiltInWallpapers.DefaultId)
        assertFalse(default.isLight, "默认壁纸必须是暗色，否则暗色主题的正文对比度不成立")
    }

    @Test
    fun `missing or unknown id falls back to default instead of failing`() {
        assertEquals(BuiltInWallpapers.DefaultId, BuiltInWallpapers.byId(null).id)
        assertEquals(BuiltInWallpapers.DefaultId, BuiltInWallpapers.byId("does-not-exist").id)
    }

    @Test
    fun `catalogue exposes distinct ids with human labels`() {
        val ids = BuiltInWallpapers.all.map { it.id }

        assertEquals(ids.toSet().size, ids.size, "壁纸 id 必须唯一：$ids")
        assertTrue(ids.contains(BuiltInWallpapers.DefaultId))
        BuiltInWallpapers.all.forEach { wallpaper ->
            assertTrue(wallpaper.label.isNotBlank(), wallpaper.id)
            assertTrue(wallpaper.scrimAlpha in 0f..1f, "${wallpaper.id} scrim=${wallpaper.scrimAlpha}")
        }
    }

    @Test
    fun `light wallpapers need a stronger scrim than dark ones`() {
        val dark = BuiltInWallpapers.all.filterNot { it.isLight }
        val light = BuiltInWallpapers.all.filter { it.isLight }

        assertTrue(dark.isNotEmpty() && light.isNotEmpty(), "内置壁纸需同时覆盖暗色与浅色")
        assertTrue(
            light.minOf { it.scrimAlpha } > dark.maxOf { it.scrimAlpha },
            "浅色壁纸的遮罩必须强于暗色壁纸，才能保住暗色主题的文字对比度",
        )
    }
}
