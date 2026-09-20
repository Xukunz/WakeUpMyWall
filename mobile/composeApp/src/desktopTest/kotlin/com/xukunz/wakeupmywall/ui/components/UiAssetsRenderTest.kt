package com.xukunz.wakeupmywall.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.xukunz.wakeupmywall.core.theme.AppSizes
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.DashboardLayout
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.ui.dashboard.DashboardData
import com.xukunz.wakeupmywall.ui.dashboard.DashboardMode
import com.xukunz.wakeupmywall.ui.icons.PowerGlyph
import com.xukunz.wakeupmywall.ui.monitor.MonitorMode
import com.xukunz.wakeupmywall.ui.powerrail.PowerRingButton
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlin.test.Test

/**
 * 交付素材的接入回归：素材是 PNG 位图，编译器不会替我们检查"这张图到底有没有被用上"，
 * 因此用渲染测试把每个接入点钉住——图缺了、资源名拼错了、派生脚本没跑，都在这里炸。
 */
@OptIn(ExperimentalTestApi::class)
class UiAssetsRenderTest {

    private val dashboardData = DashboardData(
        greeting = MockData.greetingText,
        time = MockData.clockTime,
        date = MockData.calendarDateLabel,
        weather = MockData.weather,
        events = MockData.calendarEvents,
        todos = MockData.todos,
        pc = powerRailModel(PcState.WOL_READY, MockData.defaultDevice),
        pcSummary = MockData.summaryMetrics,
        widgets = DashboardLayout.default,
    )

    @Test
    fun `weather icons render on the dashboard`() = runDesktopComposeUiTest(1280, 720) {
        setContent { WakeUpMyWallTheme { DashboardMode(dashboardData, WidgetStyle.Glass, {}) } }

        onNodeWithTag("dashboard:weather-icon").assertIsDisplayed()
        onNodeWithTag("dashboard:weather-hours").assertIsDisplayed()
    }

    @Test
    fun `pc cover renders in the summary card`() = runDesktopComposeUiTest(1280, 720) {
        setContent { WakeUpMyWallTheme { DashboardMode(dashboardData, WidgetStyle.Glass, {}) } }

        // 摘要卡整卡可点击，子节点语义被合并进卡片，缩略图只在未合并树里可见。
        onNodeWithTag("dashboard:pc-cover", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `pc cover renders on the monitor identity card`() = runDesktopComposeUiTest(1280, 720) {
        setContent {
            WakeUpMyWallTheme {
                MonitorMode(MockData.metrics, emptyMap(), WidgetStyle.Glass, identity = MockData.hardware)
            }
        }

        onNodeWithTag("monitor:identity-cover").assertIsDisplayed()
    }

    @Test
    fun `power button renders the base image with the code drawn glyph`() = runDesktopComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                PowerRingButton(enabled = true, onClick = {}, diameter = AppSizes.ringDiameter)
            }
        }

        onNodeWithTag("powerrail:primary").assertIsDisplayed()
        onNodeWithTag("powerrail:primary-glyph", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `standby and rail draw the code glyphs`() = runDesktopComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerGlyph(tint = MaterialTheme.colorScheme.primary) } }
    }
}
