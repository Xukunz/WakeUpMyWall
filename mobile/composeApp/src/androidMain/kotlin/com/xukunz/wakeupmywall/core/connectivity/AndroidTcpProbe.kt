package com.xukunz.wakeupmywall.core.connectivity

import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 真实 TCP 探测。异常→结果的映射只看**类型**：用 error message 里的字符串猜"连接被拒"
 * 会在非英文环境或不同 JVM 实现上翻车。
 */
class AndroidTcpProbe : TcpProbe {
    override suspend fun probe(host: String, port: Int, timeoutMillis: Long): TcpProbeResult =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMillis.toInt())
                }
                TcpProbeResult.Reachable
            } catch (e: IOException) {
                classifyTcpError(e)
            }
        }
}

internal fun classifyTcpError(error: IOException): TcpProbeResult = when (error) {
    is UnknownHostException -> TcpProbeResult.Unresolved(error.message ?: "unknown host")
    is SocketTimeoutException -> TcpProbeResult.TimedOut
    is ConnectException, is NoRouteToHostException -> TcpProbeResult.Refused
    else -> TcpProbeResult.Failed(error.message ?: error::class.simpleName ?: "connect failed")
}
