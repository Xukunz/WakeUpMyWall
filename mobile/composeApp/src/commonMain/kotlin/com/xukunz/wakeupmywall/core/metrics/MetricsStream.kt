package com.xukunz.wakeupmywall.core.metrics

import com.xukunz.wakeupmywall.core.network.AgentMetrics
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.withTimeoutOrNull
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
 * HTTP 基址 → 指标流的地址。**必须把 scheme 换成 `ws`**：拿 `http://…` 去调 `client.webSocket`
 * 会变成一次普通 GET（Agent 那边如实回 400），实测踩到过一次。
 */
fun webSocketUrl(baseUrl: String): String {
    val wsBase = when {
        baseUrl.startsWith("https://") -> "wss://" + baseUrl.removePrefix("https://")
        baseUrl.startsWith("http://") -> "ws://" + baseUrl.removePrefix("http://")
        else -> baseUrl
    }
    return "${wsBase.trimEnd('/')}/ws/v1/metrics"
}

/**
 * Ktor 的 WebSocket 实现。Token 走**握手头**（与 Agent 侧的校验方式对应）；
 * 断线由 `webSocket` 抛异常暴露给调用方，由调用方决定退避重连。
 */
class KtorMetricsStream(private val client: HttpClient) : MetricsStream {
    override fun frames(baseUrl: String, token: String): Flow<AgentMetrics> = flow {
        client.webSocket(
            urlString = webSocketUrl(baseUrl),
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

/**
 * 给流加一个"多久没收到帧就结束"的闸门，用于重连判定。
 *
 * 为什么必须有：对端**接受连接但不回帧**时（Agent 挂了、或者 adb reverse 那一头连不上宿主端口、
 * 连接被接住却永远没有响应），`webSocket` 自己不会抛错——只靠它的话，这条流会一直挂着，
 * 重连逻辑永远轮不上（实测踩到：Agent 重启后 app 一直留在 HTTP 兜底上，不再回到流式）。
 */
fun <T> Flow<T>.withIdleTimeout(idleTimeoutMillis: Long): Flow<T> = channelFlow {
    require(idleTimeoutMillis > 0) { "idleTimeoutMillis must be positive, was $idleTimeoutMillis" }
    // 上游跑在子协程里：超时后取消它，连底下的 WebSocket 会话一起收掉，才能干净地重连。
    val upstream = produceIn(this)
    try {
        while (true) {
            val frame = withTimeoutOrNull(idleTimeoutMillis) { upstream.receiveCatching().getOrNull() } ?: break
            send(frame)
        }
    } finally {
        upstream.cancel()
    }
}
