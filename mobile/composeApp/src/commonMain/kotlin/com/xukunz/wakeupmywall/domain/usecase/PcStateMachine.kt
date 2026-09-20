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

        // Agent 答得上就说明 PC 是开着的 —— 包括我们原以为它在关机状态（WOL_READY / OFFLINE）的时候：
        // Phase 4B 的存在性轮询正是靠这一条把"刚被唤醒/被手动开机"的机器翻成 ONLINE。
        // 唯一例外是 UNCONFIGURED：一台设备都没有时不许假装在线。
        PcEvent.AgentResponded -> if (current == PcState.UNCONFIGURED) current else PcState.ONLINE

        PcEvent.AgentLost -> when (current) {
            PcState.UNCONFIGURED -> PcState.UNCONFIGURED
            PcState.RESTARTING -> PcState.RESTARTING
            else -> if (device?.isWakeable == true) PcState.WOL_READY else PcState.OFFLINE
        }

        // "没有设备"与"有设备但连不上"是两回事：前者靠 DeviceRemoved 进、靠 DeviceConfigured 出。
        PcEvent.DeviceConfigured -> if (device?.isWakeable == true) PcState.WOL_READY else PcState.OFFLINE

        PcEvent.DeviceRemoved -> PcState.UNCONFIGURED

        // 主机答了但 Agent 不在（端口通、HTTP 语义不对/未授权）：只有"本来在线"才有意义，
        // 其它状态保持不动，免得把 WAKING / RESTARTING 这类瞬态打断。
        PcEvent.AgentUnavailable -> when (current) {
            PcState.ONLINE, PcState.AGENT_UNAVAILABLE -> PcState.AGENT_UNAVAILABLE
            else -> current
        }

        PcEvent.CommandFailed -> PcState.ERROR
    }
}
