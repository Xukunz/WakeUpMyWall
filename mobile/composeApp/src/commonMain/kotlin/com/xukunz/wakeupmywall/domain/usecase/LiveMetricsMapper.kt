package com.xukunz.wakeupmywall.domain.usecase

import com.xukunz.wakeupmywall.core.network.AgentMetrics
import com.xukunz.wakeupmywall.domain.model.HardwareIdentity
import com.xukunz.wakeupmywall.domain.model.DiskSnapshot
import com.xukunz.wakeupmywall.domain.model.LiveMetrics
import com.xukunz.wakeupmywall.domain.model.MetricsSnapshot
import com.xukunz.wakeupmywall.domain.model.PcSummarySnapshot
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Agent 载荷 → UI 领域快照的**纯函数**。规则只有三条：
 *  1. 可空字段原样保留：Agent 没读到的传感器在 UI 上是 `—`，绝不在这里补 0；
 *  2. 显示精度在这里定死（百分比/温度取整，GHz/GB/TB/Mbps 留 1 位小数），UI 不再做四舍五入；
 *  3. 时间戳换成 "HH:mm:ss" / "Since Apr 18, 2025" 这类展示串，解析失败时退回原始字符串（不丢信息）。
 *
 * 这样 Monitor / Dashboard 拿到的就是"可以直接画"的模型，而所有换算都能在 commonTest 里断言。
 */
object LiveMetricsMapper {

    private val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun map(metrics: AgentMetrics, timeZone: TimeZone = TimeZone.currentSystemDefault()): LiveMetrics {
        val capturedAtLabel = formatTime(metrics.capturedAtUtc, timeZone) ?: metrics.capturedAtUtc

        val snapshot = MetricsSnapshot(
            cpuPercent = round1(metrics.cpu.usagePercent),
            cpuClockGhz = round1(metrics.cpu.clockGhz),
            cpuCores = metrics.cpu.cores,
            cpuThreads = metrics.cpu.threads,
            cpuTempC = metrics.cpu.tempC?.roundToInt(),
            gpuPercent = round1(metrics.gpu.usagePercent),
            gpuTempC = metrics.gpu.tempC?.roundToInt(),
            vramUsedGb = round1(metrics.gpu.vramUsedGb),
            vramTotalGb = metrics.gpu.vramTotalGb?.roundToInt(),
            ramPercent = round1(metrics.memory.usagePercent),
            ramUsedGb = round1(metrics.memory.usedGb),
            ramTotalGb = metrics.memory.totalGb?.roundToInt(),
            storagePercent = round1(metrics.storage.usagePercent),
            storageUsedTb = round1(metrics.storage.usedTb),
            storageTotalTb = round1(metrics.storage.totalTb),
            storageFreeGb = metrics.storage.freeGb?.roundToInt(),
            motherboardTempC = metrics.thermal.motherboardTempC?.roundToInt(),
            // SSD 温度来自存储硬件的温度传感器（Agent 侧就是这么映射的）。
            ssdTempC = metrics.storage.tempC?.roundToInt(),
            cpuFanRpm = metrics.cpu.fanRpm,
            gpuFanRpm = metrics.gpu.fanRpm,
            caseFanRpm = metrics.thermal.caseFanRpm,
            downloadMbps = round1(metrics.network.downloadMbps),
            uploadMbps = round1(metrics.network.uploadMbps),
            uptimeSeconds = metrics.uptimeSeconds,
            bootDateLabel = bootDateLabel(metrics.bootedAtUtc, timeZone),
            // spec §8：最近应用是 P2，真实数据接入前保持空列表（UI 显示 —）。
            recentActivity = emptyList(),
            disks = mapDisks(metrics),
        )

        val identity = HardwareIdentity(
            hostname = metrics.identity.hostname,
            os = metrics.identity.os,
            cpuName = metrics.identity.cpuName,
            gpuName = metrics.identity.gpuName,
            ramModule = metrics.identity.ramModule,
            storageModule = metrics.identity.storageModule,
            cpuShortName = metrics.identity.cpuShortName,
            gpuShortName = metrics.identity.gpuShortName,
        )

        // 摘要卡是同一批读数的一个子集：只取四项 + 采集时刻，避免两处各算一遍。
        val summary = PcSummarySnapshot(
            cpuPercent = snapshot.cpuPercent?.roundToInt(),
            cpuTempC = snapshot.cpuTempC,
            ramPercent = snapshot.ramPercent?.roundToInt(),
            downloadMbps = snapshot.downloadMbps,
            uploadMbps = snapshot.uploadMbps,
            lastSeenLabel = capturedAtLabel,
        )

        return LiveMetrics(
            snapshot = snapshot,
            identity = identity,
            summary = summary,
            capturedAtLabel = capturedAtLabel,
        )
    }

    /** 1 位小数：Agent 已经四舍五入过，这里再收一次防止 `33.599998` 这种浮点尾巴进 UI。 */
    private fun round1(value: Float?): Float? = value?.let { (it * 10f).roundToInt() / 10f }

    /**
     * 磁盘列表：优先用 Agent 报的多盘清单（载荷**顶层**的 `disks`，与 `SystemMetricsPayload.Disks` 对齐）；
     * 老 Agent（0.4.0 之前）没有这个字段，就用系统盘那几个字段合成一条，UI 不会因此空掉。
     */
    private fun mapDisks(metrics: AgentMetrics): List<DiskSnapshot> {
        if (metrics.disks.isNotEmpty()) {
            return metrics.disks.map { disk ->
                DiskSnapshot(
                    name = disk.name,
                    mount = disk.mount,
                    usagePercent = round1(disk.usagePercent),
                    usedGb = round1(disk.usedGb),
                    totalGb = round1(disk.totalGb),
                    freeGb = round1(disk.freeGb),
                    tempC = disk.tempC?.roundToInt(),
                )
            }
        }

        val storage = metrics.storage
        if (storage.totalTb == null && storage.freeGb == null && storage.usagePercent == null) return emptyList()
        return listOf(
            DiskSnapshot(
                name = metrics.identity.storageModule,
                mount = "C:\\",
                usagePercent = round1(storage.usagePercent),
                usedGb = round1(storage.usedTb?.times(1024f)),
                totalGb = round1(storage.totalTb?.times(1024f)),
                freeGb = round1(storage.freeGb),
                tempC = storage.tempC?.roundToInt(),
            )
        )
    }

    private fun formatTime(iso: String, timeZone: TimeZone): String? = runCatching {
        val local = Instant.parse(iso).toLocalDateTime(timeZone)
        "${pad(local.hour)}:${pad(local.minute)}:${pad(local.second)}"
    }.getOrNull()

    private fun bootDateLabel(iso: String, timeZone: TimeZone): String? = runCatching {
        val date = Instant.parse(iso).toLocalDateTime(timeZone).date
        "Since ${monthNames[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"
    }.getOrNull()

    private fun pad(value: Int): String = if (value < 10) "0$value" else "$value"
}
