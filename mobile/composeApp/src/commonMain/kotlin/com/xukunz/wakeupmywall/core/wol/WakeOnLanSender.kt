package com.xukunz.wakeupmywall.core.wol

sealed interface WakeSendResult {
    data class Sent(val packets: Int) : WakeSendResult
    data class Failed(val reason: String) : WakeSendResult
}

interface WakeOnLanSender {
    suspend fun send(packet: ByteArray, target: String, port: Int): WakeSendResult
}

/** 桌面预览与纯 UI 测试的默认实现：如实说这条平台路径不可用，不假装发送成功。 */
object UnsupportedWakeOnLanSender : WakeOnLanSender {
    override suspend fun send(packet: ByteArray, target: String, port: Int) =
        WakeSendResult.Failed("Wake-on-LAN is not available on this platform")
}
