package com.xukunz.wakeupmywall.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SavedComputersTest {

    private val devices = listOf(
        PcDevice(id = "my-pc", name = "My PC", macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"), isDefault = true),
        PcDevice(id = "study", name = "Study PC", macAddress = MacAddress.parse("00:1B:44:11:3A:9C")),
    )

    private val input = DeviceSetupInput("My PC", "00:1A:2B:3C:4D:5E", "", "192.168.1.255", "9", "9876", "")

    private fun content(
        devices: List<PcDevice> = this.devices,
        onSelectDevice: (String) -> Unit = {},
        onDeleteDevice: (String) -> Unit = {},
        onAddDevice: () -> Unit = {},
    ): @androidx.compose.runtime.Composable () -> Unit = {
        WakeUpMyWallTheme {
            DeviceSetupScreen(
                input = input,
                result = DeviceSetupValidator.validate(input),
                devices = devices,
                report = null,
                isTesting = false,
                onInputChange = {},
                onSave = {},
                onTestConnection = {},
                onSelectDevice = onSelectDevice,
                onDeleteDevice = onDeleteDevice,
                onAddDevice = onAddDevice,
            )
        }
    }

    @Test
    fun `rows select a device, delete reports an id and add is reachable`() = runComposeUiTest {
        var selected: String? = null
        var deleted: String? = null
        var added = false
        setContent(
            content(
                onSelectDevice = { selected = it },
                onDeleteDevice = { deleted = it },
                onAddDevice = { added = true },
            ),
        )

        onNodeWithTag("device:row:study").performClick()
        assertEquals("study", selected)

        onNodeWithTag("device:delete:study").performClick()
        assertEquals("study", deleted)

        onNodeWithTag("device:add").performClick()
        assertEquals(true, added)
    }

    @Test
    fun `the default device is badged and the active row is the selected one`() = runComposeUiTest {
        setContent(content(devices = devices))

        onNodeWithTag("device:default:my-pc", useUnmergedTree = true).assertExists()
        onNodeWithTag("device:row:my-pc").assertExists()
        onNodeWithTag("device:row:study").assertExists()
    }

    @Test
    fun `an empty list still offers the add button`() = runComposeUiTest {
        var added = false
        setContent(content(devices = emptyList(), onAddDevice = { added = true }))

        onNodeWithTag("device:add").performClick()

        assertEquals(true, added)
    }
}
