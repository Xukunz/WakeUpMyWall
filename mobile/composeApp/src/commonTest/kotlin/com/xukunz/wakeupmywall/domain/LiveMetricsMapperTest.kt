package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.core.network.AgentCpuMetrics
import com.xukunz.wakeupmywall.core.network.AgentGpuMetrics
import com.xukunz.wakeupmywall.core.network.AgentIdentity
import com.xukunz.wakeupmywall.core.network.AgentMemoryMetrics
import com.xukunz.wakeupmywall.core.network.AgentMetrics
import com.xukunz.wakeupmywall.core.network.AgentNetworkMetrics
import com.xukunz.wakeupmywall.core.network.AgentStorageMetrics
import com.xukunz.wakeupmywall.core.network.AgentThermalMetrics
import com.xukunz.wakeupmywall.domain.usecase.LiveMetricsMapper
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveMetricsMapperTest {

    private val identity = AgentIdentity(
        hostname = "DESKTOP-ALPHA",
        os = "Windows 11 Pro",
        cpuName = "AMD Ryzen 7 7700X 8-Core Processor",
        cpuShortName = "Ryzen 7 7700X",
        gpuName = "NVIDIA GeForce RTX 4070 Ti",
        gpuShortName = "RTX 4070 Ti",
        ramModule = "32 GB DDR5-6000",
        storageModule = "NVMe 2 TB",
    )

    private val complete = AgentMetrics(
        capturedAtUtc = "2026-09-21T00:58:02Z",
        identity = identity,
        cpu = AgentCpuMetrics(
            name = identity.cpuName,
            usagePercent = 12.55f,
            clockGhz = 4.34f,
            cores = 8,
            threads = 16,
            tempC = 42.4f,
            fanRpm = 980,
        ),
        gpu = AgentGpuMetrics(
            name = identity.gpuName,
            usagePercent = 8.04f,
            tempC = 38.6f,
            vramUsedGb = 2.44f,
            vramTotalGb = 12f,
            fanRpm = 1200,
        ),
        memory = AgentMemoryMetrics(usagePercent = 38.04f, usedGb = 12.2f, totalGb = 32f),
        storage = AgentStorageMetrics(
            usagePercent = 95.02f,
            usedTb = 1.94f,
            totalTb = 2f,
            freeGb = 102f,
            tempC = 41.2f,
        ),
        thermal = AgentThermalMetrics(motherboardTempC = 35.4f, caseFanRpm = 870),
        network = AgentNetworkMetrics(downloadMbps = 12.44f, uploadMbps = 3.14f),
        uptimeSeconds = 289_440,
        bootedAtUtc = "2025-04-18T12:00:00Z",
    )

    @Test
    fun `agent readings land in the monitor snapshot with one decimal of display precision`() {
        val live = LiveMetricsMapper.map(complete, TimeZone.UTC)

        assertEquals(12.6f, live.snapshot.cpuPercent)
        assertEquals(4.3f, live.snapshot.cpuClockGhz)
        assertEquals(8, live.snapshot.cpuCores)
        assertEquals(16, live.snapshot.cpuThreads)
        assertEquals(42, live.snapshot.cpuTempC)
        assertEquals(980, live.snapshot.cpuFanRpm)
        assertEquals(8f, live.snapshot.gpuPercent)
        assertEquals(39, live.snapshot.gpuTempC)
        assertEquals(2.4f, live.snapshot.vramUsedGb)
        assertEquals(12, live.snapshot.vramTotalGb)
        assertEquals(38f, live.snapshot.ramPercent)
        assertEquals(12.2f, live.snapshot.ramUsedGb)
        assertEquals(32, live.snapshot.ramTotalGb)
        assertEquals(95f, live.snapshot.storagePercent)
        assertEquals(1.9f, live.snapshot.storageUsedTb)
        assertEquals(2f, live.snapshot.storageTotalTb)
        assertEquals(102, live.snapshot.storageFreeGb)
        assertEquals(35, live.snapshot.motherboardTempC)
        assertEquals(41, live.snapshot.ssdTempC)
        assertEquals(870, live.snapshot.caseFanRpm)
        assertEquals(12.4f, live.snapshot.downloadMbps)
        assertEquals(3.1f, live.snapshot.uploadMbps)
        assertEquals(289_440L, live.snapshot.uptimeSeconds)
        // spec §8：最近应用是 P2，映射阶段就是空列表（UI 显示 —）。
        assertEquals(emptyList(), live.snapshot.recentActivity)
    }

    @Test
    fun `missing sensors stay null instead of turning into zero`() {
        val sparse = complete.copy(
            cpu = AgentCpuMetrics(name = "CPU", threads = 16),
            gpu = AgentGpuMetrics(name = "GPU"),
            memory = AgentMemoryMetrics(),
            storage = AgentStorageMetrics(),
            thermal = AgentThermalMetrics(),
            network = AgentNetworkMetrics(),
        )

        val live = LiveMetricsMapper.map(sparse, TimeZone.UTC)

        assertNull(live.snapshot.cpuPercent)
        assertNull(live.snapshot.cpuClockGhz)
        assertNull(live.snapshot.cpuTempC)
        assertNull(live.snapshot.cpuFanRpm)
        assertNull(live.snapshot.gpuPercent)
        assertNull(live.snapshot.ramPercent)
        assertNull(live.snapshot.storageTempC())
        assertNull(live.snapshot.motherboardTempC)
        assertNull(live.snapshot.downloadMbps)
        assertNull(live.summary.cpuPercent)
        assertNull(live.summary.cpuTempC)
        assertNull(live.summary.downloadMbps)
    }

    @Test
    fun `identity carries the short names the cards need`() {
        val live = LiveMetricsMapper.map(complete, TimeZone.UTC)

        assertEquals("DESKTOP-ALPHA", live.identity.hostname)
        assertEquals("Ryzen 7 7700X", live.identity.cpuShortName)
        assertEquals("RTX 4070 Ti", live.identity.gpuShortName)
        assertEquals("32 GB DDR5-6000", live.identity.ramModule)
    }

    @Test
    fun `captured and boot labels come from the agent timestamps`() {
        val live = LiveMetricsMapper.map(complete, TimeZone.UTC)

        assertEquals("00:58:02", live.capturedAtLabel)
        assertEquals("Since Apr 18, 2025", live.snapshot.bootDateLabel)
        // 摘要卡的"上次可见"就是采样时刻：两个数字必须来自同一个时间戳。
        assertEquals(live.capturedAtLabel, live.summary.lastSeenLabel)
    }

    @Test
    fun `an unparsable timestamp keeps the raw string instead of dropping it`() {
        val live = LiveMetricsMapper.map(complete.copy(capturedAtUtc = "not-a-time"), TimeZone.UTC)

        assertEquals("not-a-time", live.capturedAtLabel)
    }

    private fun com.xukunz.wakeupmywall.domain.model.MetricsSnapshot.storageTempC(): Int? = ssdTempC
}
