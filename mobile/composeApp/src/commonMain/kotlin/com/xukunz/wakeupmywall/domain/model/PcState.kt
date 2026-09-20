package com.xukunz.wakeupmywall.domain.model

enum class PcState {
    UNCONFIGURED, OFFLINE, WOL_READY, WAKING, ONLINE,
    AGENT_UNAVAILABLE, SLEEPING, RESTARTING, SHUTTING_DOWN, ERROR,
}

sealed interface PcEvent {
    data object WakeRequested : PcEvent
    data object WakeTimedOut : PcEvent
    data object SleepRequested : PcEvent
    data object ShutdownRequested : PcEvent
    data object RestartRequested : PcEvent
    data object AgentResponded : PcEvent
    data object AgentLost : PcEvent
    /** 主机在线（TCP 通）但 Agent 不回应：spec §4 的 `AGENT_UNAVAILABLE`。 */
    data object AgentUnavailable : PcEvent
    /** 从"没有设备"变成"有设备"：按这台设备能不能唤醒落到 `WOL_READY` / `OFFLINE`。 */
    data object DeviceConfigured : PcEvent
    /** 设备列表被清空：回到 `UNCONFIGURED`（`AgentLost` 对 UNCONFIGURED 是自锁的，出不来）。 */
    data object DeviceRemoved : PcEvent
    data object CommandFailed : PcEvent
}

data class PcCapabilities(
    val primaryLabel: String,
    val primaryEnabled: Boolean,
    val canSleep: Boolean,
    val canShutdown: Boolean,
    val canRestart: Boolean,
    val statusText: String,
)

fun PcState.capabilities(device: PcDevice?): PcCapabilities = when (this) {
    PcState.UNCONFIGURED -> PcCapabilities("Setup PC", true, false, false, false, "Add your PC to begin")
    PcState.OFFLINE -> PcCapabilities("Wake PC", false, false, false, false, "Wake-on-LAN requires MAC")
    PcState.WOL_READY -> PcCapabilities("Wake PC", device?.macAddress != null, false, false, false, "Wake-on-LAN Ready")
    PcState.WAKING -> PcCapabilities("Waking…", false, false, false, false, "Waiting for Agent")
    PcState.ONLINE -> PcCapabilities("PC Online", false, true, true, true, "Agent connected over LAN")
    PcState.AGENT_UNAVAILABLE -> PcCapabilities("Agent unavailable", false, false, false, false, "Host reachable, Agent not responding")
    PcState.SLEEPING -> PcCapabilities("Sleeping…", false, false, false, false, "Waiting for Agent")
    PcState.RESTARTING -> PcCapabilities("Restarting…", false, false, false, false, "Waiting for Agent")
    PcState.SHUTTING_DOWN -> PcCapabilities("Shutting down…", false, false, false, false, "Waiting for Agent")
    PcState.ERROR -> PcCapabilities("Retry", true, false, false, false, "Last command failed")
}
