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
 * 设置与设备的持久化边界。Phase 0 只提供内存实现，真实后端（Room / DataStore）
 * 留到 Phase 2 与设备管理一起选型，避免在架构尚未定型时引入未验证的依赖。
 */
interface SettingsStorage {
    suspend fun readDevices(): List<PcDevice>
    suspend fun writeDevices(devices: List<PcDevice>)
    suspend fun readAppearance(): AppearanceSettings
    suspend fun writeAppearance(value: AppearanceSettings)
}
