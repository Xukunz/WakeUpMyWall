package com.xukunz.wakeupmywall.ui.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HomeSurfaceTest {

    private val dashboardData = DashboardData(
        greeting = MockData.greetingText,
        time = MockData.clockTime,
        date = MockData.calendarDateLabel,
        weather = MockData.weather,
        events = MockData.calendarEvents,
        todos = MockData.todos,
        pc = powerRailModel(PcState.ONLINE, MockData.defaultDevice),
        pcSummary = MockData.summaryMetrics,
        widgets = DashboardLayout.default,
    )

    @Test
    fun `dashboard is the default surface`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = HomeMode.Dashboard,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = {},
                    identity = MockData.hardware,
                )
            }
        }

        onNodeWithTag("dashboard:greeting").assertIsDisplayed()
    }

    @Test
    fun `pc summary click switches to monitor`() = runComposeUiTest {
        var mode = HomeMode.Dashboard
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = mode,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = { mode = it },
                    identity = MockData.hardware,
                )
            }
        }

        onNodeWithTag("dashboard:pc-summary").performClick()

        assertEquals(HomeMode.Monitor, mode)
    }

    @Test
    fun `swiping left requests standby and swiping right requests dashboard`() = runComposeUiTest {
        var mode = HomeMode.Dashboard
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = mode,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = { mode = it },
                    identity = MockData.hardware,
                )
            }
        }

        // 硬件页不是主页之一：左滑从 Dashboard 直接去 StandBy（Monitor 只能点 PC 卡片进）。
        onNodeWithTag("home:surface").performTouchInput { swipeLeft() }
        assertEquals(HomeMode.StandBy, mode)

        onNodeWithTag("home:surface").performTouchInput { swipeRight() }
        assertEquals(HomeMode.Dashboard, mode)
    }

    @Test
    fun `monitor mode renders metric cards`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = HomeMode.Monitor,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = {},
                    identity = MockData.hardware,
                )
            }
        }

        onNodeWithTag("metric:cpu").assertExists()
    }

    @Test
    fun `the monitor is not a swipe page so its own gestures stay free`() = runComposeUiTest {
        var mode by mutableStateOf(HomeMode.Monitor)
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = mode,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = { mode = it },
                    identity = MockData.hardware,
                )
            }
        }

        // Monitor 不接横向手势：这里的拖动是留给卡片自己的（例如存储卡的翻页），
        // 所以左右滑都不应该改变形态。
        onNodeWithTag("home:surface").performTouchInput { swipeLeft() }
        assertEquals(HomeMode.Monitor, mode)

        onNodeWithTag("home:surface").performTouchInput { swipeRight() }
        assertEquals(HomeMode.Monitor, mode)
    }

    @Test
    fun `monitor identity card returns to dashboard`() = runComposeUiTest {
        var mode = HomeMode.Monitor
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = mode,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = { mode = it },
                    identity = MockData.hardware,
                )
            }
        }

        onNodeWithTag("monitor:identity").performClick()

        assertEquals(HomeMode.Dashboard, mode)
    }

    @Test
    fun `monitor identity card carries the status line from the concept`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = HomeMode.Monitor,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = {},
                    identity = MockData.hardware,
                )
            }
        }

        onNodeWithTag("monitor:identity-name", useUnmergedTree = true).assertTextEquals("My PC")
        onNodeWithTag("monitor:identity-state", useUnmergedTree = true).assertTextEquals("Online")
        onNodeWithTag("monitor:identity-lastseen", useUnmergedTree = true).assertTextEquals("Last seen 1 min ago")
    }

    @Test
    fun `standby shows the long date while the dashboard keeps the short one`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                HomeSurface(
                    mode = HomeMode.StandBy,
                    dashboard = dashboardData,
                    metrics = MockData.metrics,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    onModeChange = {},
                    identity = MockData.hardware,
                )
            }
        }

        // 权威规格 D 的长日期；同一份 dashboard.date 是规格 B2 的 `Tue, Apr 22`。
        onNodeWithTag("standby:clock-date", useUnmergedTree = true).assertTextEquals("Tuesday, April 22")
    }
}
