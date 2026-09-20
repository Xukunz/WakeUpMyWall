package com.xukunz.wakeupmywall.ui.components

enum class WidgetStyle { Glass, Solid, Minimal }

/** 卡片底色不透明度：Glass 透出壁纸、Solid 完全遮挡、Minimal 只留一层极淡的底。 */
fun WidgetStyle.surfaceAlpha(): Float = when (this) {
    WidgetStyle.Glass -> 0.6f
    WidgetStyle.Solid -> 1f
    WidgetStyle.Minimal -> 0.08f
}

fun WidgetStyle.showsBorder(): Boolean = this != WidgetStyle.Minimal
