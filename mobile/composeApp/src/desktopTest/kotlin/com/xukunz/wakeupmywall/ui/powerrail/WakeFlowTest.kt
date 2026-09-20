package com.xukunz.wakeupmywall.ui.powerrail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.app.App
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
}
