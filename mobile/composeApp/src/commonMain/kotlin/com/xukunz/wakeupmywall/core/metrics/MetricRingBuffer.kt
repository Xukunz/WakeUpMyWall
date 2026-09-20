package com.xukunz.wakeupmywall.core.metrics

/**
 * 指标环形缓冲（Phase 1 全局约束里的"60 秒曲线"数据源）。
 * 默认容量 60 对应 1 Hz 采样下的 60 秒窗口；Phase 5 换成流式通道时本类不变。
 */
class MetricRingBuffer(val capacity: Int = 60) {

    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    private val storage = ArrayDeque<Float>(capacity)

    val size: Int get() = storage.size

    fun add(value: Float) {
        if (storage.size == capacity) storage.removeFirst()
        storage.addLast(value)
    }

    fun values(): List<Float> = storage.toList()

    fun latest(): Float? = storage.lastOrNull()

    fun average(): Float? = if (storage.isEmpty()) null else storage.sum() / storage.size

    fun min(): Float? = storage.minOrNull()

    fun max(): Float? = storage.maxOrNull()
}
