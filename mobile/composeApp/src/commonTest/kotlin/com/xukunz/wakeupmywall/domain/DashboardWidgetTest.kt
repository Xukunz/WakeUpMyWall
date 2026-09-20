package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.WidgetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardWidgetTest {

    @Test
    fun `default layout contains all seven widget types`() {
        assertEquals(
            WidgetType.entries.toSet(),
            DashboardLayout.default.map { it.type }.toSet(),
        )
    }

    @Test
    fun `visible filters disabled widgets and respects order`() {
        // 计划原文这里直接断言 size 相等，但默认布局里的 decorative 是关闭的，
        // 与 `visible = 过滤 disabled` 的契约冲突；先全开再验保序，过滤行为由下一条测试单独覆盖。
        val widgets = DashboardLayout.default.map {
            it.copy(enabled = true, order = WidgetType.entries.size - it.order)
        }
        val visible = DashboardLayout.visible(widgets)

        assertEquals(widgets.size, visible.size)
        assertEquals(visible.sortedBy { it.order }, visible)
    }

    @Test
    fun `toggle disables a widget without dropping it`() {
        val toggled = DashboardLayout.toggle(DashboardLayout.default, "weather")

        assertEquals(DashboardLayout.default.size, toggled.size)
        assertTrue(toggled.first { it.id == "weather" }.enabled.not())
    }

    @Test
    fun `visible drops the widgets that are off by default`() {
        val visible = DashboardLayout.visible(DashboardLayout.default)

        // 概念图首页既没有独立时钟卡、也没有几何装饰，两者默认关闭（见 Task 6 与权威规格 B）。
        assertTrue(visible.none { it.type == WidgetType.Decorative })
        assertTrue(visible.none { it.type == WidgetType.Clock })
        assertEquals(DashboardLayout.default.size - 2, visible.size)
    }
}
