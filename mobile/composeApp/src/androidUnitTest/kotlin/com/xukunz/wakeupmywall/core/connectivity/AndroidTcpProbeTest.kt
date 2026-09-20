package com.xukunz.wakeupmywall.core.connectivity

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidTcpProbeTest {

    @Test
    fun `a listening port is reachable`() = runTest {
        val server = ServerSocket(0)
        try {
            assertEquals(
                TcpProbeResult.Reachable,
                AndroidTcpProbe().probe("127.0.0.1", server.localPort, timeoutMillis = 2_000),
            )
        } finally {
            server.close()
        }
    }

    @Test
    fun `a closed port is refused`() = runTest {
        val closed = ServerSocket(0).also { it.close() }.localPort

        assertEquals(
            TcpProbeResult.Refused,
            AndroidTcpProbe().probe("127.0.0.1", closed, timeoutMillis = 2_000),
        )
    }

    @Test
    fun `exception mapping is type based, not message based`() {
        assertEquals(TcpProbeResult.Refused, classifyTcpError(ConnectException("Connection refused")))
        assertEquals(TcpProbeResult.TimedOut, classifyTcpError(SocketTimeoutException("connect timed out")))
        assertEquals(TcpProbeResult.Refused, classifyTcpError(NoRouteToHostException("No route to host")))
        assertEquals(
            TcpProbeResult.Unresolved("nope.local"),
            classifyTcpError(UnknownHostException("nope.local")),
        )
        assertTrue(classifyTcpError(IOException("boom")) is TcpProbeResult.Failed)
    }
}
