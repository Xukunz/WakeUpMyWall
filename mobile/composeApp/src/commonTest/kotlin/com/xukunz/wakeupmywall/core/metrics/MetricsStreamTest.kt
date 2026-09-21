package com.xukunz.wakeupmywall.core.metrics

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetricsStreamTest {

    @Test
    fun `a websocket frame parses into the same shape the http endpoint returns`() {
        val metrics = parseMetricsFrame(
            """
            {"capturedAtUtc":"2026-09-21T01:12:00Z",
             "identity":{"hostname":"DESKTOP-ALPHA","os":"Windows 11 Pro","cpuName":"AMD Ryzen 7 7700X",
                         "cpuShortName":"Ryzen 7 7700X","gpuName":"NVIDIA GeForce RTX 4070 Ti",
                         "gpuShortName":"RTX 4070 Ti","ramModule":"32 GB DDR5-6000","storageModule":"NVMe 2 TB"},
             "cpu":{"name":"AMD Ryzen 7 7700X","usagePercent":12.5,"threads":16},
             "gpu":{"name":"RTX 4070 Ti"},"memory":{"usagePercent":38.0},
             "storage":{},"thermal":{},"network":{"downloadMbps":12.4},
             "uptimeSeconds":289440,"bootedAtUtc":"2025-04-18T12:00:00Z"}
            """.trimIndent()
        )

        assertEquals("DESKTOP-ALPHA", metrics?.identity?.hostname)
        assertEquals(12.5f, metrics?.cpu?.usagePercent)
        assertEquals(12.4f, metrics?.network?.downloadMbps)
        // 缺项保持 null —— 流式数据与轮询数据必须是同一份契约。
        assertNull(metrics?.cpu?.tempC)
        assertNull(metrics?.storage?.tempC)
    }

    @Test
    fun `a broken frame is skipped instead of killing the stream`() {
        assertNull(parseMetricsFrame("{not json"))
        assertNull(parseMetricsFrame(""))
    }

    @Test
    fun `the stream url swaps the scheme to ws and keeps host and port`() {
        assertEquals("ws://127.0.0.1:9876/ws/v1/metrics", webSocketUrl("http://127.0.0.1:9876"))
        assertEquals("wss://desk-pc.local:9876/ws/v1/metrics", webSocketUrl("https://desk-pc.local:9876/"))
    }

    @Test
    fun `a stream that goes quiet is ended so the caller can reconnect`() = runTest {
        val frames = flow {
            emit("first")
            emit("second")
            delay(60_000)      // 之后再也不发帧：模拟"连接被接住但没有响应"
            emit("never")
        }

        val received = frames.withIdleTimeout(idleTimeoutMillis = 3_000).toList()

        assertEquals(listOf("first", "second"), received)
    }

    @Test
    fun `a stream that keeps delivering frames is not cut off`() = runTest {
        val frames = flow {
            repeat(10) {
                emit("frame-$it")
                delay(500)      // 500 ms < 3 秒的闸门
            }
        }

        val received = frames.withIdleTimeout(idleTimeoutMillis = 3_000).toList()

        assertEquals(10, received.size)
    }
}
