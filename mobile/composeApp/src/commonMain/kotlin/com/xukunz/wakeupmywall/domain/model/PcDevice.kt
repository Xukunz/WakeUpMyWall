package com.xukunz.wakeupmywall.domain.model

import com.xukunz.wakeupmywall.core.storage.MacAddressSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PcDevice(
    val id: String,
    val name: String,
    @Serializable(with = MacAddressSerializer::class)
    val macAddress: MacAddress? = null,
    val ipAddress: String? = null,
    val broadcastAddress: String = "192.168.1.255",
    val wolPort: Int = 9,
    val agentHost: String? = null,
    val agentPort: Int = 9876,
    val isDefault: Boolean = false,
    val lastSeen: Instant? = null,
)

/**
 * 能不能靠魔包唤醒：spec §4 用这条区分 `WOL_READY`（Agent 不可达但能唤醒）与
 * `OFFLINE`（连唤醒条件都不具备）。广播地址空着同样发不出包，所以一起算进来。
 */
val PcDevice.isWakeable: Boolean
    get() = macAddress != null && broadcastAddress.isNotBlank()
