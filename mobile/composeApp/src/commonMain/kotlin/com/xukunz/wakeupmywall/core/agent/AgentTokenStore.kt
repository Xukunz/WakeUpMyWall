package com.xukunz.wakeupmywall.core.agent

/**
 * 按设备保存 Agent 配对 Token 的边界。
 *
 * spec 明确要求 Token **不进 `PcDevice` 的序列化路径**：所以它不在这里的领域模型里，
 * 而是由这个接口单独管。Android 实现把 Token 用 Keystore 的 AES-GCM 加密后再落盘
 * （见 `KeystoreAgentTokenStore`），测试与桌面预览用内存实现。
 */
interface AgentTokenStore {
    suspend fun read(deviceId: String): String?
    suspend fun write(deviceId: String, token: String)
    suspend fun clear(deviceId: String)
}

class InMemoryAgentTokenStore(initial: Map<String, String> = emptyMap()) : AgentTokenStore {
    private val tokens = initial.toMutableMap()

    override suspend fun read(deviceId: String): String? = tokens[deviceId]

    override suspend fun write(deviceId: String, token: String) {
        tokens[deviceId] = token
    }

    override suspend fun clear(deviceId: String) {
        tokens.remove(deviceId)
    }
}
