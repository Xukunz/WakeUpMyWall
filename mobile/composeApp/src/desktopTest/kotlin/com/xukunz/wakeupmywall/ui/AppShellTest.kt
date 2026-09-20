package com.xukunz.wakeupmywall.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppShellTest {

    private val device = PcDevice(
        id = "1",
        name = "My PC",
        macAddress = MacAddress.parse("AA:BB:CC:DD:EE:FF"),
    )

    private fun rail(state: PcState) = powerRailModel(state, device)

    @Test
    fun `renders main content and rail side by side`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                AppShell(rail = rail(PcState.ONLINE), onRailEvent = {}) {
                    Text("MAIN", modifier = Modifier.testTag("shell:main"))
                }
            }
        }

        onNodeWithTag("shell:main").assertIsDisplayed()
        onNodeWithTag("powerrail").assertIsDisplayed()
        onNodeWithTag("appshell:main").assertExists()
        onNodeWithTag("appshell:rail").assertExists()
    }

    @Test
    fun `phone portrait stacks content above a bottom rail`() = runDesktopComposeUiTest(412, 915) {
        setContent {
            WakeUpMyWallTheme {
                AppShell(rail = rail(PcState.ONLINE), onRailEvent = {}) {
                    Text("MAIN", modifier = Modifier.testTag("shell:main"))
                }
            }
        }

        // 20:9 手机：常驻控制栏仍然在（同一批标签），只是挪到了底部。
        onNodeWithTag("powerrail").assertIsDisplayed()
        onNodeWithTag("powerrail:primary").assertIsDisplayed()
        onNodeWithTag("shell:main").assertIsDisplayed()
    }

    @Test
    fun `foldable inner screen keeps the side rail`() = runDesktopComposeUiTest(790, 700) {
        setContent {
            WakeUpMyWallTheme {
                AppShell(rail = rail(PcState.ONLINE), onRailEvent = {}) {
                    Text("MAIN", modifier = Modifier.testTag("shell:main"))
                }
            }
        }

        // 4:3.55 内屏横放（790×700）：宽度够 + 横屏，回到概念图的 72/28 侧栏。
        onNodeWithTag("powerrail").assertIsDisplayed()
        onNodeWithTag("shell:main").assertIsDisplayed()
    }

    @Test
    fun `rail events are forwarded`() = runComposeUiTest {
        var received: RailEvent? = null
        setContent {
            WakeUpMyWallTheme {
                AppShell(rail = rail(PcState.ONLINE), onRailEvent = { received = it }) {
                    Text("MAIN")
                }
            }
        }

        onNodeWithTag("powerrail:shutdown").performClick()

        assertEquals(RailEvent.Shutdown, received)
    }

    @Test
    fun `settings event is forwarded too`() = runComposeUiTest {
        var received: RailEvent? = null
        setContent {
            WakeUpMyWallTheme {
                AppShell(rail = rail(PcState.ONLINE), onRailEvent = { received = it }) {
                    Text("MAIN")
                }
            }
        }

        onNodeWithTag("powerrail:settings").performClick()

        assertEquals(RailEvent.Settings, received)
    }
}
