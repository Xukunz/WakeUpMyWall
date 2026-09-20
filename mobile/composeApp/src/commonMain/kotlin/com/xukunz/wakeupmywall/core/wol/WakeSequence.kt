package com.xukunz.wakeupmywall.core.wol

import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcEvent
import kotlinx.coroutines.delay

enum class WakeFailure { MISSING_MAC, MISSING_BROADCAST, SEND_FAILED }

sealed interface WakeOutcome {
    data class AgentOnline(val packets: Int, val waitedMillis: Long) : WakeOutcome
    data class NoAnswer(val packets: Int, val waitedMillis: Long, val lastError: String?) : WakeOutcome
    data class Failed(val reason: WakeFailure, val message: String) : WakeOutcome
}

/**
 * 唤醒编排（spec §5）：校验 → 发魔包 → 进 `WAKING` → 每 2 秒问一次 Agent 是否已经起来
 * → 应答即 `ONLINE`，预算耗尽回落 `WOL_READY`。
 *
 * [sender] 与 [agentResponded] 都注入：这样 60 秒等待能在 `runTest` 的虚拟时间里秒级验证。
 * 状态流转只通过 [onEvent] 发 `PcEvent`，下一个状态始终由 `PcStateMachine` 决定。
 */
class WakeSequence(
    private val sender: WakeOnLanSender,
    private val agentResponded: suspend (PcDevice) -> Boolean,
    private val pollIntervalMillis: Long = 2_000,
    private val waitBudgetMillis: Long = 60_000,
    private val onEvent: (PcEvent) -> Unit = {},
) {
    suspend fun wake(device: PcDevice): WakeOutcome {
        val mac = device.macAddress
            ?: return WakeOutcome.Failed(WakeFailure.MISSING_MAC, "Wake-on-LAN needs the PC's MAC address")
        if (device.broadcastAddress.isBlank()) {
            return WakeOutcome.Failed(WakeFailure.MISSING_BROADCAST, "Wake-on-LAN needs a broadcast address")
        }

        return when (val result = sender.send(MagicPacket.encode(mac), device.broadcastAddress, device.wolPort)) {
            is WakeSendResult.Failed -> WakeOutcome.Failed(WakeFailure.SEND_FAILED, result.reason)
            is WakeSendResult.Sent -> {
                onEvent(PcEvent.WakeRequested)
                var waited = 0L
                while (waited < waitBudgetMillis) {
                    delay(pollIntervalMillis)
                    waited += pollIntervalMillis
                    if (agentResponded(device)) {
                        onEvent(PcEvent.AgentResponded)
                        return WakeOutcome.AgentOnline(result.packets, waited)
                    }
                }
                onEvent(PcEvent.WakeTimedOut)
                WakeOutcome.NoAnswer(result.packets, waited, lastError = null)
            }
        }
    }
}
