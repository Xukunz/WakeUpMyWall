package com.xukunz.wakeupmywall.data.settings

import com.xukunz.wakeupmywall.core.storage.AppearanceSettings
import com.xukunz.wakeupmywall.core.storage.SettingsStorage
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySettingsStorage : SettingsStorage {
    private val devicesFlow = MutableStateFlow<List<PcDevice>>(emptyList())
    private val appearanceFlow = MutableStateFlow(AppearanceSettings())
    private var seeded = false

    override suspend fun readDevices(): List<PcDevice> = devicesFlow.value
    override suspend fun writeDevices(devices: List<PcDevice>) { devicesFlow.value = devices }
    override suspend fun readAppearance(): AppearanceSettings = appearanceFlow.value
    override suspend fun writeAppearance(value: AppearanceSettings) { appearanceFlow.value = value }
    override suspend fun isSeeded(): Boolean = seeded
    override suspend fun markSeeded() { seeded = true }
}
