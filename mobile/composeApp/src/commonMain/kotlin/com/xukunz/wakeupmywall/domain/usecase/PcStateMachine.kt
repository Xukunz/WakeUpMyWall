package com.xukunz.wakeupmywall.domain.usecase

import com.xukunz.wakeupmywall.domain.model.PcEvent
import com.xukunz.wakeupmywall.domain.model.PcState

object PcStateMachine {

    fun reduce(current: PcState, event: PcEvent): PcState = when (event) {
        PcEvent.WakeRequested -> when (current) {
            PcState.WOL_READY, PcState.OFFLINE, PcState.ERROR -> PcState.WAKING
            else -> current
        }

        PcEvent.WakeTimedOut -> if (current == PcState.WAKING) PcState.WOL_READY else current

        PcEvent.SleepRequested -> if (current == PcState.ONLINE) PcState.SLEEPING else current

        PcEvent.ShutdownRequested -> if (current == PcState.ONLINE) PcState.SHUTTING_DOWN else current

        PcEvent.RestartRequested -> if (current == PcState.ONLINE) PcState.RESTARTING else current

        PcEvent.AgentResponded -> when (current) {
            PcState.UNCONFIGURED, PcState.OFFLINE, PcState.WOL_READY -> current
            else -> PcState.ONLINE
        }

        PcEvent.AgentLost -> when (current) {
            PcState.UNCONFIGURED -> PcState.UNCONFIGURED
            PcState.RESTARTING -> PcState.RESTARTING
            else -> PcState.OFFLINE
        }

        PcEvent.CommandFailed -> PcState.ERROR
    }
}
