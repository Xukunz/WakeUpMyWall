package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * [SettingsStorage] 的 JSON 实现。读取路径一律"失败即默认值"：
 * 缺 key、JSON 损坏、字段被新版本改过，都不能让 App 起不来。
 * 写入只发生在用户显式操作或首次播种时，不会用默认值覆盖用户数据。
 */
class JsonSettingsStorage(private val store: KeyValueStore) : SettingsStorage {

    override suspend fun readDevices(): List<PcDevice> = decode(DevicesKey, emptyList())

    override suspend fun writeDevices(devices: List<PcDevice>) {
        store.write(DevicesKey, json.encodeToString(devices))
    }

    override suspend fun readAppearance(): AppearanceSettings = decode(AppearanceKey, AppearanceSettings())

    override suspend fun writeAppearance(value: AppearanceSettings) {
        store.write(AppearanceKey, json.encodeToString(value))
    }

    override suspend fun isSeeded(): Boolean = store.read(SeededKey) == "true"

    override suspend fun markSeeded() {
        store.write(SeededKey, "true")
    }

    private suspend inline fun <reified T> decode(key: String, fallback: T): T {
        val raw = store.read(key) ?: return fallback
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: SerializationException) {
            fallback
        } catch (e: IllegalArgumentException) {
            fallback
        }
    }

    private companion object {
        const val DevicesKey = "devices"
        const val AppearanceKey = "appearance"
        const val SeededKey = "seeded"

        // ignoreUnknownKeys：旧版本读到新版本写下的字段时要能降级，而不是当成坏数据清空。
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
