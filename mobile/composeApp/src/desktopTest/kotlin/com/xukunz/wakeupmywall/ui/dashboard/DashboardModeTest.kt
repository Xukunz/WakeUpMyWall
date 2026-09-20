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
        onNodeWithTag("dashboard:weather-range").assertTextEquals("↑22° ↓14°")
        onNodeWithTag("dashboard:todo-count").assertTextEquals("3 of 5")
        // PC 摘要整卡是可点击的，子节点语义被合并进卡片，断言需显式读未合并树。
        onNodeWithTag("dashboard:pc-cpu", useUnmergedTree = true).assertTextEquals("CPU 12%")
        onNodeWithTag("dashboard:pc-temp", useUnmergedTree = true).assertTextEquals("Temp 42°C")
        onNodeWithTag("dashboard:pc-ram", useUnmergedTree = true).assertTextEquals("RAM 38%")
    }
}
