package com.xukunz.wakeupmywall.core.wol

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * UDP 广播发送魔包。重试 3 次（spec §5），每次之间留一点间隔，避免被同一条网卡的队列
 * 合并丢弃。失败原因按异常**类型**分类，不去猜 message 文案。
 */
class UdpWakeOnLanSender(
    private val attempts: Int = 3,
    private val gapMillis: Long = 150,
    /**
     * 发送日志。默认不记录：`android.util.Log` 在 JVM 单元测试里是 stub（调用会抛
     * "not mocked"），所以日志由生产接线（`MainActivity`）注入，测试用默认的空实现。
     */
    private val logger: (String) -> Unit = {},
) : WakeOnLanSender {

    override suspend fun send(packet: ByteArray, target: String, port: Int): WakeSendResult =
        withContext(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val address = InetAddress.getByName(target)
                    val datagram = DatagramPacket(packet, packet.size, address, port)
                    var sent = 0
                    repeat(attempts) { index ->
                        socket.send(datagram)
                        sent++
                        if (index < attempts - 1) delay(gapMillis)
                    }
                    // 真机验收（Task 5）靠这行 logcat 证明"包真的发出去了"。
                    logger("sent ${packet.size} bytes x$sent to $target:$port")
                    WakeSendResult.Sent(sent)
                }
            } catch (e: IOException) {
                WakeSendResult.Failed(describeSendFailure(e))
            }
        }

}

/** 发送失败的原因文案：单独提出来才能用合成异常单测。 */
internal fun describeSendFailure(error: IOException): String = when (error) {
    is UnknownHostException -> "Cannot resolve \"${error.message}\""
    is SocketException -> "Socket error: ${error.message ?: "broadcast rejected"}"
    else -> error.message ?: error::class.simpleName ?: "send failed"
}
