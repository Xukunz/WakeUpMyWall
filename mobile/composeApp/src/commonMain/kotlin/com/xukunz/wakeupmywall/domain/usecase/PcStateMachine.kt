package com.xukunz.wakeupmywall.domain.usecase

import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcEvent
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.domain.model.isWakeable

object PcStateMachine {

    /**
     * [device] 只在 `AgentLost` 这一支用得上：spec §4 把"Agent 不可达"分成两种——
     * 有 MAC/广播信息的算 `WOL_READY`（还能靠魔包唤醒），缺这些信息的才是 `OFFLINE`。
     * 不传 device 时保守地退回 `OFFLINE`（Phase 0/1 的既有行为与测试不变）。
     */
    fun reduce(current: PcState, event: PcEvent, device: PcDevice? = null): PcState = when (event) {
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
            else -> if (device?.isWakeable == true) PcState.WOL_READY else PcState.OFFLINE
        }

        PcEvent.CommandFailed -> PcState.ERROR
    }
}
