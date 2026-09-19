package com.xukunz.wakeupmywall.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class AgentApiTest {

    @Test
    fun `parses valid status payload`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"hostname":"Desktop-Alpha","agentVersion":"0.1.0","uptimeSeconds":1234}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.status("http://192.168.1.10:9876", token = "t")

        val value = (result as ApiResult.Success).value
        assertEquals("Desktop-Alpha", value.hostname)
        assertEquals(1234, value.uptimeSeconds)
    }

    @Test
    fun `maps 401 to unauthorized failure`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.status("http://192.168.1.10:9876", token = "bad")

        assertEquals(ApiFailure.UNAUTHORIZED, (result as ApiResult.Failure).reason)
    }

    @Test
    fun `maps malformed payload to decoding failure`() = runTest {
        val engine = MockEngine {
            respond("""{"unexpected":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.status("http://192.168.1.10:9876", token = "t")

        assertEquals(ApiFailure.DECODING, (result as ApiResult.Failure).reason)
    }

    @Test
    fun `sends bearer token when provided`() = runTest {
        var seen: String? = null
        val engine = MockEngine { request ->
            seen = request.headers[HttpHeaders.Authorization]
            respond("""{"hostname":"h","agentVersion":"1","uptimeSeconds":1}""", HttpStatusCode.OK, jsonHeaders)
        }
        AgentApi(createAgentHttpClient(engine)).status("http://host:9876", token = "secret")

        assertEquals("Bearer secret", seen)
    }
}
