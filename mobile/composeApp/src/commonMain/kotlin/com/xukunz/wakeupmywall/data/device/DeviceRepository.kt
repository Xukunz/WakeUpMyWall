package com.xukunz.wakeupmywall.data.device

import com.xukunz.wakeupmywall.core.storage.SettingsStorage
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设备列表的唯一状态源。不变量：列表非空时**恰好一台** `isDefault = true`，[active] 就是它；
 * 列表为空时 `active = null`（Phase 3 会为这个分支补空状态 UI）。
 *
 * [seed] 只在存储里从来没有写过设备时使用一次：设备系统的真实数据要等用户自己添加，
 * 但 Phase 2 的界面还没有"一台设备都没有"的形态，所以先播一次 Mock 设备作为过渡。
 */
class DeviceRepository(
    private val storage: SettingsStorage,
    private val seed: List<PcDevice> = emptyList(),
) {
    private val state = MutableStateFlow<List<PcDevice>>(emptyList())
    val devices: StateFlow<List<PcDevice>> = state.asStateFlow()

    val active: PcDevice?
        get() = state.value.firstOrNull { it.isDefault } ?: state.value.firstOrNull()

    suspend fun load() {
        val stored = storage.readDevices()
        if (!storage.isSeeded()) {
            // 首次运行：写入种子并打标记；之后即使用户把设备全删光，也不会被重新种回来。
            state.value = normalise(stored.ifEmpty { seed })
            storage.writeDevices(state.value)
            storage.markSeeded()
            return
        }
        state.value = normalise(stored)
    }

    suspend fun add(device: PcDevice) {
        state.value = normalise(state.value + device.copy(id = uniqueId(device.id, state.value)))
        persist()
    }

    suspend fun update(id: String, device: PcDevice) {
        state.value = normalise(state.value.map { if (it.id == id) device.copy(id = id) else it })
        persist()
    }

    suspend fun delete(id: String) {
        state.value = normalise(state.value.filterNot { it.id == id })
        persist()
    }

    suspend fun setDefault(id: String) {
        state.value = normalise(state.value.map { it.copy(isDefault = it.id == id) })
        persist()
    }

    private suspend fun persist() {
        storage.writeDevices(state.value)
    }

    /** 列表非空时保证恰好一台默认设备（没有标记时取第一台）。 */
    private fun normalise(devices: List<PcDevice>): List<PcDevice> {
        if (devices.isEmpty()) return devices
        val defaultIndex = devices.indexOfFirst { it.isDefault }.takeIf { it >= 0 } ?: 0
        return devices.mapIndexed { index, device -> device.copy(isDefault = index == defaultIndex) }
    }

    private fun uniqueId(candidate: String, existing: List<PcDevice>): String {
        if (existing.none { it.id == candidate }) return candidate
        var suffix = 2
        while (existing.any { it.id == "$candidate-$suffix" }) suffix++
        return "$candidate-$suffix"
    }
}
