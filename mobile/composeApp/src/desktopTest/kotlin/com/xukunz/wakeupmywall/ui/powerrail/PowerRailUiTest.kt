package com.xukunz.wakeupmywall.ui.powerrail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.xukunz.wakeupmywall.core.theme.WakeUpMyWallTheme
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcState
import kotlin.test.Test

/**
 * 逐状态断言 Power Rail。计划 Task 4 Step 5 的原始断言写在 Step 4b（环形主按钮）之前，
 * 因此主按钮的断言从"文字等于 Wake PC"改成"可用/不可用 + 独立的标签节点"，
 * 依据是权威规格 A3（环形按钮中央是电源字形，`Power On` 与 `WAKE YOUR PC` 在环下方）。
 */
@OptIn(ExperimentalTestApi::class)
class PowerRailUiTest {

    private val device = PcDevice(
        id = "1",
        name = "My PC",
        macAddress = MacAddress.parse("AA:BB:CC:DD:EE:FF"),
    )

    private fun rail(state: PcState) = powerRailModel(state, device)

    @Test
    fun `wol ready shows the wake affordance`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(rail(PcState.WOL_READY), {}, {}, {}, {}, {}) } }

        onNodeWithTag("powerrail:primary").assertIsEnabled()
        onNodeWithTag("powerrail:primary-label").assertTextEquals("Power On")
        onNodeWithTag("powerrail:primary-caption").assertTextEquals("WAKE YOUR PC")
        onNodeWithTag("powerrail:state").assertTextEquals("Ready to wake")
    }

    @Test
    fun `online disables primary and enables shutdown`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(rail(PcState.ONLINE), {}, {}, {}, {}, {}) } }

        onNodeWithTag("powerrail:primary").assertIsNotEnabled()
        onNodeWithTag("powerrail:sleep").assertIsEnabled()
        onNodeWithTag("powerrail:shutdown").assertIsEnabled()
        onNodeWithTag("powerrail:restart").assertIsEnabled()
    }

    @Test
    fun `waking disables every action`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(rail(PcState.WAKING), {}, {}, {}, {}, {}) } }

        onNodeWithTag("powerrail:primary").assertIsNotEnabled()
        onNodeWithTag("powerrail:sleep").assertIsNotEnabled()
        onNodeWithTag("powerrail:shutdown").assertIsNotEnabled()
        onNodeWithTag("powerrail:restart").assertIsNotEnabled()
    }

    @Test
    fun `connection label follows the state machine terminology`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(rail(PcState.WOL_READY), {}, {}, {}, {}, {}) } }

        onNodeWithTag("powerrail:connection").assertTextEquals("Wake-on-LAN Ready")
        onNodeWithTag("powerrail:connection").assertIsDisplayed()
    }

    @Test
    fun `online connection label never mentions wake on lan`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(rail(PcState.ONLINE), {}, {}, {}, {}, {}) } }

        onNodeWithTag("powerrail:connection").assertTextEquals("Agent connected over LAN")
    }

    @Test
    fun `every rail element is tagged`() = runComposeUiTest {
        setContent { WakeUpMyWallTheme { PowerRail(rail(PcState.WOL_READY), {}, {}, {}, {}, {}) } }

        listOf(
            "powerrail", "powerrail:name", "powerrail:subtitle", "powerrail:state",
            "powerrail:primary", "powerrail:primary-label", "powerrail:primary-caption",
            "powerrail:sleep", "powerrail:shutdown", "powerrail:restart",
            "powerrail:connection", "powerrail:status", "powerrail:settings",
        ).forEach { tag -> onNodeWithTag(tag).assertExists() }
    }
}
