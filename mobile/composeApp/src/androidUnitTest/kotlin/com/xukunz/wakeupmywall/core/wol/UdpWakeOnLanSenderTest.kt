package com.xukunz.wakeupmywall.core.wol

import com.xukunz.wakeupmywall.domain.model.MacAddress
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UdpWakeOnLanSenderTest {

    private val packet = MagicPacket.encode(MacAddress.parse("00:1A:2B:3C:4D:5E")!!)

    @Test
    fun `the packet arrives byte for byte on a real socket`() = runTest {
        DatagramSocket(0, InetAddress.getByName("127.0.0.1")).use { listener ->
            listener.soTimeout = 3_000
            val result = UdpWakeOnLanSender(attempts = 1).send(packet, "127.0.0.1", listener.localPort)

            assertEquals(WakeSendResult.Sent(1), result)

            val buffer = ByteArray(256)
            val received = DatagramPacket(buffer, buffer.size)
            listener.receive(received)
            assertContentEquals(packet, received.data.copyOfRange(0, received.length))
            assertEquals(102, received.length)
        }
    }

    @Test
    fun `three attempts really send three datagrams`() = runTest {
        DatagramSocket(0, InetAddress.getByName("127.0.0.1")).use { listener ->
            listener.soTimeout = 3_000
            val result = UdpWakeOnLanSender(attempts = 3, gapMillis = 10).send(packet, "127.0.0.1", listener.localPort)

            assertEquals(WakeSendResult.Sent(3), result)

            val buffer = ByteArray(256)
            repeat(3) { index ->
                val received = DatagramPacket(buffer, buffer.size)
                listener.receive(received)
                assertEquals(102, received.length, "packet #${index + 1} should be 102 bytes")
            }
        }
    }

    @Test
    fun `failure reasons are typed, not guessed from message text`() {
        assertEquals("Cannot resolve \"nope.invalid\"", describeSendFailure(UnknownHostException("nope.invalid")))
        assertTrue(describeSendFailure(SocketException("Network is unreachable")).contains("Network is unreachable"))
    }
}
