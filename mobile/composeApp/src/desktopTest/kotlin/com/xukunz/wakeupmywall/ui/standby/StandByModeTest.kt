package com.xukunz.wakeupmywall.ui.standby

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
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.HomeMode
import com.xukunz.wakeupmywall.ui.dashboard.HomeModeController
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class StandByModeTest {

    private val railModel = powerRailModel(PcState.ONLINE, MockData.defaultDevice)

    private val standByData = DashboardData(
        greeting = MockData.greetingText,
        time = MockData.clockTime,
        date = MockData.calendarDateLabel,
        weather = MockData.weather,
        events = MockData.calendarEvents,
        todos = MockData.todos,
        pc = railModel,
        pcSummary = MockData.summaryMetrics,
        widgets = DashboardLayout.default,
    )

    @Test
    fun `renders clock weather next event and pc card`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { StandByMode(standByData, railModel, MockData.nextEvent, {}, {}) } }

        listOf("standby:clock", "standby:weather", "standby:next-event", "standby:pc-card", "standby:status-bar")
            .forEach { onNodeWithTag(it).assertIsDisplayed() }
    }

    @Test
    fun `clock shows the minute in the accent slot`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { StandByClock("9:41", "PM", "Tuesday, April 22") } }

        onNodeWithTag("standby:clock-hour").assertTextEquals("9:")
        onNodeWithTag("standby:clock-minute").assertTextEquals("41")
        onNodeWithTag("standby:clock-meridiem").assertTextEquals("PM")
        onNodeWithTag("standby:clock-date").assertTextEquals("Tuesday, April 22")
    }

    @Test
    fun `pc card exposes night mode and auto dim instead of power actions`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { StandByMode(standByData, railModel, MockData.nextEvent, {}, {}) } }

        onNodeWithTag("standby:night-mode").assertExists()
        onNodeWithTag("standby:auto-dim").assertExists()
        // StandBy 不渲染常驻 Power Rail，因此不得出现 powerrail:* 标签。
        onNodeWithTag("powerrail:sleep").assertDoesNotExist()
        onNodeWithTag("powerrail").assertDoesNotExist()
    }

    @Test
    fun `next event carries the concept copy`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { StandByMode(standByData, railModel, MockData.nextEvent, {}, {}) } }

        onNodeWithTag("standby:next-event-countdown", useUnmergedTree = true).assertTextEquals("In 1 hr 19 min")
        onNodeWithTag("standby:next-event-title", useUnmergedTree = true).assertTextEquals("Team sync")
        onNodeWithTag("standby:next-event-source", useUnmergedTree = true).assertTextEquals("Microsoft Teams")
    }

    @Test
    fun `pc card is the way back to the monitor`() = runComposeUiTest {
        var opened = false
        setContent { WakeUpMyWallTheme { StandByMode(standByData, railModel, MockData.nextEvent, {}, { opened = true }) } }

        onNodeWithTag("standby:open-monitor").performClick()

        assertEquals(true, opened)
    }

    @Test
    fun `three state controller cycles and clamps at both ends`() {
        val controller = HomeModeController()

        controller.onSwipeLeft()
        assertEquals(HomeMode.Monitor, controller.current.value)
        controller.onSwipeLeft()
        assertEquals(HomeMode.StandBy, controller.current.value)
        controller.onSwipeLeft()
        assertEquals(HomeMode.StandBy, controller.current.value)

        controller.onSwipeRight()
        assertEquals(HomeMode.Monitor, controller.current.value)
        controller.onSwipeRight()
        assertEquals(HomeMode.Dashboard, controller.current.value)
        controller.onSwipeRight()
        assertEquals(HomeMode.Dashboard, controller.current.value)
    }

    @Test
    fun `show jumps to standby directly`() {
        val controller = HomeModeController()

        controller.show(HomeMode.StandBy)

        assertEquals(HomeMode.StandBy, controller.current.value)
    }
}
