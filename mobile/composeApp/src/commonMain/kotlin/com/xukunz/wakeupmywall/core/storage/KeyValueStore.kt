package com.xukunz.wakeupmywall.core.storage

/**
 * 平台键值存储的最小边界：只有字符串读写，序列化与领域规则都留在 commonMain。
 * Android 侧由 DataStore Preferences 适配，测试与桌面预览用内存实现。
 */
interface KeyValueStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
}

class InMemoryKeyValueStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {
    private val values = initial.toMutableMap()

    override suspend fun read(key: String): String? = values[key]

    override suspend fun write(key: String, value: String) {
        values[key] = value
    }
}
