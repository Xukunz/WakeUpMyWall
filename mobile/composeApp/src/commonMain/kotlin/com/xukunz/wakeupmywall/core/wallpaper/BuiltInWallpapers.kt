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
    /** 默认壁纸：极光（暗色）。母版见 imgs/wallpaper/wallpaper_aurora.png。 */
    val Aurora = Wallpaper(id = "aurora", label = "Aurora", isLight = false, scrimAlpha = 0.18f)

    /** 备选壁纸：Minimal（浅色）。母版见 imgs/wallpaper/wallpaper_minimal.png。 */
    val Minimal = Wallpaper(id = "minimal", label = "Minimal", isLight = true, scrimAlpha = 0.62f)

    const val DefaultId: String = "aurora"

    val all: List<Wallpaper> = listOf(Aurora, Minimal)

    /** 未知或缺失的 id 一律回落到默认壁纸，避免脏设置导致空白背景。 */
    fun byId(id: String?): Wallpaper = all.firstOrNull { it.id == id } ?: Aurora
}
