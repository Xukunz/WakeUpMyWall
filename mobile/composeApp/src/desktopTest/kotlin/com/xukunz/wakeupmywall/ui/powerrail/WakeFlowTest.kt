package com.xukunz.wakeupmywall.ui.powerrail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.xukunz.wakeupmywall.app.AppNavigator
import com.xukunz.wakeupmywall.app.App
import com.xukunz.wakeupmywall.app.Workspace
import com.xukunz.wakeupmywall.core.connectivity.TcpProbe
import com.xukunz.wakeupmywall.core.connectivity.TcpProbeResult
import com.xukunz.wakeupmywall.core.storage.InMemoryKeyValueStore
import com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage
import kotlinx.coroutines.runBlocking
import com.xukunz.wakeupmywall.core.wol.WakeOnLanSender
import com.xukunz.wakeupmywall.core.wol.WakeSendResult
import com.xukunz.wakeupmywall.domain.model.PcState
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeSender : WakeOnLanSender {
    var sends = 0
    override suspend fun send(packet: ByteArray, target: String, port: Int): WakeSendResult {
        sends++
        return WakeSendResult.Sent(3)
    }
}

@OptIn(ExperimentalTestApi::class)
class WakeFlowTest {

    @Test
    fun `tapping the ring sends a packet and the rail shows waking`() = runComposeUiTest {
        val sender = FakeSender()
        setContent {
            App(
                initialPcState = PcState.WOL_READY,
                wakeSender = sender,
                wakePollMillis = 20,
                // 预算故意留长：这条用例断言的是"刚点下去的那一刻停在 WAKING"，
                // 预算太小会让流程在断言前就跑到超时回落。
                wakeBudgetMillis = 60_000,
                wakeAgentResponds = { false },
            )
        }

        onNodeWithTag("powerrail:primary").performClick()

        assertEquals(1, sender.sends)
        // WakeRequested → WAKING。`powerrail:state` 渲染的是规格 A2 的短形态（`Waking PC…`），
        // 长形态 `Waiting for Agent` 在 `powerrail:status` 那一行。
        onNodeWithTag("powerrail:state", useUnmergedTree = true).assertTextEquals("Waking PC…")
        onNodeWithTag("powerrail:status", useUnmergedTree = true).assertTextEquals("Waiting for Agent")
    }

    @Test
    fun `a wake that nobody answers explains itself on the rail`() = runComposeUiTest {
        setContent {
            App(
                initialPcState = PcState.WOL_READY,
                wakeSender = FakeSender(),
                wakePollMillis = 20,
                wakeBudgetMillis = 100,
                wakeAgentResponds = { false },
            )
        }

        onNodeWithTag("powerrail:primary").performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("powerrail:wake-note").fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithTag("powerrail:wake-note", useUnmergedTree = true)
            .assertTextEquals("Sent 3 wake packets — no answer from the Agent within <1 s")
    }

    @Test
    fun `a refused test connection puts the rail into wake ready and the ring can then send`() =
        runComposeUiTest {
            val sender = FakeSender()
            val refusingProbe = object : TcpProbe {
                override suspend fun probe(host: String, port: Int, timeoutMillis: Long) = TcpProbeResult.Refused
            }
            val navigator = AppNavigator().apply { goTo(Workspace.Settings) }
            setContent {
                App(
                    navigator = navigator,
                    probe = refusingProbe,
                    wakeSender = sender,
                    wakePollMillis = 20,
                    wakeBudgetMillis = 60_000,
                    wakeAgentResponds = { false },
                )
            }

            // PC 关机、Agent 不可达、但这台设备有 MAC → spec §4 的 WOL_READY
            onNodeWithTag("device:test").performClick()
            waitUntilExactlyOneExists(
                hasTestTag("powerrail:state") and hasText("Ready to wake"),
                timeoutMillis = 5_000,
            )

            // WOL_READY 下主按钮才可点，点了就真的发包（本用例注入的假发送器）
            onNodeWithTag("powerrail:primary").performClick()
            assertEquals(1, sender.sends)
        }

    /** 用户实测反馈的核心缺陷：不点 Test Connection 就永远是 Mock 的 `PC Online`，电源环按不动。 */
    @Test
    fun `a real probe decides the rail state at startup without any tap`() = runComposeUiTest {
        val refusingProbe = object : TcpProbe {
            override suspend fun probe(host: String, port: Int, timeoutMillis: Long) = TcpProbeResult.Refused
        }
        setContent {
            App(probe = refusingProbe, wakeSender = FakeSender(), wakeAgentResponds = { false })
        }

        // 启动时探测一次：Agent 不可达 + 设备有 MAC → WOL_READY，不需要用户先去 Settings 点一下
        waitUntilExactlyOneExists(
            hasTestTag("powerrail:state") and hasText("Ready to wake"),
            timeoutMillis = 5_000,
        )
    }

    @Test
    fun `with no saved device the rail asks for setup instead of pretending to be online`() =
        runComposeUiTest {
            val storage = JsonSettingsStorage(InMemoryKeyValueStore())
            runBlocking { storage.markSeeded() }   // 已播种过但列表为空 = 用户把设备都删了

            setContent { App(storage = storage) }

            waitUntilExactlyOneExists(
                hasTestTag("powerrail:state") and hasText("Not configured"),
                timeoutMillis = 5_000,
            )
            onNodeWithTag("powerrail:primary-label", useUnmergedTree = true).assertTextEquals("Setup PC")
        }

    @Test
    fun `an unpaired energy action explains that pairing comes first`() = runComposeUiTest {
        setContent { App() }

        onNodeWithTag("powerrail:sleep").performClick()
        onNodeWithTag("dialog:power-confirm").performClick()

        onNodeWithTag("powerrail:wake-note", useUnmergedTree = true)
            .assertTextEquals("Pair the phone in Device Setup first (Agent section)")
    }
}
