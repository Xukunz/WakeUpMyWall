package com.xukunz.wakeupmywall.domain.usecase

import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice

data class DeviceSetupInput(
    val name: String,
    val mac: String,
    val ip: String,
    val broadcast: String,
    val wolPort: String,
    val agentPort: String,
    val agentHost: String,
)

data class DeviceSetupResult(
    val isValid: Boolean,
    val errors: Map<String, String>,
    val device: PcDevice?,
)

/**
 * 设备表单校验（纯逻辑，可单测）。错误文案在这里写死成常量，UI 只负责显示，
 * 避免同一句提示在多个 Composable 里各写一遍。
 */
object DeviceSetupValidator {

    const val NAME_REQUIRED = "Name is required"
    const val MAC_INVALID = "Enter a valid MAC address"
    const val IP_INVALID = "Enter a valid IPv4 address"
    const val PORT_INVALID = "Port must be between 1 and 65535"
    const val HOST_INVALID = "Enter a valid host"

    fun validate(input: DeviceSetupInput): DeviceSetupResult {
        val errors = buildMap {
            if (input.name.isBlank()) put("name", NAME_REQUIRED)
            if (MacAddress.parse(input.mac) == null) put("mac", MAC_INVALID)
            if (!isValidIpv4(input.broadcast)) put("broadcast", IP_INVALID)
            if (input.ip.isNotBlank() && !isValidIpv4(input.ip)) put("ip", IP_INVALID)
            if (!isValidPort(input.wolPort)) put("wolPort", PORT_INVALID)
            if (!isValidPort(input.agentPort)) put("agentPort", PORT_INVALID)
            if (input.agentHost.isNotBlank() && !isValidHost(input.agentHost)) put("agentHost", HOST_INVALID)
        }

        val device = if (errors.isEmpty()) {
            PcDevice(
                id = input.name.trim().lowercase().replace(' ', '-'),
                name = input.name.trim(),
                macAddress = MacAddress.parse(input.mac),
                ipAddress = input.ip.ifBlank { null },
                broadcastAddress = input.broadcast.trim(),
                wolPort = input.wolPort.toInt(),
                agentHost = input.agentHost.ifBlank { null },
                agentPort = input.agentPort.toInt(),
            )
        } else {
            null
        }

        return DeviceSetupResult(errors.isEmpty(), errors, device)
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.trim().split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            val number = part.toIntOrNull()
            number != null && number in 0..255 && part.none { it == '+' } &&
                (part.length == 1 || !part.startsWith("0"))
        }
    }

    private fun isValidPort(value: String): Boolean = value.toIntOrNull()?.let { it in 1..65535 } ?: false

    /** 主机名或 IPv4：不能含空白与路径分隔符。 */
    private fun isValidHost(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.any { it.isWhitespace() || it == '/' }) return false
        return isValidIpv4(trimmed) || trimmed.matches(Regex("[A-Za-z0-9.-]+"))
    }
}
