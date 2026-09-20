package com.xukunz.wakeupmywall.core.wol

import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingSender(private val result: WakeSendResult) : WakeOnLanSender {
    val calls = mutableListOf<Triple<Int, String, Int>>()
    override suspend fun send(packet: ByteArray, target: String, port: Int): WakeSendResult {
        calls += Triple(packet.size, target, port)
        return result
    }
}

class WakeSequenceTest {

    private val device = PcDevice(
        id = "my-pc",
        name = "My PC",
        macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
        broadcastAddress = "192.168.1.255",
        wolPort = 9,
        isDefault = true,
    )

    /** 按顺序回答 [answers]，用完之后一律 false。 */
    private fun sequence(
        sender: WakeOnLanSender,
        answers: List<Boolean>,
        events: MutableList<PcEvent> = mutableListOf(),
    ): WakeSequence {
        var poll = 0
        return WakeSequence(
            sender = sender,
            agentResponded = { answers.getOrElse(poll++) { false } },
            pollIntervalMillis = 2_000,
            waitBudgetMillis = 60_000,
            onEvent = { events += it },
        )
    }

    @Test
    fun `a device without a mac never sends`() = runTest {
        val sender = RecordingSender(WakeSendResult.Sent(3))

        val outcome = sequence(sender, emptyList()).wake(device.copy(macAddress = null))

        assertEquals(WakeFailure.MISSING_MAC, (outcome as WakeOutcome.Failed).reason)
        assertTrue(sender.calls.isEmpty())
    }

    @Test
    fun `a device without a broadcast address never sends`() = runTest {
        val sender = RecordingSender(WakeSendResult.Sent(3))

        val outcome = sequence(sender, emptyList()).wake(device.copy(broadcastAddress = "  "))

        assertEquals(WakeFailure.MISSING_BROADCAST, (outcome as WakeOutcome.Failed).reason)
        assertTrue(sender.calls.isEmpty())
    }

    @Test
    fun `a send failure is reported verbatim and never enters waking`() = runTest {
        val sender = RecordingSender(WakeSendResult.Failed("Socket error: Network is unreachable"))
        val events = mutableListOf<PcEvent>()

        val outcome = sequence(sender, emptyList(), events).wake(device)

        assertEquals(WakeFailure.SEND_FAILED, (outcome as WakeOutcome.Failed).reason)
        assertTrue(outcome.message.contains("Network is unreachable"))
        assertTrue(events.isEmpty(), "没发出去就不该进 WAKING")
    }

    @Test
    fun `the packet goes to the broadcast address and the configured port`() = runTest {
        val sender = RecordingSender(WakeSendResult.Sent(3))

        sequence(sender, listOf(true)).wake(device)

        assertEquals(Triple(102, "192.168.1.255", 9), sender.calls.single())
    }

    @Test
    fun `the first agent answer ends the wait with online`() = runTest {
        val events = mutableListOf<PcEvent>()

        val outcome = sequence(RecordingSender(WakeSendResult.Sent(3)), listOf(false, true), events).wake(device)

        assertEquals(WakeOutcome.AgentOnline(packets = 3, waitedMillis = 4_000), outcome)
        assertEquals(listOf(PcEvent.WakeRequested, PcEvent.AgentResponded), events)
    }

    @Test
    fun `no answer inside the budget ends back at wake ready`() = runTest {
        val events = mutableListOf<PcEvent>()

        val outcome = sequence(RecordingSender(WakeSendResult.Sent(3)), emptyList(), events).wake(device)

        assertEquals(WakeOutcome.NoAnswer(packets = 3, waitedMillis = 60_000, lastError = null), outcome)
        assertEquals(listOf(PcEvent.WakeRequested, PcEvent.WakeTimedOut), events)
    }
}
