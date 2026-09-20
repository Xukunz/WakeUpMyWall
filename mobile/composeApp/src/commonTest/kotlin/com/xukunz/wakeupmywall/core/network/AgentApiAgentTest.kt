package com.xukunz.wakeupmywall.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class AgentApiAgentTest {

    @Test
    fun `pairing returns the token and hits the pairing endpoint`() = runTest {
        var path = ""
        val engine = MockEngine { request ->
            path = request.url.encodedPath
            respond("""{"token":"abc123"}""", HttpStatusCode.OK, jsonHeaders)
        }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.pair("http://192.168.1.10:9876", "123456")

        assertEquals("/api/v1/pairing", path)
        assertEquals("abc123", (result as ApiResult.Success).value.token)
    }

    @Test
    fun `a rejected pairing code surfaces the agent's own wording`() = runTest {
        val engine = MockEngine {
            respond("""{"error":"invalid or expired pairing code"}""", HttpStatusCode.Forbidden, jsonHeaders)
        }

        val result = AgentApi(createAgentHttpClient(engine)).pair("http://192.168.1.10:9876", "000000")

        val failure = result as ApiResult.Failure
        assertEquals(ApiFailure.UNAUTHORIZED, failure.reason)
        assertTrue(failure.message.contains("invalid or expired"))
    }

    @Test
    fun `an already paired agent reports a conflict`() = runTest {
        val engine = MockEngine {
            respond("""{"error":"already paired"}""", HttpStatusCode.Conflict, jsonHeaders)
        }

        val result = AgentApi(createAgentHttpClient(engine)).pair("http://192.168.1.10:9876", "123456")

        assertEquals(ApiFailure.CONFLICT, (result as ApiResult.Failure).reason)
    }

    @Test
    fun `power posts to the action path with the bearer token`() = runTest {
        var seenPath = ""
        var seenMethod: HttpMethod? = null
        var seenAuth: String? = null
        val engine = MockEngine { request ->
            seenPath = request.url.encodedPath
            seenMethod = request.method
            seenAuth = request.headers[HttpHeaders.Authorization]
            respond("""{"action":"shutdown","accepted":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        val api = AgentApi(createAgentHttpClient(engine))

        val result = api.power("http://192.168.1.10:9876", "secret", PowerAction.SHUTDOWN)

        assertEquals("/api/v1/power/shutdown", seenPath)
        assertEquals(HttpMethod.Post, seenMethod)
        assertEquals("Bearer secret", seenAuth)
        assertEquals(PowerResponse("shutdown", true), (result as ApiResult.Success).value)
    }

    @Test
    fun `an unknown power action is reported as not found`() = runTest {
        val engine = MockEngine {
            respond("""{"error":"unknown power action"}""", HttpStatusCode.NotFound, jsonHeaders)
        }

        val result = AgentApi(createAgentHttpClient(engine))
            .power("http://192.168.1.10:9876", "secret", PowerAction.LOCK)

        val failure = result as ApiResult.Failure
        assertEquals(ApiFailure.NOT_FOUND, failure.reason)
        assertTrue(failure.message.contains("unknown power action"))
    }

    @Test
    fun `actions are parsed into id and display name pairs`() = runTest {
        val engine = MockEngine {
            respond(
                """[{"id":"browser","name":"Open Browser"},{"id":"steam","name":"Launch Steam"}]""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = AgentApi(createAgentHttpClient(engine)).actions("http://192.168.1.10:9876", "secret")

        assertEquals(
            listOf(ActionEntry("browser", "Open Browser"), ActionEntry("steam", "Launch Steam")),
            (result as ApiResult.Success).value,
        )
    }
}
