package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 指标语义色是**图形元素**（环、圆点、细条），按 WCAG 2.1 对非文本内容的要求取 3:1。
 * 卡片底是 `DarkSurface.card`（叠在壁纸上时更亮一点，因此这里是最严格的下界检验）。
 */
class MetricColorsTest {

    private val surfaces = listOf(DarkSurface.card, DarkSurface.background)

    private val palette: Map<String, Color> = buildMap {
        put("cpu", MetricColors.cpu)
        put("gpu", MetricColors.gpu)
        put("ram", MetricColors.ram)
        put("storage", MetricColors.storage)
        put("network", MetricColors.network)
        put("temperature", MetricColors.temperature)
        put("temperatureWarm", MetricColors.temperatureWarm)
        put("fan", MetricColors.fan)
        put("cpuIcon", MetricColors.cpuIcon)
        put("gpuIcon", MetricColors.gpuIcon)
        put("ramIcon", MetricColors.ramIcon)
        put("storageIcon", MetricColors.storageIcon)
        put("networkIcon", MetricColors.networkIcon)
        put("uptimeIcon", MetricColors.uptimeIcon)
        put("activityIcon", MetricColors.activityIcon)
        MetricColors.activityDots.forEachIndexed { index, color -> put("activityDot$index", color) }
    }

    @Test
    fun `every metric color stays visible on both dark surfaces`() {
        palette.forEach { (name, color) ->
            surfaces.forEach { surface ->
                val ratio = contrastRatio(color, surface)
                assertTrue(ratio >= 3.0, "$name on $surface 对比度只有 $ratio，低于图形元素的 3:1")
            }
        }
    }

    @Test
    fun `the four metric rings are distinguishable from each other`() {
        val rings = listOf(MetricColors.cpu, MetricColors.gpu, MetricColors.ram, MetricColors.storage)
        assertTrue(rings.toSet().size == rings.size, "指标环的颜色必须互不相同：$rings")
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun Color.luminance(): Double {
        val r = linear(red)
        val g = linear(green)
        val b = linear(blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linear(channel: Float): Double {
        val v = channel.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
}
