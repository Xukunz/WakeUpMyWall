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
