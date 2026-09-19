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
