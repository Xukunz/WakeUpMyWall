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
    fun `metric cards use the short model names while the identity card keeps the full ones`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { MonitorMode(MockData.metrics, history, WidgetStyle.Glass, identity = MockData.hardware) } }

        // 权威规格 C2：卡片型号小字是 `Ryzen 7 7700X` / `RTX 4070 Ti`；全名只出现在身份卡（C1）。
        onNodeWithTag("metric:cpu-model").assertTextEquals("Ryzen 7 7700X")
        onNodeWithTag("metric:gpu-model").assertTextEquals("RTX 4070 Ti")
        onNodeWithTag("metric:ram-model").assertTextEquals("32 GB DDR5")
        onNodeWithTag("metric:storage-model").assertTextEquals("2 TB NVMe SSD")

        onNodeWithTag("monitor:identity-cpu").assertTextEquals("AMD Ryzen 7 7700X")
        onNodeWithTag("monitor:identity-gpu").assertTextEquals("NVIDIA GeForce RTX 4070 Ti")
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
