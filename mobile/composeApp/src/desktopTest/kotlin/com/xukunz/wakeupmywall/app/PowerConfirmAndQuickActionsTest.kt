package com.xukunz.wakeupmywall.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.xukunz.wakeupmywall.core.agent.InMemoryAgentTokenStore
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
 * 三条用户实测反馈：
 *  1. Sleep/Shutdown/Restart 会误触 —— 现在必须先过确认弹窗；
 *  2. 电源按钮链路上 Quick Actions 此前"点了没反应" —— 现在真的发 `POST /api/v1/actions/{id}` 并给回执；
 *  3. 进 PC 卡片后只能点顶卡返回 —— 现在左上角有返回按钮（Android 上系统返回手势同样生效）。
 */
@OptIn(ExperimentalTestApi::class)
class PowerConfirmAndQuickActionsTest {

    private val online = ConnectionReport.AgentOnline("DESKTOP-ALPHA", "0.4.0")

    @Test
    fun `sleep asks for confirmation before doing anything`() = runComposeUiTest {
        val calls = mutableListOf<String>()
        val engine = MockEngine { request ->
            calls += request.url.encodedPath
            respond("""{"action":"sleep","accepted":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        setContent {
            App(
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { online },
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                agentApiFactory = { AgentApi(createAgentHttpClient(engine)) },
            )
        }

        onNodeWithTag("powerrail:sleep").performClick()
        onNodeWithTag("dialog:power").assertIsDisplayed()

        // 先取消：什么都不该发生（这正是"避免误触"的关键）。
        onNodeWithTag("dialog:power-cancel").performClick()
        onNodeWithTag("powerrail:state", useUnmergedTree = true).assertTextEquals("Online")
        // 只看电源端点：指标轮询（/api/v1/system）与这条断言无关。
        assertEquals(emptyList(), calls.filter { it.startsWith("/api/v1/power") })

        // 再确认：这才真的发请求并进入瞬态。
        onNodeWithTag("powerrail:sleep").performClick()
        onNodeWithTag("dialog:power-confirm").performClick()
        waitUntilExactlyOneExists(hasTestTag("powerrail:state") and hasText("Sleeping…"), timeoutMillis = 5_000)
        assertEquals(listOf("/api/v1/power/sleep"), calls.filter { it.startsWith("/api/v1/power") })
    }

    @Test
    fun `a quick action really calls the agent and reports back`() = runComposeUiTest {
        val calls = mutableListOf<String>()
        val engine = MockEngine { request ->
            calls += request.url.encodedPath
            respond("""{"id":"steam","executed":true}""", HttpStatusCode.OK, jsonHeaders)
        }
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { online },
                agentTokens = InMemoryAgentTokenStore(mapOf("desktop-alpha" to "token")),
                agentApiFactory = { AgentApi(createAgentHttpClient(engine)) },
            )
        }

        onNodeWithTag("action:Steam").performClick()

        waitUntilExactlyOneExists(hasTestTag("powerrail:wake-note"), timeoutMillis = 5_000)
        assertEquals(listOf("/api/v1/actions/steam"), calls.filter { it.startsWith("/api/v1/actions") })
        onNodeWithTag("powerrail:wake-note", useUnmergedTree = true)
            .assertTextEquals("Steam launched on My PC")
    }

    @Test
    fun `the monitor has a back affordance that returns to the dashboard`() = runComposeUiTest {
        setContent {
            App(
                navigator = AppNavigator().apply { goTo(Workspace.Monitor) },
                initialPcState = PcState.ONLINE,
                agentPollMillis = 60_000,
                agentReportProbe = { online },
            )
        }

        onNodeWithTag("monitor:back").performClick()

        onNodeWithTag("dashboard:greeting").assertIsDisplayed()
    }
}
