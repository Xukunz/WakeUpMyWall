package com.xukunz.wakeupmywall.core.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetricsCadenceTest {

    @Test
    fun `active screens refresh every second and idle ones every five`() {
        val cadence = MetricsCadence()

        assertEquals(1_000, cadence.intervalMillis(isIdle = false))
        assertEquals(5_000, cadence.intervalMillis(isIdle = true))
    }

    @Test
    fun `an idle interval slower than the active one is rejected at construction`() {
        val failure = runCatching { MetricsCadence(activeMillis = 5_000, idleMillis = 1_000) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException, "配置错了就该在构造时就炸，而不是悄悄跑")
    }

    @Test
    fun `the activity clock flips to idle exactly at the timeout`() {
        val clock = ActivityClock(idleTimeoutMillis = 60_000)

        clock.touch(nowMillis = 10_000)
        assertFalse(clock.isIdle(nowMillis = 69_999))
        assertTrue(clock.isIdle(nowMillis = 70_000), "刚好 60 秒算 Idle（到点即闲）")
        assertTrue(clock.isIdle(nowMillis = 75_000))
    }

    @Test
    fun `a fresh launch counts as active until the timeout passes`() {
        val clock = ActivityClock(idleTimeoutMillis = 60_000)

        assertFalse(clock.isIdle(nowMillis = 0))
        assertFalse(clock.isIdle(nowMillis = 59_999))
        assertTrue(clock.isIdle(nowMillis = 60_000))
    }

    @Test
    fun `touching again makes the screen active immediately`() {
        val clock = ActivityClock(idleTimeoutMillis = 60_000)
        clock.touch(nowMillis = 0)
        assertTrue(clock.isIdle(nowMillis = 120_000))

        clock.touch(nowMillis = 120_000)
        assertFalse(clock.isIdle(nowMillis = 120_001))
    }

    @Test
    fun `the gate accepts the first frame and then one per interval`() {
        val gate = MetricsGate(MetricsCadence(activeMillis = 1_000, idleMillis = 5_000))

        assertTrue(gate.accept(nowMillis = 0, isIdle = false), "第一帧永远接受")
        assertFalse(gate.accept(nowMillis = 400, isIdle = false), "还没到 1 秒：丢掉")
        assertTrue(gate.accept(nowMillis = 1_000, isIdle = false))
    }

    @Test
    fun `the gate slows down to the idle interval while the screen is idle`() {
        val gate = MetricsGate(MetricsCadence(activeMillis = 1_000, idleMillis = 5_000))

        assertTrue(gate.accept(nowMillis = 0, isIdle = true))
        assertFalse(gate.accept(nowMillis = 4_999, isIdle = true), "Idle 下 1 Hz 的帧要被丢掉")
        assertTrue(gate.accept(nowMillis = 5_000, isIdle = true))

        // 用户一摸屏幕就恢复 1 秒的节奏（而不是等到下一个 5 秒窗口）。
        assertFalse(gate.accept(nowMillis = 5_500, isIdle = false), "刚接受过的帧不重复喂")
        assertTrue(gate.accept(nowMillis = 6_000, isIdle = false), "回到 ACTIVE 后按 1 秒一帧")
    }
}
