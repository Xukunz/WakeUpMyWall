package com.xukunz.wakeupmywall.core.connectivity

import com.xukunz.wakeupmywall.core.network.AgentApi
import com.xukunz.wakeupmywall.core.network.createAgentHttpClient
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeProbe(private val result: TcpProbeResult) : TcpProbe {
    override suspend fun probe(host: String, port: Int, timeoutMillis: Long) = result
}

class ConnectivityTesterTest {

    private val device = PcDevice(
        id = "my-pc",
        name = "My PC",
        macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
        ipAddress = "192.168.1.10",
        agentHost = "192.168.1.10",
    )

    private fun tester(probe: TcpProbeResult, status: HttpStatusCode = HttpStatusCode.OK) = ConnectivityTester(
        probe = FakeProbe(probe),
        api = AgentApi(
            createAgentHttpClient(
                MockEngine {
                    respond(
                        content = """{"hostname":"Desktop-Alpha","agentVersion":"0.1.0","uptimeSeconds":42}""",
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ),
        ),
    )

    @Test
    fun `without a mac there is nothing to wake`() = runTest {
        val report = tester(TcpProbeResult.Reachable).test(device.copy(macAddress = null))

        assertEquals(ConnectionFailure.MISSING_MAC, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a mac without a host reports wol only instead of a fake failure`() = runTest {
        val report = tester(TcpProbeResult.Reachable).test(device.copy(ipAddress = null, agentHost = null))

        assertTrue(report is ConnectionReport.WolOnly)
    }

    @Test
    fun `a refused port names the port that did not answer`() = runTest {
        val report = tester(TcpProbeResult.Refused).test(device)

        val failed = report as ConnectionReport.Failed
        assertEquals(ConnectionFailure.PORT_REFUSED, failed.reason)
        assertTrue(failed.message.contains("9876"))
    }

    @Test
    fun `a timeout is reported as a timeout`() = runTest {
        val report = tester(TcpProbeResult.TimedOut).test(device)

        assertEquals(ConnectionFailure.PORT_TIMEOUT, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `an unresolvable host is reported as such`() = runTest {
        val report = tester(TcpProbeResult.Unresolved("nope.local")).test(device.copy(agentHost = "nope.local"))

        assertEquals(ConnectionFailure.HOST_UNRESOLVED, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a 404 means the port answers but is not the agent`() = runTest {
        val report = tester(TcpProbeResult.Reachable, HttpStatusCode.NotFound).test(device)

        assertEquals(ConnectionFailure.NOT_AN_AGENT, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a 401 is reported as a token problem, not as offline`() = runTest {
        val report = tester(TcpProbeResult.Reachable, HttpStatusCode.Unauthorized).test(device)

        assertEquals(ConnectionFailure.UNAUTHORIZED, (report as ConnectionReport.Failed).reason)
    }

    @Test
    fun `a healthy agent reports its version`() = runTest {
        val report = tester(TcpProbeResult.Reachable).test(device)

        val online = report as ConnectionReport.AgentOnline
        assertEquals("0.1.0", online.version)
        assertEquals("Desktop-Alpha", online.hostname)
    }
}
