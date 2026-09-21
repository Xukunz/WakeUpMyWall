package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.MonitorCardDetail
import com.xukunz.wakeupmywall.domain.model.MonitorCardId
import com.xukunz.wakeupmywall.domain.model.MonitorLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonitorLayoutTest {

    @Test
    fun `the default layout matches the shipped order`() {
        assertEquals(
            listOf(
                MonitorCardId.Cpu, MonitorCardId.Gpu, MonitorCardId.Ram, MonitorCardId.Storage,
                MonitorCardId.Temps, MonitorCardId.Network, MonitorCardId.Uptime,
                MonitorCardId.Activity, MonitorCardId.Quote,
            ),
            MonitorLayout.Default.map { it.id },
        )
        assertTrue(MonitorLayout.Default.all { it.enabled }, "默认全部显示")
    }

    @Test
    fun `moving stops at both ends`() {
        val first = MonitorLayout.Default.first().id
        val last = MonitorLayout.Default.last().id

        assertEquals(MonitorLayout.Default, MonitorLayout.move(MonitorLayout.Default, first, -1))
        assertEquals(MonitorLayout.Default, MonitorLayout.move(MonitorLayout.Default, last, +1))

        val moved = MonitorLayout.move(MonitorLayout.Default, MonitorCardId.Storage, -3)
        assertEquals(MonitorCardId.Storage, moved.first().id)
    }

    @Test
    fun `the last visible card cannot be hidden`() {
        var cards = MonitorLayout.Default
        MonitorCardId.entries.forEach { id -> cards = MonitorLayout.toggle(cards, id) }

        assertEquals(1, MonitorLayout.visible(cards).size, "至少要留一张卡")
    }

    @Test
    fun `toggling hides a single card`() {
        val cards = MonitorLayout.toggle(MonitorLayout.Default, MonitorCardId.Cpu)

        assertFalse(cards.first { it.id == MonitorCardId.Cpu }.enabled)
        assertFalse(MonitorLayout.visible(cards).any { it.id == MonitorCardId.Cpu })
    }

    @Test
    fun `detail only accepts the values that card supports`() {
        val accepted = MonitorLayout.setDetail(MonitorLayout.Default, MonitorCardId.Cpu, MonitorCardDetail.Cores)
        assertEquals(MonitorCardDetail.Cores, accepted.first { it.id == MonitorCardId.Cpu }.detail)

        // 风扇/网络这类整卡没有"信息类别"：给了也不认，保持原值。
        val rejected = MonitorLayout.setDetail(MonitorLayout.Default, MonitorCardId.Network, MonitorCardDetail.Cores)
        assertEquals(MonitorLayout.Default, rejected)
    }

    @Test
    fun `cycling walks through the supported details and wraps`() {
        var cards = MonitorLayout.setDetail(MonitorLayout.Default, MonitorCardId.Cpu, MonitorCardDetail.Clock)

        cards = MonitorLayout.cycleDetail(cards, MonitorCardId.Cpu)
        assertEquals(MonitorCardDetail.Cores, cards.first { it.id == MonitorCardId.Cpu }.detail)
        cards = MonitorLayout.cycleDetail(cards, MonitorCardId.Cpu)
        assertEquals(MonitorCardDetail.Temp, cards.first { it.id == MonitorCardId.Cpu }.detail)
        cards = MonitorLayout.cycleDetail(cards, MonitorCardId.Cpu)
        assertEquals(MonitorCardDetail.Clock, cards.first { it.id == MonitorCardId.Cpu }.detail, "循环回到第一项")
    }
}
