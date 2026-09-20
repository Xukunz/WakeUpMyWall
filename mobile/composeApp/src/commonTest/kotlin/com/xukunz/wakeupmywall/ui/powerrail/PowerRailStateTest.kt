package com.xukunz.wakeupmywall.ui.powerrail

import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PowerRailStateTest {

    private val device = PcDevice(
        id = "1",
        name = "My PC",
        macAddress = MacAddress.parse("AA:BB:CC:DD:EE:FF"),
    )

    @Test
    fun `wol ready shows the concept label and caption`() {
        val model = powerRailModel(PcState.WOL_READY, device)

        // 权威规格 A3：环形主按钮下方是 `Power On` + `WAKE YOUR PC`
        assertEquals("Power On", model.primaryLabel)
        assertEquals("WAKE YOUR PC", model.primaryCaption)
        assertTrue(model.primaryEnabled)
        assertEquals("Ready to wake", model.stateLabel)
        assertEquals("Wake-on-LAN Ready", model.connectionLabel)
    }

    @Test
    fun `offline without mac disables wake`() {
        val model = powerRailModel(PcState.OFFLINE, device.copy(macAddress = null))

        assertFalse(model.primaryEnabled)
    }

    @Test
    fun `online enables secondary actions only`() {
        val model = powerRailModel(PcState.ONLINE, device)

        assertFalse(model.primaryEnabled, "在线时主按钮不应可点")
        assertTrue(model.canSleep && model.canShutdown && model.canRestart)
        assertEquals("Online", model.stateLabel)
        assertEquals("Agent connected over LAN", model.connectionLabel)
    }

    @Test
    fun `terminal states disable every action`() {
        listOf(PcState.WAKING, PcState.SLEEPING, PcState.RESTARTING, PcState.SHUTTING_DOWN).forEach { state ->
            val model = powerRailModel(state, device)
            assertFalse(model.primaryEnabled, "$state primary")
            assertFalse(model.canSleep || model.canShutdown || model.canRestart, "$state secondary")
        }
    }

    @Test
    fun `unconfigured shows setup pc`() {
        val model = powerRailModel(PcState.UNCONFIGURED, null)

        assertEquals("Setup PC", model.primaryLabel)
        assertEquals("My PC", model.pcName)
    }

    @Test
    fun `status line carries capability detail while connection label carries the channel`() {
        // 稳定状态下两者文案一致（都来自 Phase 0 的能力描述），
        // 过渡状态才是它们分工的地方：通道仍是 WOL，能力描述变成"等待 Agent"。
        val waking = powerRailModel(PcState.WAKING, device)
        assertEquals("Wake-on-LAN Ready", waking.connectionLabel)
        assertEquals("Waiting for Agent", waking.statusLine)
    }

    @Test
    fun `connection label never claims wake on lan while online`() {
        PcState.entries.forEach { state ->
            val label = powerRailModel(state, device).connectionLabel
            if (state == PcState.ONLINE) {
                assertFalse(label.contains("Wake-on-LAN"), "$state: $label")
            }
        }
    }
}
