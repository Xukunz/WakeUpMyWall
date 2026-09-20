package com.xukunz.wakeupmywall.data.mock

import com.xukunz.wakeupmywall.domain.model.ActivityEntry
import com.xukunz.wakeupmywall.domain.model.CalendarEvent
import com.xukunz.wakeupmywall.domain.model.HardwareIdentity
import com.xukunz.wakeupmywall.domain.model.HourlyForecast
import com.xukunz.wakeupmywall.domain.model.MacAddress
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.domain.model.NextEvent
import com.xukunz.wakeupmywall.domain.model.PcDevice
import com.xukunz.wakeupmywall.domain.model.PcSummarySnapshot
import com.xukunz.wakeupmywall.domain.model.TodoItem
import com.xukunz.wakeupmywall.domain.model.WeatherSnapshot

/**
 * Phase 1 唯一数据源（Global Constraint：本阶段只用 Mock Data）。
 * 取值对齐 docs/superpowers/plans/2026-09-19-phase1-design-system-and-mock-ui.md 的
 * 「概念图还原规格」——那是本阶段的权威来源，因此这里的数字与概念图一致，
 * 而不是沿用计划正文早期草稿里的近似值。替换真实数据源时只需换掉本对象。
 */
object MockData {

    private const val DEFAULT_DEVICE_ID = "desktop-alpha"

    val devices: List<PcDevice> = listOf(
        PcDevice(
            id = DEFAULT_DEVICE_ID,
            // 权威规格 A1/E：用户在 Device Setup 里给这台机器起的名字是 `My PC`；
            // `DESKTOP-ALPHA` 是主机名，只在 Monitor 身份卡上出现。
            name = "My PC",
            macAddress = MacAddress.parse("00:1A:2B:3C:4D:5E"),
            ipAddress = "192.168.1.10",
            agentHost = "192.168.1.10",
            isDefault = true,
        ),
        PcDevice(
            id = "living-room",
            name = "Living Room PC",
            macAddress = MacAddress.parse("00:1B:44:11:3A:9C"),
        ),
        PcDevice(
            id = "workstation",
            name = "Workstation",
            macAddress = MacAddress.parse("00:25:96:AF:12:BC"),
        ),
        PcDevice(
            id = "media-server",
            name = "Media Server",
            macAddress = MacAddress.parse("00:16:3E:70:9F:21"),
        ),
    )

    val defaultDevice: PcDevice = devices.first { it.isDefault }

    /** PC 摘要卡与身份卡的相对时间文案。接入 Agent 后改为由 `PcDevice.lastSeen` 计算。 */
    val defaultDeviceLastSeenLabel: String = "1 min ago"

    /** 首页文案（权威规格 B 行 1/2）。Phase 6/7 接入真实时钟与日历后由系统时间生成。 */
    val greetingText: String = "Good Evening"
    val clockTime: String = "21:04"
    val calendarDateLabel: String = "Tue, Apr 22"
    /**
     * StandBy 的长日期（权威规格 D 逐字写的是 `Tuesday, April 22`）。
     * 日历卡走的是短形态 `Tue, Apr 22`（规格 B2），两者不能共用一个字符串。
     */
    val standbyDateLabel: String = "Tuesday, April 22"

    val hardware = HardwareIdentity(
        hostname = "DESKTOP-ALPHA",
        os = "Windows 11 Pro",
        cpuName = "AMD Ryzen 7 7700X",
        gpuName = "NVIDIA GeForce RTX 4070 Ti",
        ramModule = "32 GB DDR5",
        storageModule = "2 TB NVMe SSD",
        // 卡片短名与概念图逐字一致（权威规格 C2 的括号内文案）。
        cpuShortName = "Ryzen 7 7700X",
        gpuShortName = "RTX 4070 Ti",
    )

    val weather = WeatherSnapshot(
        location = "Riverside, CA",
        temperatureC = 18,
        condition = "Partly Cloudy",
        highC = 22,
        lowC = 14,
        nextHours = listOf(
            HourlyForecast("10PM", 17),
            HourlyForecast("1AM", 16),
            HourlyForecast("4AM", 15),
            HourlyForecast("7AM", 16),
        ),
        summary = "Clearer skies later tonight.",
    )

    /** StandBy 的下一场日程（权威规格 D）。 */
    val nextEvent = NextEvent(
        countdownLabel = "In 1 hr 19 min",
        title = "Team sync",
        timeRange = "11:00 PM – 12:00 AM",
        source = "Microsoft Teams",
    )

    val calendarEvents: List<CalendarEvent> = listOf(
        CalendarEvent(time = "10:00", title = "Team sync"),
        CalendarEvent(time = "1:00", title = "Lunch break"),
        CalendarEvent(time = "4:00", title = "Plan next week"),
    )

    val todos: List<TodoItem> = listOf(
        TodoItem(id = "1", title = "Finish project notes", done = true),
        TodoItem(id = "2", title = "Order desk accessories", done = true),
        TodoItem(id = "3", title = "Reply to Alex"),
        TodoItem(id = "4", title = "Plan weekend trip"),
        TodoItem(id = "5", title = "Read 1 chapter"),
    )

    /** 首页摘要卡读数（权威规格 B3）。 */
    val summaryMetrics = PcSummarySnapshot(
        cpuPercent = 12,
        cpuTempC = 42,
        ramPercent = 38,
        downloadMbps = 12.4f,
        uploadMbps = 3.1f,
        lastSeenLabel = defaultDeviceLastSeenLabel,
    )

    val metrics = MetricsSnapshot(
        cpuPercent = 28f,
        cpuClockGhz = 4.9f,
        cpuCores = 8,
        cpuThreads = 16,
        cpuTempC = 68,
        gpuPercent = 62f,
        gpuTempC = 67,
        vramUsedGb = 8.1f,
        vramTotalGb = 12,
        ramPercent = 38f,
        ramUsedGb = 12.1f,
        ramTotalGb = 32,
        storagePercent = 54f,
        storageUsedTb = 1.1f,
        storageTotalTb = 2.0f,
        storageFreeGb = 554,
        motherboardTempC = 42,
        ssdTempC = 38,
        cpuFanRpm = 1240,
        gpuFanRpm = 1560,
        caseFanRpm = 820,
        downloadMbps = 124.3f,
        uploadMbps = 31.7f,
        uptimeSeconds = 3 * 24 * 3600L + 6 * 3600L + 24 * 60L,
        bootDateLabel = "Since Apr 18, 2025",
        recentActivity = listOf(
            ActivityEntry(app = "Microsoft Edge", whenLabel = "5 min ago"),
            ActivityEntry(app = "Steam", whenLabel = "12 min ago"),
            ActivityEntry(app = "Visual Studio Code", whenLabel = "28 min ago"),
            ActivityEntry(app = "Spotify", whenLabel = "1 hr ago"),
        ),
    )
}
