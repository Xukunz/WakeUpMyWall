package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.text.TextStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesignTokensTest {

    private val scale: List<Pair<String, TextStyle>> = listOf(
        "label" to AppTypography.label,
        "body" to AppTypography.body,
        "title" to AppTypography.title,
        "metricValue" to AppTypography.metricValue,
        "display" to AppTypography.display,
    )

    @Test
    fun `font sizes form a strictly increasing scale`() {
        scale.zipWithNext().forEach { (smaller, larger) ->
            assertTrue(
                smaller.second.fontSize < larger.second.fontSize,
                "${smaller.first}(${smaller.second.fontSize}) 应小于 ${larger.first}(${larger.second.fontSize})",
            )
        }
    }

    @Test
    fun `every token pins an explicit font size and weight`() {
        scale.forEach { (name, style) ->
            assertTrue(style.fontSize.isSp, "$name 必须显式指定 sp")
            assertTrue(style.fontWeight != null, "$name 必须显式指定字重")
        }
    }

    @Test
    fun `metric unit is readable but clearly smaller than the value`() {
        assertTrue(AppTypography.metricUnit.fontSize < AppTypography.metricValue.fontSize)
    }

    @Test
    fun `shape scale is distinct per purpose`() {
        val radii = listOf(AppShapes.card, AppShapes.button, AppShapes.badge)
            .map { it.topStart }
        assertEquals(radii.toSet().size, radii.size, "卡片 / 按钮 / 徽标圆角必须互相区分：$radii")
    }
}
