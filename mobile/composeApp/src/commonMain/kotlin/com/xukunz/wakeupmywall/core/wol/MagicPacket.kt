package com.xukunz.wakeupmywall.core.wol

import com.xukunz.wakeupmywall.domain.model.MacAddress

/**
 * WOL 魔包（spec §5）：6 字节 `0xFF` 同步头 + 目标 MAC 重复 16 次 = 102 字节。
 * 全项目只有这一个编码入口，避免别处再手写一遍字节序列。
 */
object MagicPacket {
    const val MAC_REPEATS = 16
    const val SIZE = 6 + MAC_REPEATS * 6

    fun encode(mac: MacAddress): ByteArray {
        val packet = ByteArray(SIZE)
        for (index in 0 until 6) packet[index] = 0xFF.toByte()
        val address = mac.bytes
        repeat(MAC_REPEATS) { repeat -> address.copyInto(packet, 6 + repeat * 6) }
        return packet
    }
}
