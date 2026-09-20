package com.xukunz.wakeupmywall.ui.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
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
    fun `swiping left requests monitor and swiping right requests dashboard`() = runComposeUiTest {
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

        onNodeWithTag("home:surface").performTouchInput { swipeLeft() }
        assertEquals(HomeMode.Monitor, mode)

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
}
