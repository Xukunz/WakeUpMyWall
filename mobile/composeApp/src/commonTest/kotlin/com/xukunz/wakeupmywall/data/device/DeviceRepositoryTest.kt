package com.xukunz.wakeupmywall.data.device

import com.xukunz.wakeupmywall.core.storage.InMemoryKeyValueStore
import com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceRepositoryTest {

    private fun device(id: String, name: String = id, isDefault: Boolean = false) =
        PcDevice(id = id, name = name, isDefault = isDefault)

    private fun repo(storage: JsonSettingsStorage, seed: List<PcDevice> = emptyList()) =
        DeviceRepository(storage, seed)

    @Test
    fun `seeding happens once and never overwrites user data`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val seed = listOf(device("my-pc", "My PC", isDefault = true))

        val first = repo(storage, seed).apply { load() }
        assertEquals(listOf("my-pc"), first.devices.value.map { it.id })

        // 用户删掉种子设备后再冷启动：不能又被种回来。
        first.delete("my-pc")
        val second = repo(storage, seed).apply { load() }
        assertEquals(emptyList(), second.devices.value)
        assertNull(second.active)
    }

    @Test
    fun `add persists and the first device becomes default`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }

        repository.add(device("study", "Study PC"))

        assertEquals(true, repository.active?.isDefault)
        assertEquals("study", DeviceRepository(storage).apply { load() }.active?.id)
    }

    @Test
    fun `set default moves the badge and keeps exactly one default`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("a", "A", isDefault = true))
        repository.add(device("b", "B"))

        repository.setDefault("b")

        assertEquals(listOf(false, true), repository.devices.value.map { it.isDefault })
        assertEquals("b", repository.active?.id)
    }

    @Test
    fun `deleting the default promotes the first remaining device`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("a", "A", isDefault = true))
        repository.add(device("b", "B"))

        repository.delete("a")

        assertEquals(listOf("b"), repository.devices.value.map { it.id })
        assertEquals(true, repository.devices.value.single().isDefault)
    }

    @Test
    fun `update writes the edited fields back`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("a", "A", isDefault = true))

        repository.update("a", repository.devices.value.single().copy(name = "Desk PC", wolPort = 7))

        val reloaded = DeviceRepository(storage).apply { load() }.devices.value.single()
        assertEquals("Desk PC", reloaded.name)
        assertEquals(7, reloaded.wolPort)
    }

    @Test
    fun `an id collision gets a suffix instead of overwriting`() = runTest {
        val storage = JsonSettingsStorage(InMemoryKeyValueStore())
        val repository = repo(storage).apply { load() }
        repository.add(device("home", "Home"))

        repository.add(device("home", "Home again"))

        assertEquals(listOf("home", "home-2"), repository.devices.value.map { it.id })
    }
}
