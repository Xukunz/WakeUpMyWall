package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MonitorModeTest {

    private val history = mapOf(
        MetricKeys.Cpu to List(60) { 20f + it % 10 },
        MetricKeys.Gpu to List(60) { 60f + it % 5 },
        MetricKeys.Ram to List(60) { 38f },
    )

    @Test
    fun `renders every monitor block from the concept`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware) } }

        listOf(
            "monitor:identity", "monitor:quick-actions",
            "metric:cpu", "metric:gpu", "metric:ram", "metric:storage",
            "metric:temps", "metric:fans", "metric:network", "metric:uptime",
            "monitor:activity", "monitor:quote",
        ).forEach { onNodeWithTag(it).assertExists() }
    }

    @Test
    fun `quick actions expose the four concept entries`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware) } }

        listOf("Browser", "Discord", "Steam", "Spotify").forEach { action ->
            onNodeWithTag("action:$action").assertExists()
        }
    }

    @Test
    fun `metric card shows ring percent and footer values`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware) } }

        onNodeWithTag("metric:cpu-ring").assertTextEquals("28%")
        onNodeWithTag("metric:cpu-footer-primary").assertTextEquals("4.9 GHz")
        onNodeWithTag("metric:cpu-footer-secondary").assertTextEquals("8 cores 16 threads")
    }

    @Test
    fun `cpu card shows percent value`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware) } }

        // 百分数只出现在环心（权威规格 C），因此断言环心节点而非另设的大号数值。
        onNodeWithTag("metric:cpu-ring").assertTextEquals("28%")
    }

    @Test
    fun `sparkline renders without history`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, emptyMap(), WidgetStyle.Glass, identity = MockData.hardware) } }

        onNodeWithTag("metric:cpu").assertExists()
    }

    @Test
    fun `identity card and temps carry the concept numbers`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware) } }

        onNodeWithTag("monitor:identity-hostname", useUnmergedTree = true).assertTextEquals("DESKTOP-ALPHA")
        onNodeWithTag("monitor:identity-os", useUnmergedTree = true).assertTextEquals("Windows 11 Pro")
        onNodeWithTag("metric:temps-cpu", useUnmergedTree = true).assertTextEquals("CPU 68°C")
        onNodeWithTag("metric:fans-cpu", useUnmergedTree = true).assertTextEquals("CPU Fan 1,240 RPM")
        onNodeWithTag("metric:uptime-value").assertTextEquals("3d 6h 24m")
    }
}
