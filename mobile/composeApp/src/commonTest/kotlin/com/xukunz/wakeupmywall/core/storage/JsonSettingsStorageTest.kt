package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonSettingsStorageTest {

    private val desktop = PcDevice(
        id = "my-pc",
        name = "My PC",
        macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
        broadcastAddress = "192.168.1.255",
        wolPort = 9,
        isDefault = true,
    )

    @Test
    fun `devices survive a round trip`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        storage.writeDevices(listOf(desktop, desktop.copy(id = "study", name = "Study PC", isDefault = false)))

        val loaded = storage.readDevices()

        assertEquals(listOf("my-pc", "study"), loaded.map { it.id })
        assertEquals(MacAddress.parse("00:1A:2B:3C:4D:5E"), loaded.first().macAddress)
        assertEquals(true, loaded.first().isDefault)
    }

    @Test
    fun `missing keys fall back to defaults`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())

        assertEquals(emptyList(), storage.readDevices())
        assertEquals(AppearanceSettings(), storage.readAppearance())
    }

    @Test
    fun `corrupt json does not crash the app`() = runTest {
        val store = InMemoryKeyValueStore(mapOf("devices" to "{ this is not json"))
        val storage = JsonSettingsStorage(store)

        assertEquals(emptyList(), storage.readDevices())
    }

    @Test
    fun `unknown fields from a newer version are ignored`() = runTest {
        val raw =
            """[{"id":"my-pc","name":"My PC","broadcastAddress":"192.168.1.255","wolPort":9,"agentPort":9876,"isDefault":true,"sceneId":"den"}]"""
        val storage = JsonSettingsStorage(InMemoryKeyValueStore(mapOf("devices" to raw)))

        val loaded = storage.readDevices()

        assertEquals(1, loaded.size)
        assertEquals("My PC", loaded.single().name)
    }

    @Test
    fun `appearance round trip keeps accent, style and wallpaper`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val value = AppearanceSettings(accent = "AuroraBlue", widgetStyle = "Solid", wallpaperId = "minimal")

        storage.writeAppearance(value)

        assertEquals(value, storage.readAppearance())
        assertTrue(storage.readDevices().isEmpty())
    }

    @Test
    fun `the seeded flag starts false and sticks once written`() = runTest {
        val store = InMemoryKeyValueStore()
        val storage = JsonSettingsStorage(store)

        assertEquals(false, storage.isSeeded())

        storage.markSeeded()

        assertEquals(true, storage.isSeeded())
        // 冷启动等价于"用同一个 store 重新建一个实例"：
        assertEquals(true, JsonSettingsStorage(store).isSeeded())
    }
}
