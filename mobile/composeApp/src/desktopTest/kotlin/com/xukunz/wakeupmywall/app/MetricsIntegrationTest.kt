package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.xukunz.wakeupmywall.core.agent.InMemoryAgentTokenStore
import com.xukunz.wakeupmywall.core.network.AgentCpuMetrics
import com.xukunz.wakeupmywall.core.network.AgentGpuMetrics
import com.xukunz.wakeupmywall.core.network.AgentIdentity
import com.xukunz.wakeupmywall.core.network.AgentMemoryMetrics
import com.xukunz.wakeupmywall.core.network.AgentMetrics
import com.xukunz.wakeupmywall.core.network.AgentNetworkMetrics
import com.xukunz.wakeupmywall.core.network.AgentStorageMetrics
import com.xukunz.wakeupmywall.core.network.AgentThermalMetrics
import com.xukunz.wakeupmywall.core.network.ApiFailure
import com.xukunz.wakeupmywall.core.network.ApiResult
import com.xukunz.wakeupmywall.domain.model.PcState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 指标采样的集成链路：探针 → LiveMetricsMapper → Monitor 的读数与新鲜度标注。
 * 探针由测试注入，所以这里验的是"采到/采不到 → 界面怎么说话"，不依赖真实网络。
 */
@OptIn(ExperimentalTestApi::class)
class MetricsIntegrationTest {

    /**
     * 身份卡是可点区域，语义树会把子节点合并进父节点——`monitor:stale` 这类标签只在
     * **未合并树**里看得到，所以等待/断言都必须显式 `useUnmergedTree = true`（实测踩到）。
     */
    private fun ComposeUiTest.awaitTag(tag: String) = waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private val sample = AgentMetrics(
        capturedAtUtc = "2026-09-21T00:58:02Z",
        identity = AgentIdentity(
            hostname = "DESKTOP-ALPHA",
            os = "Windows 11 Pro",
            cpuName = "AMD Ryzen 7 7700X 8-Core Processor",
            cpuShortName = "Ryzen 7 7700X",
            gpuName = "NVIDIA GeForce RTX 4070 Ti",
            gpuShortName = "RTX 4070 Ti",
            ramModule = "32 GB DDR5-6000",
            storageModule = "NVMe 2 TB",
        ),
        cpu = AgentCpuMetrics(name = "Ryzen 7 7700X", usagePercent = 12.4f, clockGhz = 4.3f, cores = 8, threads = 16, tempC = 42.4f, fanRpm = 980),
        gpu = AgentGpuMetrics(name = "RTX 4070 Ti", usagePercent = 8f, tempC = 38f, vramUsedGb = 2.4f, vramTotalGb = 12f, fanRpm = 1200),
        memory = AgentMemoryMetrics(usagePercent = 38f, usedGb = 12.2f, totalGb = 32f),
        storage = AgentStorageMetrics(usagePercent = 95f, usedTb = 1.9f, totalTb = 2f, freeGb = 102f, tempC = 41f),
        thermal = AgentThermalMetrics(motherboardTempC = 35f, caseFanRpm = 870),
        network = AgentNetworkMetrics(downloadMbps = 12.4f, uploadMbps = 3.1f),
        uptimeSeconds = 289_440,
        bootedAtUtc = "2025-04-18T12:00:00Z",
    )

    @Test
    fun `the agent numbers replace the preview readings on the monitor`() = runComposeUiTest {
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { com.xukunz.wakeupmywall.core.connectivity.ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0") },
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                metricsPollMillis = 20,
                metricsProbe = { _, _ -> ApiResult.Success(sample) },
            )
        }

        awaitTag("monitor:captured")
        onNodeWithTag("metric:cpu-ring").assertTextEquals("12%")
        onNodeWithTag("metric:cpu-footer-primary").assertTextEquals("4.3 GHz")
        onNodeWithTag("metric:temps-cpu", useUnmergedTree = true).assertTextEquals("42°C")
        onNodeWithTag("metric:fans-cpu", useUnmergedTree = true).assertTextEquals("980 RPM")
        onNodeWithTag("metric:network-down", useUnmergedTree = true).assertTextEquals("↓12.4 Mbps")
        onNodeWithTag("metric:uptime-value", useUnmergedTree = true).assertTextEquals("3d 8h 24m")
        onNodeWithTag("monitor:identity-hostname", useUnmergedTree = true).assertTextEquals("DESKTOP-ALPHA")
        onNodeWithTag("monitor:stale", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `an agent that stops answering turns the readings into em dashes`() = runComposeUiTest {
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { com.xukunz.wakeupmywall.core.connectivity.ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0") },
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                metricsPollMillis = 20,
                metricsProbe = { _, _ -> ApiResult.Failure(ApiFailure.NETWORK, "Nothing is listening on 192.168.1.10:9876") },
            )
        }

        // 3 次失败（≈60 ms）之后才判定掉线：一次抖动不该把读数打回 —。
        awaitTag("monitor:stale")
        onNodeWithTag("metric:cpu-ring").assertTextEquals("—")
        onNodeWithTag("metric:network-down", useUnmergedTree = true).assertTextEquals("↓— Mbps")
        onNodeWithTag("monitor:metrics-note", useUnmergedTree = true)
            .assertTextEquals("Nothing is listening on 192.168.1.10:9876")
    }

    @Test
    fun `a pc that answers again brings the readings back`() = runComposeUiTest {
        var fail = true
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { com.xukunz.wakeupmywall.core.connectivity.ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0") },
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                metricsPollMillis = 20,
                metricsProbe = { _, _ ->
                    if (fail) ApiResult.Failure(ApiFailure.NETWORK, "nothing on 9876") else ApiResult.Success(sample)
                },
            )
        }

        awaitTag("monitor:stale")
        onNodeWithTag("metric:cpu-ring").assertTextEquals("—")

        fail = false
        waitUntilExactlyOneExists(hasTestTag("metric:cpu-ring") and hasText("12%"), timeoutMillis = 5_000)
        onNodeWithTag("monitor:stale", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `without a pairing token the app asks to pair and never samples`() = runComposeUiTest {
        var probes = 0
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { com.xukunz.wakeupmywall.core.connectivity.ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0") },
                agentTokens = InMemoryAgentTokenStore(),                 // 没配过对
                metricsPollMillis = 20,
                metricsProbe = { _, _ -> probes++; ApiResult.Success(sample) },
            )
        }

        awaitTag("monitor:metrics-note")
        onNodeWithTag("monitor:metrics-note", useUnmergedTree = true)
            .assertTextEquals("Pair the phone in Device Setup first (Agent section)")
        assertEquals(0, probes)
    }
}
