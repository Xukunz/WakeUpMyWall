package com.xukunz.wakeupmywall.core.connectivity

sealed interface TcpProbeResult {
    data object Reachable : TcpProbeResult
    data object Refused : TcpProbeResult
    data object TimedOut : TcpProbeResult
    data class Unresolved(val host: String) : TcpProbeResult
    data class Failed(val reason: String) : TcpProbeResult
}

interface TcpProbe {
    suspend fun probe(host: String, port: Int, timeoutMillis: Long = 2_000): TcpProbeResult
}

/** 桌面预览与纯 UI 测试的默认实现：不猜结果，直接说这条平台路径不可用。 */
object UnsupportedTcpProbe : TcpProbe {
    override suspend fun probe(host: String, port: Int, timeoutMillis: Long) =
        TcpProbeResult.Failed("TCP probe is not available on this platform")
}
