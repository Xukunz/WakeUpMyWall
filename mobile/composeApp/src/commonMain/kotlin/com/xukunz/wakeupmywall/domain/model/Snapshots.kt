package com.xukunz.wakeupmywall.domain.model

/**
 * 只读快照模型：字段按「概念图还原规格」列出屏幕要显示的信息量，
 * 真实数据源（Weather Provider / Android Calendar / Agent）按对应 Phase 替换 Mock。所有取值都不可变。
 */
/**
 * 逐时预报。`condition` / `isNight` 是图标选型要用的数据，必须由数据源给出，
 * 不允许从已经格式化好的 `label`（如 `10PM`）反推——那会把展示串当数据用。
 * Phase 6 接真实 Weather Provider 后由 provider 直接填这两个字段。
 */
data class HourlyForecast(
    val label: String,
    val temperatureC: Int,
    val condition: String = "Cloudy",
    val isNight: Boolean = false,
)

data class WeatherSnapshot(
    val location: String,
    val temperatureC: Int,
    val condition: String,
    val highC: Int,
    val lowC: Int,
    val nextHours: List<HourlyForecast>,
    /** StandBy 天气卡上的一句话天气（权威规格 D）。 */
    val summary: String = "",
    /** 当前是夜间：决定图标用日间版还是夜间版（概念图 21:04 用的是月亮+云）。 */
    val isNight: Boolean = false,
)

/** StandBy 的下一场日程（权威规格 D：In 1 hr 19 min / Team sync / 11:00 PM – 12:00 AM / Microsoft Teams）。 */
data class NextEvent(
    val countdownLabel: String,
    val title: String,
    val timeRange: String,
    val source: String,
)

data class CalendarEvent(val time: String, val title: String)

data class TodoItem(val id: String, val title: String, val done: Boolean = false)

data class ActivityEntry(val app: String, val whenLabel: String)

/**
 * 首页 PC 摘要卡的读数。权威规格 B3 给的是低负载读数（CPU 12% / Temp 42°C / RAM 38% /
 * ↓12.4 ↑3.1 Mbps），与 Monitor 页的实时读数（C2）不是同一组数字，因此单独建模。
 */
data class PcSummarySnapshot(
    val cpuPercent: Int,
    val cpuTempC: Int,
    val ramPercent: Int,
    val downloadMbps: Float,
    val uploadMbps: Float,
    val lastSeenLabel: String,
)

/** 静态硬件身份（型号小字）。实时数值一律在 [MetricsSnapshot]，同一事实只存一处。 */
data class HardwareIdentity(
    val hostname: String,
    val os: String,
    val cpuName: String,
    val gpuName: String,
    val ramModule: String,
    val storageModule: String,
    /**
     * 指标卡上的型号小字（权威规格 C2）。概念图里卡片只用短名（`Ryzen 7 7700X` / `RTX 4070 Ti`），
     * 带厂商前缀的全名只出现在身份卡上——卡片宽度装不下全名，实测会折成两行把环形进度挤变形。
     */
    val cpuShortName: String,
    val gpuShortName: String,
)

data class MetricsSnapshot(
    val cpuPercent: Float,
    val cpuClockGhz: Float,
    val cpuCores: Int,
    val cpuThreads: Int,
    val cpuTempC: Int,
    val gpuPercent: Float,
    val gpuTempC: Int,
    val vramUsedGb: Float,
    val vramTotalGb: Int,
    val ramPercent: Float,
    val ramUsedGb: Float,
    val ramTotalGb: Int,
    val storagePercent: Float,
    val storageUsedTb: Float,
    val storageTotalTb: Float,
    val storageFreeGb: Int,
    val motherboardTempC: Int,
    val ssdTempC: Int,
    val cpuFanRpm: Int,
    val gpuFanRpm: Int,
    val caseFanRpm: Int,
    val downloadMbps: Float,
    val uploadMbps: Float,
    val uptimeSeconds: Long,
    val bootDateLabel: String,
    val recentActivity: List<ActivityEntry>,
)
