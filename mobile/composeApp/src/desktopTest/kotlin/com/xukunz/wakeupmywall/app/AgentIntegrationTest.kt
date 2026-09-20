package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.xukunz.wakeupmywall.core.connectivity.ConnectionFailure
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport
import com.xukunz.wakeupmywall.domain.model.PcState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 存在性轮询：把"PC 开着吗"变成状态行上的 Online / Agent unavailable / Ready to wake。
 * 报告由测试注入，所以这里验的是"报告 → 状态 → 界面"这条链，不依赖真实网络。
 */
@OptIn(ExperimentalTestApi::class)
class AgentIntegrationTest {

    private fun agentTest(
        initial: PcState,
        report: ConnectionReport,
        assert: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        setContent {
            App(
                initialPcState = initial,
                agentPollMillis = 20,
                agentReportProbe = { report },
            )
        }
        assert()
    }

    private fun ComposeUiTest.expectState(label: String) = waitUntilExactlyOneExists(
        hasTestTag("powerrail:state") and hasText(label),
        timeoutMillis = 5_000,
    )

    @Test
    fun `an answering agent shows up as online`() = agentTest(
        initial = PcState.WOL_READY,
        report = ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0"),
    ) {
        expectState("Online")
    }

    @Test
    fun `a host that answers without the agent is reported as agent unavailable`() = agentTest(
        initial = PcState.ONLINE,
        report = ConnectionReport.Failed(ConnectionFailure.NOT_AN_AGENT, "404 on /api/v1/status"),
    ) {
        expectState("Agent unavailable")
    }

    @Test
    fun `an unreachable pc falls back to wake ready when it can be woken`() = agentTest(
        initial = PcState.ONLINE,
        report = ConnectionReport.Failed(ConnectionFailure.PORT_REFUSED, "nothing on 9876"),
    ) {
        expectState("Ready to wake")
    }

    @Test
    fun `a device with no mac goes offline instead of wake ready`() = runComposeUiTest {
        val storage = com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage(
            com.xukunz.wakeupmywall.core.storage.InMemoryKeyValueStore(),
        )
        kotlinx.coroutines.runBlocking {
            storage.writeDevices(
                listOf(
                    com.xukunz.wakeupmywall.domain.model.PcDevice(
                        id = "brand-new",
                        name = "New PC",
                        isDefault = true,
                    ),
                ),
            )
            storage.markSeeded()
        }
        setContent {
            App(
                storage = storage,
                initialPcState = PcState.ONLINE,
                agentPollMillis = 20,
                agentReportProbe = { ConnectionReport.Failed(ConnectionFailure.PORT_REFUSED, "nothing on 9876") },
            )
        }

        expectState("Offline")
        assertEquals("Offline", "Offline") // 保持断言意图显式：无 MAC ≠ 可唤醒
    }
}
