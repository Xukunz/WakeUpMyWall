package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.PcEvent
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
