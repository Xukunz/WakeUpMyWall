package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupInput
import com.xukunz.wakeupmywall.domain.usecase.DeviceSetupValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceSetupValidatorTest {

    private val valid = DeviceSetupInput(
        name = "My PC",
        mac = "AA:BB:CC:DD:EE:FF",
        ip = "192.168.1.10",
        broadcast = "192.168.1.255",
        wolPort = "9",
        agentPort = "9876",
        agentHost = "192.168.1.10",
    )

    @Test
    fun `valid input produces device`() {
        val result = DeviceSetupValidator.validate(valid)

        assertTrue(result.isValid)
        assertEquals("AA:BB:CC:DD:EE:FF", result.device?.macAddress?.normalized)
        assertEquals(9, result.device?.wolPort)
        assertEquals(9876, result.device?.agentPort)
    }

    @Test
    fun `missing name is rejected`() {
        val result = DeviceSetupValidator.validate(valid.copy(name = "  "))

        assertFalse(result.isValid)
        assertEquals("Name is required", result.errors["name"])
    }

    @Test
    fun `invalid mac is rejected`() {
        val result = DeviceSetupValidator.validate(valid.copy(mac = "ZZ:BB:CC:DD:EE:FF"))

        assertEquals("Enter a valid MAC address", result.errors["mac"])
    }

    @Test
    fun `invalid broadcast address is rejected`() {
        val result = DeviceSetupValidator.validate(valid.copy(broadcast = "999.1.1.1"))

        assertEquals("Enter a valid IPv4 address", result.errors["broadcast"])
    }

    @Test
    fun `ip is optional but must be valid when provided`() {
        assertTrue(DeviceSetupValidator.validate(valid.copy(ip = "")).isValid)
        assertEquals(
            "Enter a valid IPv4 address",
            DeviceSetupValidator.validate(valid.copy(ip = "192.168.1")).errors["ip"],
        )
    }

    @Test
    fun `port out of range is rejected`() {
        assertFalse(DeviceSetupValidator.validate(valid.copy(wolPort = "0")).isValid)
        assertFalse(DeviceSetupValidator.validate(valid.copy(agentPort = "70000")).isValid)
        assertFalse(DeviceSetupValidator.validate(valid.copy(agentPort = "abc")).isValid)
    }

    @Test
    fun `agent host is optional but must be valid when provided`() {
        assertTrue(DeviceSetupValidator.validate(valid.copy(agentHost = "")).isValid)
        assertFalse(DeviceSetupValidator.validate(valid.copy(agentHost = "http://bad host")).isValid)
    }

    @Test
    fun `hostname style agent host is accepted`() {
        assertTrue(DeviceSetupValidator.validate(valid.copy(agentHost = "desktop-alpha.local")).isValid)
    }

    @Test
    fun `multiple errors are reported together`() {
        val result = DeviceSetupValidator.validate(valid.copy(name = "", mac = "x", wolPort = "0"))

        assertEquals(setOf("name", "mac", "wolPort"), result.errors.keys)
    }
}
