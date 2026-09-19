package com.xukunz.wakeupmywall.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

@Serializable
data class AgentStatus(val hostname: String, val agentVersion: String, val uptimeSeconds: Long)

class AgentApi(private val client: HttpClient) {

    suspend fun status(baseUrl: String, token: String?): ApiResult<AgentStatus> = try {
        val response = client.get("$baseUrl/api/v1/status") {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.value in 200..299) {
            ApiResult.Success(response.body<AgentStatus>())
        } else {
            ApiResult.Failure(response.status.toFailure(), "HTTP ${response.status.value}")
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
}

private fun HttpStatusCode.toFailure(): ApiFailure = when (value) {
    401, 403 -> ApiFailure.UNAUTHORIZED
    404 -> ApiFailure.NOT_FOUND
    in 500..599 -> ApiFailure.SERVER
    else -> ApiFailure.NETWORK
}
