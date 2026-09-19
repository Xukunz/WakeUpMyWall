package com.xukunz.wakeupmywall.data.settings

import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStorageTest {

    @Test
    fun `default appearance is glass aurora blue`() = runTest {
        val appearance = InMemorySettingsStorage().readAppearance()
        assertEquals("AuroraBlue", appearance.accent)
        assertEquals("Glass", appearance.widgetStyle)
        assertEquals(BuiltInWallpapers.DefaultId, appearance.wallpaperId)
    }

    @Test
    fun `devices round trip keeps mac`() = runTest {
        val storage = InMemorySettingsStorage()
        val device = PcDevice(id = "1", name = "My PC", macAddress = MacAddress.parse("AA:BB:CC:DD:EE:FF"))

        storage.writeDevices(listOf(device))

        assertEquals(listOf(device), storage.readDevices())
    }

    @Test
    fun `empty storage returns empty device list`() = runTest {
        assertEquals(emptyList(), InMemorySettingsStorage().readDevices())
    }
}
