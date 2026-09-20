package com.xukunz.wakeupmywall.core.connectivity

import com.xukunz.wakeupmywall.core.network.AgentApi
import com.xukunz.wakeupmywall.core.network.ApiFailure
import com.xukunz.wakeupmywall.core.network.ApiResult
import com.xukunz.wakeupmywall.domain.model.PcDevice

enum class ConnectionFailure {
    MISSING_MAC, PORT_REFUSED, PORT_TIMEOUT, HOST_UNRESOLVED, PROBE_UNAVAILABLE,
    NOT_AN_AGENT, UNAUTHORIZED, AGENT_ERROR, NETWORK,
}

sealed interface ConnectionReport {
    /** 有 MAC、没配 Agent 主机：WOL 可用，但没有可探测的 Agent。 */
    data class WolOnly(val mac: String) : ConnectionReport
    data class AgentOnline(val hostname: String, val version: String) : ConnectionReport
    data class Failed(val reason: ConnectionFailure, val message: String) : ConnectionReport
}

/**
 * 把"TCP 通不通"与"Agent 答不答"合成一句可解释的结论。
 * 顺序有意义：先排除输入缺失，再报网络层具体原因，最后才是 HTTP 语义。
 */
class ConnectivityTester(
    private val probe: TcpProbe,
    /**
     * 取一个 AgentApi。**必须是工厂而不是实例**：`createAgentHttpClient()` 需要一个平台 HTTP
     * 引擎，桌面 target（只用于 UI 测试与设计预览）没有装引擎；提前构造会让每一屏都起不来。
     * 只有 TCP 真的连上了才会调用它。
     */
    private val api: () -> AgentApi,
    private val token: String? = null,
) {
    suspend fun test(device: PcDevice): ConnectionReport {
        // MacAddress 只公开 normalized；value 是私有的，这里用归一化后的字符串。
        val mac = device.macAddress?.normalized
            ?: return failed(ConnectionFailure.MISSING_MAC, "Add a MAC address first — wake-on-LAN needs it")

        val host = device.agentHost ?: device.ipAddress
            ?: return ConnectionReport.WolOnly(mac)

        return when (val tcp = probe.probe(host, device.agentPort)) {
            TcpProbeResult.Reachable -> checkAgent(host, device.agentPort)
            TcpProbeResult.Refused -> failed(
                ConnectionFailure.PORT_REFUSED,
                "Nothing is listening on $host:${device.agentPort} — the PC may be asleep, or the Agent is not running",
            )
            TcpProbeResult.TimedOut -> failed(
                ConnectionFailure.PORT_TIMEOUT,
                "$host:${device.agentPort} did not answer within 2 s — wrong IP, or the firewall is dropping it",
            )
            is TcpProbeResult.Unresolved -> failed(
                ConnectionFailure.HOST_UNRESOLVED,
                "Cannot resolve \"${tcp.host}\" — check the Agent host name",
            )
            is TcpProbeResult.Failed -> failed(ConnectionFailure.PROBE_UNAVAILABLE, tcp.reason)
        }
    }

    private suspend fun checkAgent(host: String, port: Int): ConnectionReport =
        when (val result = api().status("http://$host:$port", token)) {
            is ApiResult.Success -> ConnectionReport.AgentOnline(result.value.hostname, result.value.agentVersion)
            is ApiResult.Failure -> when (result.reason) {
                ApiFailure.NOT_FOUND -> failed(
                    ConnectionFailure.NOT_AN_AGENT,
                    "$host:$port answers, but /api/v1/status returned 404 — this is not the WakeUpMyWall Agent",
                )
                ApiFailure.UNAUTHORIZED -> failed(
                    ConnectionFailure.UNAUTHORIZED,
                    "The Agent rejected the token — pair the device again after Phase 4 lands",
                )
                ApiFailure.SERVER -> failed(
                    ConnectionFailure.AGENT_ERROR,
                    "The Agent answered with a server error (${result.message})",
                )
                ApiFailure.DECODING -> failed(
                    ConnectionFailure.AGENT_ERROR,
                    "The Agent answered, but the payload was not a status document",
                )
                ApiFailure.TIMEOUT, ApiFailure.NETWORK -> failed(
                    ConnectionFailure.NETWORK,
                    "TCP connected but the status call failed (${result.message})",
                )
            }
        }

    private fun failed(reason: ConnectionFailure, message: String) = ConnectionReport.Failed(reason, message)
}
