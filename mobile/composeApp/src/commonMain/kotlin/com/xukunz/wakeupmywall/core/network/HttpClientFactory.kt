package com.xukunz.wakeupmywall.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val agentJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Agent 与 PC 同处局域网，超时必须短：超过 2.5 秒的等待只会拖慢状态机的反馈。
 * 这里刻意不安装 HttpCallValidator，4xx/5xx 不抛异常，由 [AgentApi] 做稳定的错误映射。
 */
fun createAgentHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(agentJson) }
        // 指标流（Phase 5C）走同一个客户端的 WebSocket：不装这个插件，`client.webSocket` 会直接抛。
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 2_500
            connectTimeoutMillis = 2_000
            socketTimeoutMillis = 2_500
        }
    }
    return engine?.let { HttpClient(it, configure) } ?: HttpClient(configure)
}
