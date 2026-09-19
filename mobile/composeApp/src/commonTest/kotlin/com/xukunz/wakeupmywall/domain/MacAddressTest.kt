package com.xukunz.wakeupmywall.domain

import com.xukunz.wakeupmywall.domain.model.MacAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MacAddressTest {

    @Test
    fun `parses colon separated mac and normalizes to uppercase`() {
        val mac = assertNotNull(MacAddress.parse("aa:bb:cc:dd:ee:ff"))
        assertEquals("AA:BB:CC:DD:EE:FF", mac.normalized)
    }

    @Test
    fun `parses dash separated mac`() {
        assertEquals("AA:BB:CC:DD:EE:FF", MacAddress.parse("AA-BB-CC-DD-EE-FF")?.normalized)
    }

    @Test
    fun `rejects wrong segment count`() {
        assertNull(MacAddress.parse("AA:BB:CC:DD:EE"))
    }

    @Test
    fun `rejects non hex characters`() {
        assertNull(MacAddress.parse("ZZ:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `exposes six bytes`() {
        assertEquals(
            listOf(0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF).map { it.toByte() },
            MacAddress.parse("AA:BB:CC:DD:EE:FF")?.bytes?.toList(),
        )
    }
}
