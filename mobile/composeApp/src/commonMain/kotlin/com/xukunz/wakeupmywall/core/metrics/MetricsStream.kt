package com.xukunz.wakeupmywall.core.metrics

import com.xukunz.wakeupmywall.core.network.AgentMetrics
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * 指标流的抽象。真实实现是 WebSocket（`/ws/v1/metrics`），测试里注入脚本化的 Flow，
 * 所以"连上/断开/重连"这些行为都能在不联网的情况下验。
 */
interface MetricsStream {
    fun frames(baseUrl: String, token: String): Flow<AgentMetrics>
}

private val frameJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * 一帧文本 → 指标。**坏帧返回 null**：流里偶尔来一条半截 JSON，不该把整条流打断
 * （调用方跳过 null 继续收下一帧）。
 */
fun parseMetricsFrame(frame: String): AgentMetrics? =
    runCatching { frameJson.decodeFromString<AgentMetrics>(frame) }.getOrNull()

/**
 * Ktor 的 WebSocket 实现。Token 走**握手头**（与 Agent 侧的校验方式对应）；
 * 断线由 `webSocket` 抛异常暴露给调用方，由调用方决定退避重连。
 */
class KtorMetricsStream(private val client: HttpClient) : MetricsStream {
    override fun frames(baseUrl: String, token: String): Flow<AgentMetrics> = flow {
        client.webSocket(
            urlString = "$baseUrl/ws/v1/metrics",
            request = { headers.append(HttpHeaders.Authorization, "Bearer $token") },
        ) {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    parseMetricsFrame(frame.readText())?.let { emit(it) }
                }
            }
        }
    }
}
