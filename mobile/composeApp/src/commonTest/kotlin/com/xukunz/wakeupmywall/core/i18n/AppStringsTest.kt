package com.xukunz.wakeupmywall.core.i18n

import com.xukunz.wakeupmywall.domain.model.PcState
import com.xukunz.wakeupmywall.ui.powerrail.powerRailModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 多语言的两条底线：
 *  1. **英文逐字不变**（Phase 1/3 的视觉基线与断言都建立在那些词上）；
 *  2. 中文下领域层产出的状态词也要翻（靠 `localize()` 的"英文词 → 译文"对照表）。
 */
class AppStringsTest {

    @Test
    fun `english keeps the exact domain wording`() {
        val rail = powerRailModel(PcState.ONLINE, device = null, strings = EnglishStrings)

        assertEquals("Online", rail.stateLabel)
        assertEquals("PC Online", rail.primaryLabel)
        assertEquals("POWER CONTROL", rail.subtitle)
        assertEquals("Agent connected over LAN", rail.connectionLabel)
    }

    @Test
    fun `chinese translates the wording that comes from the domain layer`() {
        val rail = powerRailModel(PcState.ONLINE, device = null, strings = ChineseSimplifiedStrings)

        assertEquals("在线", rail.stateLabel)
        assertEquals("电脑在线", rail.primaryLabel)
        assertEquals("电源控制", rail.subtitle)
        assertEquals("已通过局域网连接 Agent", rail.connectionLabel)
    }

    @Test
    fun `an unknown english phrase is left untouched instead of being mangled`() {
        // 表里没有的词原样返回：宁可不翻，也别翻错。
        assertEquals(
            "Some future string",
            ChineseSimplifiedStrings.localize("Some future string"),
        )
        assertTrue(ChineseSimplifiedStrings.localize("Ready to wake").isNotEmpty())
    }
}
