package com.xukunz.wakeupmywall.ui.powerrail

import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.domain.model.capabilities

/**
 * Power Rail 的展示模型。可用性字段一律来自 `PcState.capabilities(device)`，
 * UI 里不允许再写 `if (state == ONLINE)` 这类散落判断（Phase 1 全局约束）。
 */
data class PowerRailModel(
    val pcName: String,
    /** 设备 id：决定摘要卡 / 身份卡用哪张封面素材（取不到回落默认封面）。 */
    val deviceId: String?,
    val subtitle: String,
    val stateLabel: String,
    val primaryLabel: String,
    val primaryCaption: String,
    val primaryEnabled: Boolean,
    val canSleep: Boolean,
    val canShutdown: Boolean,
    val canRestart: Boolean,
    val connectionLabel: String,
    val statusLine: String,
    /** 唤醒失败/超时的解释行；null = 不渲染（成功与未尝试过都不该多一行）。 */
    val wakeNote: String? = null,
)

/** Agent 可达（= 次级电源动作可用）。状态点用它决定是否用"在线色"。 */
val PowerRailModel.agentReachable: Boolean get() = canSleep

/**
 * 通道条 / 底栏的第二行（能力描述 [PowerRailModel.statusLine]）只在它与通道文案
 * [PowerRailModel.connectionLabel] **不同**时才渲染。
 *
 * 规格 A5 的通道条是"图标 + 通道文案 + 箭头"一行；加了第二行以后，ONLINE 的
 * `statusText`（`Agent connected over LAN`）与 WOL_READY 的（`Wake-on-LAN Ready`）恰好与
 * 通道文案逐字相同，同一张卡里同一句话就出现两次。其余状态（OFFLINE 的
 * `Wake-on-LAN requires MAC`、WAKING 的 `Waiting for Agent`）两行内容不同，是有用的信息，照旧保留。
 */
val PowerRailModel.showsStatusLine: Boolean get() = statusLine != connectionLabel

fun powerRailModel(state: PcState, device: PcDevice?, wakeNote: String? = null): PowerRailModel {
    val capabilities = state.capabilities(device)
    return PowerRailModel(
        // 一台设备都没配时不要借 Mock 的名字：那会让人以为"有台叫 My PC 的机器"。
        pcName = device?.name ?: "No PC yet",
        deviceId = device?.id,
        subtitle = "POWER CONTROL",
        stateLabel = state.shortLabel(),
        primaryLabel = state.primaryLabel(capabilities.primaryLabel),
        primaryCaption = state.primaryCaption(),
        primaryEnabled = capabilities.primaryEnabled,
        canSleep = capabilities.canSleep,
        canShutdown = capabilities.canShutdown,
        canRestart = capabilities.canRestart,
        // 唯一决定连接条文案的地方：在线走 Agent，其余一律是 WOL 通道（规范 §3 术语规则）。
        connectionLabel = if (state == PcState.ONLINE) "Agent connected over LAN" else "Wake-on-LAN Ready",
        statusLine = capabilities.statusText,
        wakeNote = wakeNote,
    )
}

/** 权威规格 A2：状态行取能力描述的短形态。 */
private fun PcState.shortLabel(): String = when (this) {
    PcState.UNCONFIGURED -> "Not configured"
    PcState.OFFLINE -> "Offline"
    PcState.WOL_READY -> "Ready to wake"
    PcState.WAKING -> "Waking PC…"
    PcState.ONLINE -> "Online"
    PcState.AGENT_UNAVAILABLE -> "Agent unavailable"
    PcState.SLEEPING -> "Sleeping…"
    PcState.RESTARTING -> "Restarting…"
    PcState.SHUTTING_DOWN -> "Shutting down…"
    PcState.ERROR -> "Needs attention"
}

/**
 * 权威规格 A3 把 WOL_READY 的主标签钉为 `Power On`；其余状态沿用 Phase 0 的能力描述，
 * 因为"Waking… / Setup PC / Retry"本来就是能力层给出的用户动作。
 */
private fun PcState.primaryLabel(capabilityLabel: String): String = when (this) {
    PcState.WOL_READY -> "Power On"
    else -> capabilityLabel
}

private fun PcState.primaryCaption(): String = when (this) {
    PcState.UNCONFIGURED -> "ADD YOUR PC"
    PcState.OFFLINE, PcState.WOL_READY -> "WAKE YOUR PC"
    PcState.WAKING, PcState.SLEEPING, PcState.RESTARTING, PcState.SHUTTING_DOWN -> "WAITING FOR AGENT"
    PcState.ONLINE -> "AGENT CONNECTED"
    PcState.AGENT_UNAVAILABLE -> "CHECK THE AGENT"
    PcState.ERROR -> "TRY AGAIN"
}
