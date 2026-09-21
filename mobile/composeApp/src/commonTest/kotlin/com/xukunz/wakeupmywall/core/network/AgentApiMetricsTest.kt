package com.xukunz.wakeupmywall.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val metricsJsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private val sampleMetrics = """
    {"capturedAtUtc":"2026-09-21T00:58:02Z",
     "identity":{"hostname":"DESKTOP-ALPHA","os":"Windows 11 Pro","cpuName":"AMD Ryzen 7 7700X",
                 "cpuShortName":"Ryzen 7 7700X","gpuName":"NVIDIA GeForce RTX 4070 Ti",
                 "gpuShortName":"RTX 4070 Ti","ramModule":"32 GB DDR5-6000","storageModule":"NVMe 2 TB"},
     "cpu":{"name":"AMD Ryzen 7 7700X","usagePercent":12.5,"clockGhz":4.3,"cores":8,"threads":16,
            "tempC":42.0,"fanRpm":980},
     "gpu":{"name":"NVIDIA GeForce RTX 4070 Ti","usagePercent":8.0,"tempC":38.0,"vramUsedGb":2.4,
            "vramTotalGb":12.0,"fanRpm":1200},
     "memory":{"usagePercent":38.0,"usedGb":12.2,"totalGb":32.0},
     "storage":{"usagePercent":95.0,"usedTb":1.9,"totalTb":2.0,"freeGb":102.0,"tempC":41.0},
     "disks":[{"name":"Windows","mount":"C:\\","usagePercent":60.0,"usedGb":307.2,"totalGb":512.0,
               "freeGb":151.3,"tempC":41.0},
              {"name":"Data","mount":"D:\\","usagePercent":70.5,"usedGb":1443.2,"totalGb":2048.0,
               "freeGb":604.8,"tempC":null}],
     "thermal":{"motherboardTempC":35.0,"caseFanRpm":870},
     "network":{"downloadMbps":12.4,"uploadMbps":3.1},
     "uptimeSeconds":289440,"bootedAtUtc":"2025-04-18T12:00:00Z"}
""".trimIndent()

private val sparseMetrics = """
    {"capturedAtUtc":"2026-09-21T00:58:02Z",
     "identity":{"hostname":"PC","os":"Windows 11","cpuName":"CPU","cpuShortName":"CPU",
                 "gpuName":"GPU","gpuShortName":"GPU","ramModule":"RAM","storageModule":"SSD"},
     "cpu":{"name":"CPU","threads":8},
     "gpu":{"name":"GPU"},
     "memory":{},
     "storage":{},
     "thermal":{},
     "network":{},
     "uptimeSeconds":120,"bootedAtUtc":"2026-09-21T00:00:00Z"}
""".trimIndent()

class AgentApiMetricsTest {

    @Test
    fun `system sends the bearer token and parses every section`() = runTest {
        var path = ""
        var authorization: String? = null
        val engine = MockEngine { request ->
            path = request.url.encodedPath
            authorization = request.headers[HttpHeaders.Authorization]
            respond(sampleMetrics, HttpStatusCode.OK, metricsJsonHeaders)
        }

        val result = AgentApi(createAgentHttpClient(engine)).system("http://192.168.1.10:9876", "token-1")

        assertEquals("/api/v1/system", path)
        assertEquals("Bearer token-1", authorization)
        val metrics = (result as ApiResult.Success).value
        assertEquals("DESKTOP-ALPHA", metrics.identity.hostname)
        assertEquals("Ryzen 7 7700X", metrics.identity.cpuShortName)
        assertEquals(12.5f, metrics.cpu.usagePercent)
        assertEquals(4.3f, metrics.cpu.clockGhz)
        assertEquals(980, metrics.cpu.fanRpm)
        assertEquals(32f, metrics.memory.totalGb)
        assertEquals(1.9f, metrics.storage.usedTb)
        assertEquals(870, metrics.thermal.caseFanRpm)
        assertEquals(12.4f, metrics.network.downloadMbps)
        assertEquals(289440L, metrics.uptimeSeconds)
    }

    @Test
    fun `the fixed disk list comes from the top level of the payload`() = runTest {
        // 契约点：Agent 的 `SystemMetricsPayload.Disks` 是**顶层字段**，不在 `storage` 里。
        // 之前的实现只在 `storage.disks` 里找它，于是真实 Agent 的多盘清单永远是空的，
        // Storage 卡片一直只有合成出来的一张盘、翻页控件从不出现（用户实测）。
        val engine = MockEngine { respond(sampleMetrics, HttpStatusCode.OK, metricsJsonHeaders) }

        val metrics = (AgentApi(createAgentHttpClient(engine))
            .system("http://192.168.1.10:9876", "token-1") as ApiResult.Success).value

        assertEquals(2, metrics.disks.size)
        assertEquals("C:\\", metrics.disks[0].mount)
        assertEquals(307.2f, metrics.disks[0].usedGb)
        assertEquals(41f, metrics.disks[0].tempC)
        assertEquals("Data", metrics.disks[1].name)
        assertEquals(2048f, metrics.disks[1].totalGb)
        assertNull(metrics.disks[1].tempC)
    }

    @Test
    fun `an agent without the disk list parses to an empty list instead of failing`() = runTest {
        // 老 Agent（0.4.0 之前）没有 `disks`：载荷要照样解析，交给映射层用系统盘合成一条兜底。
        val engine = MockEngine { respond(sparseMetrics, HttpStatusCode.OK, metricsJsonHeaders) }

        val metrics = (AgentApi(createAgentHttpClient(engine))
            .system("http://192.168.1.10:9876", "token-1") as ApiResult.Success).value

        assertEquals(emptyList(), metrics.disks)
    }

    @Test
    fun `missing sensors stay null instead of becoming zero`() = runTest {
        val engine = MockEngine { respond(sparseMetrics, HttpStatusCode.OK, metricsJsonHeaders) }

        val metrics = (AgentApi(createAgentHttpClient(engine))
            .system("http://192.168.1.10:9876", null) as ApiResult.Success).value

        assertNull(metrics.cpu.usagePercent)
        assertNull(metrics.cpu.tempC)
        assertNull(metrics.cpu.fanRpm)
        assertNull(metrics.memory.totalGb)
        assertNull(metrics.storage.tempC)
        assertNull(metrics.thermal.motherboardTempC)
        assertNull(metrics.network.downloadMbps)
        assertNull(metrics.gpu.vramTotalGb)
        assertEquals(8, metrics.cpu.threads)
    }

    @Test
    fun `a rejected token is unauthorized with a readable reason`() = runTest {
        val engine = MockEngine {
            respond("""{"error":"missing or invalid token"}""", HttpStatusCode.Unauthorized, metricsJsonHeaders)
        }

        val result = AgentApi(createAgentHttpClient(engine)).system("http://192.168.1.10:9876", "wrong")

        val failure = result as ApiResult.Failure
        assertEquals(ApiFailure.UNAUTHORIZED, failure.reason)
        assertEquals("missing or invalid token", failure.message)
    }

    @Test
    fun `a malformed payload is a decoding failure rather than a crash`() = runTest {
        val engine = MockEngine { respond("""{"capturedAtUtc":42}""", HttpStatusCode.OK, metricsJsonHeaders) }

        val result = AgentApi(createAgentHttpClient(engine)).system("http://192.168.1.10:9876", "token-1")

        assertEquals(ApiFailure.DECODING, (result as ApiResult.Failure).reason)
    }
}
