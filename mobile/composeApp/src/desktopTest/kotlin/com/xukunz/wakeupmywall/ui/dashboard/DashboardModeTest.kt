package com.xukunz.wakeupmywall.ui.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DashboardModeTest {

    private val data = DashboardData(
        greeting = "Good Evening",
        time = "21:04",
        date = "Tue, Apr 22",
        weather = MockData.weather,
        events = MockData.calendarEvents,
        todos = MockData.todos,
        pc = powerRailModel(PcState.ONLINE, MockData.devices.first()),
        pcSummary = MockData.summaryMetrics,
        widgets = DashboardLayout.default,
    )

    @Test
    fun `renders greeting weather calendar todo and pc summary`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, {}) } }

        listOf("dashboard:greeting", "dashboard:weather", "dashboard:calendar", "dashboard:todo", "dashboard:pc-summary")
            .forEach { onNodeWithTag(it).assertIsDisplayed() }
    }

    @Test
    fun `quote card brand strip and perspective mark are rendered`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, {}) } }

        onNodeWithTag("dashboard:quote").assertIsDisplayed()
        onNodeWithTag("dashboard:brand").assertIsDisplayed()
        onNodeWithTag("dashboard:perspective").assertIsDisplayed()
    }

    @Test
    fun `clock and decorative widgets stay hidden in the default layout`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, {}) } }

        onNodeWithTag("dashboard:clock").assertDoesNotExist()
        onNodeWithTag("dashboard:decorative").assertDoesNotExist()
    }

    @Test
    fun `clock widget appears when enabled`() = runComposeUiTest {
        val withClock = data.copy(
            widgets = DashboardLayout.default.map { if (it.id == "clock") it.copy(enabled = true) else it },
        )
        setContent { WakeUpMyWallTheme { DashboardMode(withClock, WidgetStyle.Glass, {}) } }

        onNodeWithTag("dashboard:clock").assertIsDisplayed()
    }

    @Test
    fun `pc summary click is forwarded`() = runComposeUiTest {
        var clicked = false
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, onPcSummaryClick = { clicked = true }) } }

        onNodeWithTag("dashboard:pc-summary").performClick()

        assertTrue(clicked)
    }

    @Test
    fun `widgets show the numbers pinned by the concept spec`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { DashboardMode(data, WidgetStyle.Glass, {}) } }

        onNodeWithTag("dashboard:weather-temp").assertTextEquals("18°")
        // 概念图 B 行 2 把最高/最低温排成温度右侧的上下两行，因此是两个节点而不是一个。
        onNodeWithTag("dashboard:weather-high").assertTextEquals("↑22°")
        onNodeWithTag("dashboard:weather-low").assertTextEquals("↓14°")
        onNodeWithTag("dashboard:todo-count").assertTextEquals("3 of 5")
        // PC 摘要整卡是可点击的，子节点语义被合并进卡片，断言需显式读未合并树。
        // 摘要卡改成概念图的"横向四列"后，名称与数值是两个节点（名称在上、数值在下）。
        onNodeWithTag("dashboard:pc-cpu-label", useUnmergedTree = true).assertTextEquals("CPU")
        onNodeWithTag("dashboard:pc-cpu", useUnmergedTree = true).assertTextEquals("12%")
        onNodeWithTag("dashboard:pc-temp", useUnmergedTree = true).assertTextEquals("42°C")
        onNodeWithTag("dashboard:pc-ram", useUnmergedTree = true).assertTextEquals("38%")
    }
}
