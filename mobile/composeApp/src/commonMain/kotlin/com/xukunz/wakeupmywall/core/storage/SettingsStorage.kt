package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.core.wallpaper.BuiltInWallpapers
import com.xukunz.wakeupmywall.domain.model.PcDevice
import kotlinx.serialization.Serializable

@Serializable
data class AppearanceSettings(
    val accent: String = "AuroraBlue",
    val widgetStyle: String = "Glass",
    val wallpaperId: String = BuiltInWallpapers.DefaultId,
)

/**
 * 设置与设备的持久化边界。Phase 0 只有内存实现；Phase 2 定型为
 * [KeyValueStore]（平台键值）→ [JsonSettingsStorage]（JSON + 坏数据兜底）这一条链，
 * Android 侧由 DataStore Preferences 提供键值存储。
 */
interface SettingsStorage {
    suspend fun readDevices(): List<PcDevice>
    suspend fun writeDevices(devices: List<PcDevice>)
    suspend fun readAppearance(): AppearanceSettings
    suspend fun writeAppearance(value: AppearanceSettings)

    /**
     * 设备列表是否已经写过一次。用于"首次运行播种 Mock 设备"这个过渡手段：
     * 用户把设备全删光之后，下次冷启动不能再被种回来。
     */
    suspend fun isSeeded(): Boolean
    suspend fun markSeeded()
}
