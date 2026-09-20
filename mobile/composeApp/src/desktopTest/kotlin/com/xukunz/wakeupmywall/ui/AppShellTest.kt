package com.xukunz.wakeupmywall.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
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
