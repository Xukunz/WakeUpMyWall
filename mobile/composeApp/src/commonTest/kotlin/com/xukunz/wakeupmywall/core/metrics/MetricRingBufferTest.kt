package com.xukunz.wakeupmywall.core.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MetricRingBufferTest {

    @Test
    fun `empty buffer reports nulls`() {
        val buffer = MetricRingBuffer(capacity = 3)

        assertEquals(0, buffer.size)
        assertNull(buffer.latest())
        assertNull(buffer.average())
        assertNull(buffer.min())
        assertNull(buffer.max())
    }

    @Test
    fun `keeps insertion order until capacity`() {
        val buffer = MetricRingBuffer(capacity = 3)
        listOf(1f, 2f, 3f).forEach(buffer::add)

        assertEquals(listOf(1f, 2f, 3f), buffer.values())
    }

    @Test
    fun `drops oldest value when full`() {
        val buffer = MetricRingBuffer(capacity = 3)
        listOf(1f, 2f, 3f, 4f).forEach(buffer::add)

        assertEquals(listOf(2f, 3f, 4f), buffer.values())
        assertEquals(3, buffer.size)
    }

    @Test
    fun `computes latest average min and max`() {
        val buffer = MetricRingBuffer(capacity = 60)
        listOf(10f, 20f, 30f).forEach(buffer::add)

        assertEquals(30f, buffer.latest())
        assertEquals(20f, buffer.average())
        assertEquals(10f, buffer.min())
        assertEquals(30f, buffer.max())
    }

    @Test
    fun `sixty samples represent sixty seconds`() {
        val buffer = MetricRingBuffer(capacity = 60)
        repeat(60) { buffer.add(it.toFloat()) }

        assertEquals(60, buffer.size)
        assertEquals(59f, buffer.latest())
    }

    @Test
    fun `single sample is its own average`() {
        val buffer = MetricRingBuffer(capacity = 60)
        buffer.add(42f)

        assertEquals(42f, buffer.average())
    }

    @Test
    fun `capacity must be positive`() {
        assertFailsWith<IllegalArgumentException> { MetricRingBuffer(capacity = 0) }
    }
}
