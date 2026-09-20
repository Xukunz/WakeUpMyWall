package com.xukunz.wakeupmywall.core.wol

import com.xukunz.wakeupmywall.domain.model.MacAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MagicPacketTest {

    private val mac = MacAddress.parse("00:1A:2B:3C:4D:5E")!!

    @Test
    fun `packet is six ff bytes followed by the mac sixteen times`() {
        val packet = MagicPacket.encode(mac)

        assertEquals(102, packet.size)
        assertContentEquals(ByteArray(6) { 0xFF.toByte() }, packet.copyOfRange(0, 6))
        repeat(MagicPacket.MAC_REPEATS) { repeat ->
            val start = 6 + repeat * 6
            assertContentEquals(
                byteArrayOf(0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E),
                packet.copyOfRange(start, start + 6),
            )
        }
    }

    @Test
    fun `dash separated and lowercase input encodes identically`() {
        val dashed = MacAddress.parse("00-1a-2b-3c-4d-5e")!!

        assertContentEquals(MagicPacket.encode(mac), MagicPacket.encode(dashed))
    }

    @Test
    fun `the tail of the packet is the mac, not padding`() {
        val packet = MagicPacket.encode(mac)

        assertContentEquals(
            byteArrayOf(0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E),
            packet.copyOfRange(packet.size - 6, packet.size),
        )
    }
}
