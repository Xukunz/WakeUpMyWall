package com.xukunz.wakeupmywall.ui.monitor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.ui.components.WidgetStyle
import kotlin.test.Test

/**
 * Phase 5B：真实读数可能缺项，Monitor 必须**如实地**显示 `—`（而不是 0、也不是 `null`），
 * 并且把"这份数据是什么时候采的 / 还新不新鲜"标出来。
 */
@OptIn(ExperimentalTestApi::class)
class MonitorMetricsTest {

    private val history = mapOf(MetricKeys.Cpu to List(60) { 20f }, MetricKeys.Network to List(60) { 5f })

    @Test
    fun `missing readings render as an em dash instead of zero`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                MonitorMode(
                    metrics = MetricsSnapshot.Unknown,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    identity = MockData.hardware,
                )
            }
        }

        onNodeWithTag("metric:cpu-ring").assertTextEquals("—")
        onNodeWithTag("metric:cpu-footer-primary").assertTextEquals("— GHz")
        onNodeWithTag("metric:cpu-footer-secondary").assertTextEquals("— cores — threads")
        onNodeWithTag("metric:temps-cpu", useUnmergedTree = true).assertTextEquals("—")
        onNodeWithTag("metric:fans-cpu", useUnmergedTree = true).assertTextEquals("— RPM")
        onNodeWithTag("metric:network-down", useUnmergedTree = true).assertTextEquals("↓— Mbps")
        onNodeWithTag("metric:uptime-value", useUnmergedTree = true).assertTextEquals("—")
        // spec §8 的 Recent Activity 是 P2：没有数据就明说，不摆四条假记录。
        onNodeWithTag("monitor:activity-empty", useUnmergedTree = true).assertTextEquals("—")
    }

    @Test
    fun `a fresh sample is labelled with its capture time`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                MonitorMode(
                    metrics = MockData.metrics,
                    history = history,
                    style = WidgetStyle.Glass,
                    identity = MockData.hardware,
                    capturedLabel = "21:04:32",
                )
            }
        }

        onNodeWithTag("monitor:captured", useUnmergedTree = true).assertTextEquals("Updated 21:04:32")
        onNodeWithTag("monitor:stale").assertDoesNotExist()
    }

    @Test
    fun `a stale sample is labelled as the last update and flagged`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                MonitorMode(
                    metrics = MetricsSnapshot.Unknown,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    identity = MockData.hardware,
                    capturedLabel = "21:04:32",
                    isStale = true,
                )
            }
        }

        onNodeWithTag("monitor:captured", useUnmergedTree = true).assertTextEquals("Last update 21:04:32")
        onNodeWithTag("monitor:stale", useUnmergedTree = true).assertTextEquals("No fresh metrics")
    }

    @Test
    fun `the failure reason is shown when the agent rejects the request`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                MonitorMode(
                    metrics = MetricsSnapshot.Unknown,
                    history = emptyMap(),
                    style = WidgetStyle.Glass,
                    identity = MockData.hardware,
                    isStale = true,
                    metricsNote = "missing or invalid token",
                )
            }
        }

        onNodeWithTag("monitor:metrics-note", useUnmergedTree = true)
            .assertTextEquals("missing or invalid token")
    }
}
