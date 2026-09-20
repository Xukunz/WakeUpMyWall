package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.PcEvent
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.domain.usecase.PcStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

class PcStateMachineTest {

    @Test
    fun `wake command moves wol ready to waking`() {
        assertEquals(PcState.WAKING, PcStateMachine.reduce(PcState.WOL_READY, PcEvent.WakeRequested))
    }

    @Test
    fun `agent heartbeat moves waking to online`() {
        assertEquals(PcState.ONLINE, PcStateMachine.reduce(PcState.WAKING, PcEvent.AgentResponded))
    }

    @Test
    fun `wake timeout falls back to wol ready`() {
        assertEquals(PcState.WOL_READY, PcStateMachine.reduce(PcState.WAKING, PcEvent.WakeTimedOut))
    }

    @Test
    fun `shutdown command is ignored when offline`() {
        assertEquals(PcState.OFFLINE, PcStateMachine.reduce(PcState.OFFLINE, PcEvent.ShutdownRequested))
    }

    @Test
    fun `shutdown from online goes to shutting down then offline`() {
        val shuttingDown = PcStateMachine.reduce(PcState.ONLINE, PcEvent.ShutdownRequested)
        assertEquals(PcState.SHUTTING_DOWN, shuttingDown)
        assertEquals(PcState.OFFLINE, PcStateMachine.reduce(shuttingDown, PcEvent.AgentLost))
    }

    @Test
    fun `unconfigured device stays unconfigured when agent is lost`() {
        assertEquals(PcState.UNCONFIGURED, PcStateMachine.reduce(PcState.UNCONFIGURED, PcEvent.AgentLost))
    }

    @Test
    fun `losing the agent lands on wake ready when the device can be woken`() {
        val device = PcDevice(
            id = "my-pc",
            name = "My PC",
            macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
            broadcastAddress = "192.168.1.255",
            isDefault = true,
        )

        assertEquals(PcState.WOL_READY, PcStateMachine.reduce(PcState.ONLINE, PcEvent.AgentLost, device))
    }

    @Test
    fun `losing the agent lands on offline when the device has no mac`() {
        val device = PcDevice(id = "new-pc", name = "New PC", broadcastAddress = "192.168.1.255", isDefault = true)

        assertEquals(PcState.OFFLINE, PcStateMachine.reduce(PcState.ONLINE, PcEvent.AgentLost, device))
    }

    @Test
    fun `an unconfigured app leaves unconfigured once a device exists`() {
        val wakeable = PcDevice(
            id = "my-pc",
            name = "My PC",
            macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
            broadcastAddress = "192.168.1.255",
            isDefault = true,
        )
        val withoutMac = PcDevice(id = "new-pc", name = "New PC", isDefault = true)

        // AgentLost 对 UNCONFIGURED 是自锁的：必须先来 DeviceConfigured 才出得来
        assertEquals(
            PcState.UNCONFIGURED,
            PcStateMachine.reduce(PcState.UNCONFIGURED, PcEvent.AgentLost, wakeable),
        )
        assertEquals(
            PcState.WOL_READY,
            PcStateMachine.reduce(PcState.UNCONFIGURED, PcEvent.DeviceConfigured, wakeable),
        )
        assertEquals(
            PcState.OFFLINE,
            PcStateMachine.reduce(PcState.UNCONFIGURED, PcEvent.DeviceConfigured, withoutMac),
        )
    }

    @Test
    fun `removing every device goes back to unconfigured`() {
        assertEquals(
            PcState.UNCONFIGURED,
            PcStateMachine.reduce(PcState.WOL_READY, PcEvent.DeviceRemoved),
        )
        assertEquals(
            PcState.UNCONFIGURED,
            PcStateMachine.reduce(PcState.ONLINE, PcEvent.DeviceRemoved),
        )
    }

    @Test
    fun `restart keeps restarting until agent returns`() {
        val restarting = PcStateMachine.reduce(PcState.ONLINE, PcEvent.RestartRequested)
        assertEquals(PcState.RESTARTING, restarting)
        assertEquals(PcState.ONLINE, PcStateMachine.reduce(restarting, PcEvent.AgentResponded))
    }

    @Test
    fun `error state recovers to online when agent responds`() {
        assertEquals(PcState.ONLINE, PcStateMachine.reduce(PcState.ERROR, PcEvent.AgentResponded))
    }
}
