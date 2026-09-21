package com.xukunz.wakeupmywall.core.metrics

/**
 * spec §9 的刷新节奏：ACTIVE 1 秒 / IDLE 5 秒。纯策略——UI 只负责告诉它"现在闲不闲"。
 * 服务端固定 1 Hz 推帧，Idle 时由这边**只接受**每 5 秒一帧（少一次重组 + 少一次绘制）。
 */
data class MetricsCadence(
    val activeMillis: Long = 1_000,
    val idleMillis: Long = 5_000,
) {
    init {
        require(activeMillis > 0) { "activeMillis must be positive, was $activeMillis" }
        require(idleMillis >= activeMillis) { "idle 不该比 active 更频繁：$idleMillis < $activeMillis" }
    }

    fun intervalMillis(isIdle: Boolean): Long = if (isIdle) idleMillis else activeMillis
}

/**
 * "最后一次交互到现在多久"的判定（spec §9 默认 60 秒）。放在这里而不是 UI 里，
 * 是因为 Phase 8 的自动调光 / 像素位移要的是同一份判定。
 *
 * 初值 0 = 还没触摸过，按"刚启动"算：app 起来后的第一个 60 秒内保持 ACTIVE。
 */
class ActivityClock(private val idleTimeoutMillis: Long = 60_000) {

    private var lastInteractionMillis: Long = 0L

    fun touch(nowMillis: Long) {
        lastInteractionMillis = nowMillis
    }

    /** 边界按"到点即 Idle"算：`now - last >= idleTimeoutMillis`。 */
    fun isIdle(nowMillis: Long): Boolean = nowMillis - lastInteractionMillis >= idleTimeoutMillis
}

/**
 * 帧门控：服务端固定 1 Hz 推，这里决定"这一帧要不要喂给 UI"。
 * 纯类（只记上一次接受帧的时刻），所以节奏规则能在 commonTest 里逐条断言，
 * 而不用在 UI 测试里靠 sleep 去赌时序。
 */
class MetricsGate(private val cadence: MetricsCadence) {

    private var lastAcceptedMillis: Long? = null

    fun accept(nowMillis: Long, isIdle: Boolean): Boolean {
        val last = lastAcceptedMillis
        if (last == null || nowMillis - last >= cadence.intervalMillis(isIdle)) {
            lastAcceptedMillis = nowMillis
            return true
        }
        return false
    }
}
