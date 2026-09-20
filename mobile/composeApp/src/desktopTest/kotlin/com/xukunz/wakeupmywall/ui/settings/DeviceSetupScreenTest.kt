package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.data.mock.MockData
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DeviceSetupScreenTest {

    private val validInput = DeviceSetupInput(
        name = "My PC",
        mac = "AA:BB:CC:DD:EE:FF",
        ip = "192.168.1.10",
        broadcast = "192.168.1.255",
        wolPort = "9",
        agentPort = "9876",
        agentHost = "192.168.1.10",
    )

    @Test
    fun `invalid form disables save and shows mac error`() = runComposeUiTest {
        val input = validInput.copy(mac = "bad")
        val result = DeviceSetupValidator.validate(input)
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    input = input,
                    result = result,
                    devices = MockData.devices,
                    report = null,
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

        onNodeWithTag("device:save").assertIsNotEnabled()
        onNodeWithTag("device:error:mac").assertTextEquals("Enter a valid MAC address")
    }

    @Test
    fun `valid form enables save`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    validInput,
                    DeviceSetupValidator.validate(validInput),
                    devices = MockData.devices,
                    report = null,
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

        onNodeWithTag("device:save").assertIsEnabled()
    }

    @Test
    fun `test connection is forwarded`() = runComposeUiTest {
        var tested = false
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    validInput,
                    DeviceSetupValidator.validate(validInput),
                    devices = MockData.devices,
                    report = null,
                    isTesting = false,
                    onInputChange = {},
                    onSave = {},
                    onTestConnection = { tested = true },
                    onSelectDevice = {},
                    onDeleteDevice = {},
                    onAddDevice = {},
                )
            }
        }

        onNodeWithTag("device:test").performClick()

        assertTrue(tested)
    }

    @Test
    fun `saved computers and integrated services follow the concept spec`() = runComposeUiTest {
        setContent {
            WakeUpMyWallTheme {
                DeviceSetupScreen(
                    validInput,
                    DeviceSetupValidator.validate(validInput),
                    devices = MockData.devices,
                    report = null,
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

        onNodeWithTag("device:saved").assertExists()
        onNodeWithTag("device:saved:desktop-alpha", useUnmergedTree = true).assertExists()
        onNodeWithTag("device:services").assertExists()
        onNodeWithTag("device:services:weather", useUnmergedTree = true)
            .assertTextEquals("OpenWeatherMap · Riverside, CA")
    }
}
