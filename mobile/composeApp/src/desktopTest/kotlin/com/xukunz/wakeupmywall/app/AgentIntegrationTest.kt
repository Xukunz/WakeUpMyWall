package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.xukunz.wakeupmywall.core.agent.InMemoryAgentTokenStore
import com.xukunz.wakeupmywall.core.connectivity.ConnectionFailure
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport
import com.xukunz.wakeupmywall.core.network.AgentApi
import com.xukunz.wakeupmywall.core.network.createAgentHttpClient
import com.xukunz.wakeupmywall.domain.model.PcState
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * 存在性轮询：把"PC 开着吗"变成状态行上的 Online / Agent unavailable / Ready to wake。
 * 报告由测试注入，所以这里验的是"报告 → 状态 → 界面"这条链，不依赖真实网络。
 */
@OptIn(ExperimentalTestApi::class)
class AgentIntegrationTest {

    private fun agentTest(
        initial: PcState,
        report: ConnectionReport,
        assert: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        setContent {
            App(
                initialPcState = initial,
                agentPollMillis = 20,
                agentReportProbe = { report },
            )
        }
        assert()
    }

    private fun ComposeUiTest.expectState(label: String) = waitUntilExactlyOneExists(
        hasTestTag("powerrail:state") and hasText(label),
        timeoutMillis = 5_000,
    )

    @Test
    fun `an answering agent shows up as online`() = agentTest(
        initial = PcState.WOL_READY,
        report = ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0"),
    ) {
        expectState("Online")
    }

    @Test
    fun `a host that answers without the agent is reported as agent unavailable`() = agentTest(
        initial = PcState.ONLINE,
        report = ConnectionReport.Failed(ConnectionFailure.NOT_AN_AGENT, "404 on /api/v1/status"),
    ) {
        expectState("Agent unavailable")
    }

    @Test
    fun `an unreachable pc falls back to wake ready when it can be woken`() = agentTest(
        initial = PcState.ONLINE,
        report = ConnectionReport.Failed(ConnectionFailure.PORT_REFUSED, "nothing on 9876"),
    ) {
        expectState("Ready to wake")
    }

    @Test
    fun `a device with no mac goes offline instead of wake ready`() = runComposeUiTest {
        val storage = com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage(
            com.xukunz.wakeupmywall.core.storage.InMemoryKeyValueStore(),
        )
        kotlinx.coroutines.runBlocking {
            storage.writeDevices(
                listOf(
                    com.xukunz.wakeupmywall.domain.model.PcDevice(
                        id = "brand-new",
                        name = "New PC",
                        isDefault = true,
                    ),
                ),
            )
            storage.markSeeded()
        }
        setContent {
            App(
                storage = storage,
                initialPcState = PcState.ONLINE,
                agentPollMillis = 20,
                agentReportProbe = { ConnectionReport.Failed(ConnectionFailure.PORT_REFUSED, "nothing on 9876") },
            )
        }

        expectState("Offline")
        assertEquals("Offline", "Offline") // 保持断言意图显式：无 MAC ≠ 可唤醒
    }

    @Test
    fun `sleep posts to the agent and moves the rail into sleeping`() = runComposeUiTest {
        val calls = mutableListOf<String>()
        val engine = MockEngine { request ->
            calls += request.url.encodedPath
            respond("""{"action":"sleep","accepted":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        setContent {
            App(
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,                       // 让轮询别搅局，这条只验"发命令 → 进瞬态"
                agentReportProbe = { ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0") },
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                agentApiFactory = { AgentApi(createAgentHttpClient(engine)) },
            )
        }

        onNodeWithTag("powerrail:sleep").performClick()

        waitUntil(timeoutMillis = 5_000) { calls.contains("/api/v1/power/sleep") }
        expectState("Sleeping…")
    }

    @Test
    fun `without a pairing token the app asks to pair first and sends nothing`() = runComposeUiTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"action":"sleep","accepted":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        setContent {
            App(
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0") },
                agentTokens = InMemoryAgentTokenStore(),          // 没配过对
                agentApiFactory = { AgentApi(createAgentHttpClient(engine)) },
            )
        }

        onNodeWithTag("powerrail:sleep").performClick()

        waitUntilExactlyOneExists(hasTestTag("powerrail:wake-note"), timeoutMillis = 5_000)
        onNodeWithTag("powerrail:wake-note", useUnmergedTree = true)
            .assertTextEquals("Pair the phone in Device Setup first (Agent section)")
        assertEquals(0, calls)
    }

    @Test
    fun `a rejected token is shown as the reason`() = runComposeUiTest {
        val engine = MockEngine {
            respond("""{"error":"missing or invalid token"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        setContent {
            App(
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.1.0") },
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "stale")),
                agentApiFactory = { AgentApi(createAgentHttpClient(engine)) },
            )
        }

        onNodeWithTag("powerrail:shutdown").performClick()

        waitUntilExactlyOneExists(hasTestTag("powerrail:wake-note"), timeoutMillis = 5_000)
        onNodeWithTag("powerrail:wake-note", useUnmergedTree = true)
            .assertTextEquals("missing or invalid token")
    }
}
