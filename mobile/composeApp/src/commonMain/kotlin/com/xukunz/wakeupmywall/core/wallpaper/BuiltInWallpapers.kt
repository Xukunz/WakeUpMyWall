package com.xukunz.wakeupmywall.core.wallpaper

/**
 * 内置壁纸编目。`scrimAlpha` 是压在壁纸上的黑色遮罩强度：
 * 浅色壁纸必须压得更狠，否则暗色主题的正文与状态文字会失去对比度。
 * 真实持久化与自定义壁纸（Photo Picker）按路标留到 Phase 7。
 */
data class Wallpaper(
    val id: String,
    val label: String,
    val isLight: Boolean,
    val scrimAlpha: Float,
)

object BuiltInWallpapers {
    /** 备选壁纸：极光（暗色）。母版见 imgs/wallpaper/wallpaper_aurora.png。 */
    val Aurora = Wallpaper(id = "aurora", label = "Aurora", isLight = false, scrimAlpha = 0.18f)

    /** 备选壁纸：Minimal（浅色）。母版见 imgs/wallpaper/wallpaper_minimal.png。 */
    val Minimal = Wallpaper(id = "minimal", label = "Minimal", isLight = true, scrimAlpha = 0.62f)

    /**
     * 2026-09-20 补齐的 5 张母版（imgs/wallpaper/，3840×2160 无损 PNG，只留档不打包）。
     * `scrimAlpha` 按母版实测平均亮度定：越亮的壁纸压得越狠，保证暗色主题下的正文对比度。
     * 实测平均亮度（0–255）：aurora 65 / dusk_lake 67 / space 37 / city_night 36 /
     * forest_mist 83 / cherry 104 / minimal 206。
     */
    /**
     * **默认壁纸**：Dusk Lake（暗色）。概念图 F 行把 `Dusk Lake` 标成默认（带对勾），
     * 2026-09-20 用户确认按概念图走，因此默认值从 `aurora` 改成它。
     */
    val DuskLake = Wallpaper(id = "dusk_lake", label = "Dusk Lake", isLight = false, scrimAlpha = 0.30f)
    val ForestMist = Wallpaper(id = "forest_mist", label = "Forest Mist", isLight = false, scrimAlpha = 0.32f)
    val CityNight = Wallpaper(id = "city_night", label = "City Night", isLight = false, scrimAlpha = 0.20f)
    val Space = Wallpaper(id = "space", label = "Space", isLight = false, scrimAlpha = 0.18f)
    val Cherry = Wallpaper(id = "cherry", label = "Cherry Blossom", isLight = false, scrimAlpha = 0.36f)

    const val DefaultId: String = "dusk_lake"

    val all: List<Wallpaper> = listOf(DuskLake, Aurora, ForestMist, CityNight, Space, Cherry, Minimal)

    /**
     * 未知或缺失的 id 一律回落到**默认壁纸**（而不是写死 aurora），避免脏设置导致空白背景。
     * 默认值改成 Dusk Lake 时这里如果还写 `?: Aurora`，界面上就会悄悄显示另一张图。
     */
    fun byId(id: String?): Wallpaper =
        all.firstOrNull { it.id == id }
            ?: all.firstOrNull { it.id == DefaultId }
            ?: error("默认壁纸 $DefaultId 不在 BuiltInWallpapers.all 里")
}
