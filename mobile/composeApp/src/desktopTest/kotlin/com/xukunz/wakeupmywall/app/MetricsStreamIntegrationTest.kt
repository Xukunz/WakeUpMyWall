package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.xukunz.wakeupmywall.core.agent.InMemoryAgentTokenStore
import com.xukunz.wakeupmywall.core.metrics.MetricsStream
import com.xukunz.wakeupmywall.core.network.AgentCpuMetrics
import com.xukunz.wakeupmywall.core.network.AgentGpuMetrics
import com.xukunz.wakeupmywall.core.network.AgentIdentity
import com.xukunz.wakeupmywall.core.network.AgentMemoryMetrics
import com.xukunz.wakeupmywall.core.network.AgentMetrics
import com.xukunz.wakeupmywall.core.network.AgentNetworkMetrics
import com.xukunz.wakeupmywall.core.network.AgentStorageMetrics
import com.xukunz.wakeupmywall.core.network.AgentThermalMetrics
import com.xukunz.wakeupmywall.core.network.ApiResult
import com.xukunz.wakeupmywall.domain.model.PcState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlin.test.Test

/**
 * Phase 5C：WebSocket 帧 → 界面；流不可用时回退到 HTTP 轮询。
 * 流是注入的假实现，所以这两条路径都能在不联网、不起服务的情况下验。
 */
@OptIn(ExperimentalTestApi::class)
class MetricsStreamIntegrationTest {

    private fun sample(hostname: String, cpuPercent: Float): AgentMetrics = AgentMetrics(
        capturedAtUtc = "2026-09-21T01:12:00Z",
        identity = AgentIdentity(
            hostname = hostname,
            os = "Windows 11 Pro",
            cpuName = "AMD Ryzen 7 7700X",
            cpuShortName = "Ryzen 7 7700X",
            gpuName = "NVIDIA GeForce RTX 4070 Ti",
            gpuShortName = "RTX 4070 Ti",
            ramModule = "32 GB DDR5-6000",
            storageModule = "NVMe 2 TB",
        ),
        cpu = AgentCpuMetrics(name = "Ryzen 7 7700X", usagePercent = cpuPercent, clockGhz = 4.3f, cores = 8, threads = 16, tempC = 42f, fanRpm = 980),
        gpu = AgentGpuMetrics(name = "RTX 4070 Ti", usagePercent = 8f, tempC = 38f, vramUsedGb = 2.4f, vramTotalGb = 12f, fanRpm = 1200),
        memory = AgentMemoryMetrics(usagePercent = 38f, usedGb = 12.2f, totalGb = 32f),
        storage = AgentStorageMetrics(usagePercent = 95f, usedTb = 1.9f, totalTb = 2f, freeGb = 102f, tempC = 41f),
        thermal = AgentThermalMetrics(motherboardTempC = 35f, caseFanRpm = 870),
        network = AgentNetworkMetrics(downloadMbps = 12.4f, uploadMbps = 3.1f),
        uptimeSeconds = 289_440,
        bootedAtUtc = "2025-04-18T12:00:00Z",
    )

    private class ScriptedStream(private val frames: () -> Flow<AgentMetrics>) : MetricsStream {
        override fun frames(baseUrl: String, token: String): Flow<AgentMetrics> = frames()
    }

    private fun ComposeUiTest.awaitTag(tag: String) = waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `stream frames drive the monitor`() = runComposeUiTest {
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                metricsPollMillis = 60_000,      // 轮询退到一边：这条只验流
                metricsStreamFactory = {
                    ScriptedStream { flow { while (true) { emit(sample("STREAM-PC", 37.4f)); delay(10) } } }
                },
            )
        }

        awaitTag("monitor:captured")
        onNodeWithTag("metric:cpu-ring").assertTextEquals("37%")
        onNodeWithTag("monitor:identity-hostname", useUnmergedTree = true).assertTextEquals("STREAM-PC")
    }

    @Test
    fun `a stream that never delivers falls back to http polling`() = runComposeUiTest {
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                metricsPollMillis = 20,
                metricsProbe = { _, _ -> ApiResult.Success(sample("HTTP-PC", 55.4f)) },
                metricsStreamFactory = { ScriptedStream { emptyFlow() } },   // 老 Agent：连上即结束
            )
        }

        // 两次"一帧都没收到"之后，HTTP 轮询接手。
        waitUntilExactlyOneExists(hasTestTag("metric:cpu-ring") and hasText("55%"), timeoutMillis = 8_000)
        onNodeWithTag("monitor:identity-hostname", useUnmergedTree = true).assertTextEquals("HTTP-PC")
    }
}
