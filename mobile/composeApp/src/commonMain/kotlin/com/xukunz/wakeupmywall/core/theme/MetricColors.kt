package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.graphics.Color

/**
 * 指标语义色。
 *
 * 取值来自概念图 `imgs/concept/cb576a75-…png` 的像素实测（对每张指标卡的环形区域做
 * 高饱和像素统计，取众数），不是拍脑袋定的色板：
 *
 * | 用途 | 概念图实测 | 本文件取值 |
 * | --- | --- | --- |
 * | CPU 环 | `#00C0F0` | [cpu] |
 * | GPU 环 | `#18F048` | [gpu] |
 * | RAM 环 | `#4878F0` | [ram] |
 * | Storage 环 | `#18A8F0` | [storage] |
 * | 温度点（常温 / 偏热） | `#48F0A8` / `#F0A860` | [temperature] / [temperatureWarm] |
 * | 风扇点 | `#48F0A8` | [fan] |
 * | 最近活动点 | `#F0A818` / `#78F078` / `#F0A8C0` | [activityDots] |
 *
 * 这些是**图形元素**（环、点、细条），按 WCAG 对非文本内容的要求取 3:1 对比度，
 * `MetricColorsTest` 会把这条断言固化下来；文字仍然只走 `colorScheme`。
 * Phase 5 接入真实指标后如果要改配色，改这里一处即可。
 */
object MetricColors {
    val cpu = Color(0xFF2BC7F5)
    val gpu = Color(0xFF31E05C)
    val ram = Color(0xFF5B7BF5)
    val storage = Color(0xFF28AEF2)
    val network = Color(0xFF52D8F2)
    val temperature = Color(0xFF4DE88C)
    val temperatureWarm = Color(0xFFF5A930)
    val fan = Color(0xFF45E8A8)

    /** 指标卡标题行的小图标。概念图里它们是同一族的青蓝色，只有 RAM / Storage 略灰。 */
    val cpuIcon = Color(0xFF52D8F2)
    val gpuIcon = Color(0xFF52EDF2)
    val ramIcon = Color(0xFFA8D8F0)
    val storageIcon = Color(0xFF90B0DC)
    val networkIcon = Color(0xFF52D8F2)
    val uptimeIcon = Color(0xFFB7E3F6)
    val activityIcon = Color(0xFF8FB6C8)

    /** 最近活动列表的圆点，按行循环取色（概念图实测值）。 */
    val activityDots: List<Color> = listOf(
        Color(0xFFF0A818),
        Color(0xFF6E7A8C),
        Color(0xFF78F078),
        Color(0xFFF0A8C0),
    )
}
