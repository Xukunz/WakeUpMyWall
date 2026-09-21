package com.xukunz.wakeupmywall.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AgentStatus(val hostname: String, val agentVersion: String, val uptimeSeconds: Long)

@Serializable
data class PairingResponse(val token: String)

@Serializable
data class ActionEntry(val id: String, val name: String)

@Serializable
data class PowerResponse(val action: String, val accepted: Boolean)

/**
 * Phase 5A 的 `GET /api/v1/system` 载荷。字段名与 `docs/plans/agent-api.md` 的表格逐字对应；
 * **可空的数值字段表示 Agent 没读到那个传感器**（例如没有硬件监控库权限时的温度），
 * 所以这里一律保留 `null`，绝不在解析层把它变成 0。
 */
@Serializable
data class AgentMetrics(
    val capturedAtUtc: String,
    val identity: AgentIdentity,
    val cpu: AgentCpuMetrics,
    val gpu: AgentGpuMetrics,
    val memory: AgentMemoryMetrics,
    val storage: AgentStorageMetrics,
    val thermal: AgentThermalMetrics,
    val network: AgentNetworkMetrics,
    val uptimeSeconds: Long,
    val bootedAtUtc: String,
)

@Serializable
data class AgentIdentity(
    val hostname: String,
    val os: String,
    val cpuName: String,
    val cpuShortName: String,
    val gpuName: String,
    val gpuShortName: String,
    val ramModule: String,
    val storageModule: String,
)

@Serializable
data class AgentCpuMetrics(
    val name: String,
    val usagePercent: Float? = null,
    val clockGhz: Float? = null,
    val cores: Int? = null,
    val threads: Int? = null,
    val tempC: Float? = null,
    val fanRpm: Int? = null,
)

@Serializable
data class AgentGpuMetrics(
    val name: String,
    val usagePercent: Float? = null,
    val tempC: Float? = null,
    val vramUsedGb: Float? = null,
    val vramTotalGb: Float? = null,
    val fanRpm: Int? = null,
)

@Serializable
data class AgentMemoryMetrics(
    val usagePercent: Float? = null,
    val usedGb: Float? = null,
    val totalGb: Float? = null,
)

@Serializable
data class AgentStorageMetrics(
    val usagePercent: Float? = null,
    val usedTb: Float? = null,
    val totalTb: Float? = null,
    val freeGb: Float? = null,
    val tempC: Float? = null,
)

@Serializable
data class AgentThermalMetrics(
    val motherboardTempC: Float? = null,
    val caseFanRpm: Int? = null,
)

@Serializable
data class AgentNetworkMetrics(
    val downloadMbps: Float? = null,
    val uploadMbps: Float? = null,
)

/** spec §5 的四个电源动作；[path] 就是 Agent 端点里的动作名。 */
enum class PowerAction(val path: String) {
    SLEEP("sleep"),
    SHUTDOWN("shutdown"),
    RESTART("restart"),
    LOCK("lock"),
}

class AgentApi(private val client: HttpClient) {

    suspend fun status(baseUrl: String, token: String?): ApiResult<AgentStatus> =
        call { client.get("$baseUrl/api/v1/status") { bearer(token) } }

    /** spec §8 的指标：受保护端点，必须带 Token（Phase 5A 起不再回 501）。 */
    suspend fun system(baseUrl: String, token: String?): ApiResult<AgentMetrics> =
        call { client.get("$baseUrl/api/v1/system") { bearer(token) } }

    /** 配对端点免鉴权：用 PC 端打印的 6 位配对码换 Token。 */
    suspend fun pair(baseUrl: String, code: String): ApiResult<PairingResponse> =
        call {
            client.post("$baseUrl/api/v1/pairing") {
                contentType(ContentType.Application.Json)
                setBody(PairingRequest(code))
            }
        }

    suspend fun actions(baseUrl: String, token: String?): ApiResult<List<ActionEntry>> =
        call { client.get("$baseUrl/api/v1/actions") { bearer(token) } }

    suspend fun power(baseUrl: String, token: String?, action: PowerAction): ApiResult<PowerResponse> =
        call { client.post("$baseUrl/api/v1/power/${action.path}") { bearer(token) } }

    private suspend inline fun <reified T> call(block: suspend () -> HttpResponse): ApiResult<T> = try {
        val response = block()
        if (response.status.value in 200..299) {
            ApiResult.Success(response.body<T>())
        } else {
            ApiResult.Failure(response.status.toFailure(), response.errorText())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        ApiResult.Failure(ApiFailure.TIMEOUT, e.message ?: "timeout")
    } catch (e: ContentConvertException) {
        // Ktor 3.6 的 JSON 转换失败是 ContentConvertException（实测为 JsonConvertException），
        // 原始 kotlinx 异常挂在 cause 上，因此两者都要映射为 DECODING。
        ApiResult.Failure(ApiFailure.DECODING, e.message ?: "decoding error")
    } catch (e: SerializationException) {
        ApiResult.Failure(ApiFailure.DECODING, e.message ?: "decoding error")
    } catch (e: Throwable) {
        ApiResult.Failure(ApiFailure.NETWORK, e.message ?: "network error")
    }

    private fun HttpRequestBuilder.bearer(token: String?) {
        if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
    }
}

private fun HttpStatusCode.toFailure(): ApiFailure = when (value) {
    401, 403 -> ApiFailure.UNAUTHORIZED
    404 -> ApiFailure.NOT_FOUND
    409 -> ApiFailure.CONFLICT
    in 500..599 -> ApiFailure.SERVER
    else -> ApiFailure.NETWORK
}

@Serializable
private data class PairingRequest(val code: String)

private val errorJson = Json { ignoreUnknownKeys = true }

/** Agent 的错误体是 `{"error":"…"}`；读不出来就退回状态码，别丢信息。 */
private suspend fun HttpResponse.errorText(): String {
    val raw = runCatching { bodyAsText() }.getOrDefault("")
    val serverError = runCatching {
        errorJson.parseToJsonElement(raw).jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull()
    return serverError ?: "HTTP ${status.value}"
}
