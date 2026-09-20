package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.connectivity.ConnectionFailure
import com.xukunz.wakeupmywall.core.connectivity.ConnectionReport
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DeviceSetupFormTest {

    private val input = DeviceSetupInput(
        name = "My PC",
        mac = "00:1A:2B:3C:4D:5E",
        ip = "192.168.1.10",
        broadcast = "192.168.1.255",
        wolPort = "9",
        agentPort = "9876",
        agentHost = "192.168.1.10",
    )

    @Test
    fun `the wol port stepper walks up and down`() = runComposeUiTest {
        var current by mutableStateOf(input)
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    input = current,
                    result = DeviceSetupValidator.validate(current),
                    devices = emptyList(),
                    report = null,
                    isTesting = false,
                    onInputChange = { current = it },
                    onSave = {},
                    onTestConnection = {},
                    onSelectDevice = {},
                    onDeleteDevice = {},
                    onAddDevice = {},
                )
            }
        }

        onNodeWithTag("device:wolPort-increment").performClick()
        assertEquals("10", current.wolPort)

        onNodeWithTag("device:wolPort-decrement").performClick()
        assertEquals("9", current.wolPort)
    }

    @Test
    fun `a refused connection is shown together with its reason`() = runComposeUiTest {
        val message = "Nothing is listening on 192.168.1.10:9876 — the PC may be asleep, or the Agent is not running"
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    input = input,
                    result = DeviceSetupValidator.validate(input),
                    devices = emptyList(),
                    report = ConnectionReport.Failed(ConnectionFailure.PORT_REFUSED, message),
                    isTesting = false,
                    onInputChange = {},
                    onSave = {},
                    onTestConnection = {},
                    onSelectDevice = {},
                    onDeleteDevice = {},
                    onAddDevice = {},
                )
            }
        }

        onNodeWithTag("device:connection", useUnmergedTree = true).assertTextEquals(message)
    }

    @Test
    fun `the connection block says which state it is in`() = runComposeUiTest {
        var report by mutableStateOf<ConnectionReport?>(null)
        var testing by mutableStateOf(false)
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    input = input,
                    result = DeviceSetupValidator.validate(input),
                    devices = emptyList(),
                    report = report,
                    isTesting = testing,
                    onInputChange = {},
                    onSave = {},
                    onTestConnection = {},
                    onSelectDevice = {},
                    onDeleteDevice = {},
                    onAddDevice = {},
                )
            }
        }

        onNodeWithTag("device:connection", useUnmergedTree = true).assertTextEquals("Not tested yet")

        testing = true
        waitForIdle()
        onNodeWithTag("device:connection", useUnmergedTree = true).assertTextEquals("Testing…")

        testing = false
        report = ConnectionReport.AgentOnline(hostname = "DESKTOP-ALPHA", version = "0.1.0")
        waitForIdle()
        onNodeWithTag("device:connection", useUnmergedTree = true)
            .assertTextEquals("Agent online · DESKTOP-ALPHA · v0.1.0")

        report = ConnectionReport.WolOnly(mac = "00:1A:2B:3C:4D:5E")
        waitForIdle()
        onNodeWithTag("device:connection", useUnmergedTree = true)
            .assertTextEquals("WOL Ready · no Agent configured yet")
    }
}
