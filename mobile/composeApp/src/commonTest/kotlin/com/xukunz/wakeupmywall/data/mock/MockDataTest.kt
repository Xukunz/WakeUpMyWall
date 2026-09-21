package com.xukunz.wakeupmywall.data.mock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockDataTest {

    @Test
    fun `exactly one device is the default and it is wire ready`() {
        val defaults = MockData.devices.filter { it.isDefault }

        assertEquals(1, defaults.size, "默认设备必须唯一")
        assertNotNull(defaults.single().macAddress, "默认设备必须有 MAC 才能 WOL")
        assertNotNull(defaults.single().agentHost, "默认设备必须有 Agent 主机才能取指标")
    }

    @Test
    fun `metrics stay inside physically plausible bounds`() {
        val metrics = MockData.metrics

        // 模型在 Phase 5B 起是可空的（真实读数可能缺项），但**设计预览的 Mock 必须是完整的**：
        // 任何一项变成 null 都说明 Mock 被人删了字段，视觉基线就不再成立。
        val percentages = listOf(metrics.cpuPercent, metrics.gpuPercent, metrics.ramPercent, metrics.storagePercent)
        val temperatures = listOf(metrics.cpuTempC, metrics.gpuTempC, metrics.motherboardTempC, metrics.ssdTempC)
        val fans = listOf(metrics.cpuFanRpm, metrics.gpuFanRpm, metrics.caseFanRpm)
        assertFalse(percentages.any { it == null }, "Mock 的百分比读数不许缺项")
        assertFalse(temperatures.any { it == null }, "Mock 的温度读数不许缺项")
        assertFalse(fans.any { it == null }, "Mock 的风扇读数不许缺项")

        percentages.filterNotNull()
            .forEach { assertTrue(it in 0f..100f, "百分比越界：$it") }
        temperatures.filterNotNull()
            .forEach { assertTrue(it in 0..120, "温度越界：$it") }
        fans.filterNotNull()
            .forEach { assertTrue(it >= 0, "转速不能为负：$it") }
        assertTrue(metrics.vramUsedGb!! <= metrics.vramTotalGb!!)
        assertTrue(metrics.ramUsedGb!! <= metrics.ramTotalGb!!)
        assertTrue(metrics.storageUsedTb!! <= metrics.storageTotalTb!!)
    }

    @Test
    fun `weather exposes the four hourly slots shown in the concept`() {
        assertEquals(4, MockData.weather.nextHours.size)
    }
}
