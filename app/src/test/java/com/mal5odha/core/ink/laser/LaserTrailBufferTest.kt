package com.mal5odha.core.ink.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaserTrailBufferTest {

    @Test
    fun testEmptyBuffer() {
        val buffer = LaserTrailBuffer(capacity = 16)
        assertEquals(0, buffer.size)
        assertTrue(buffer.isEmpty())
        assertFalse(buffer.isNotEmpty())
    }

    @Test
    fun testPushAndRetrievePoints() {
        val buffer = LaserTrailBuffer(capacity = 16)
        buffer.push(10f, 20f, 1000L, 0.8f)
        buffer.push(30f, 40f, 1050L, 1.0f)

        assertEquals(2, buffer.size)
        assertFalse(buffer.isEmpty())
        assertTrue(buffer.isNotEmpty())

        assertEquals(10f, buffer.getX(0), 0.001f)
        assertEquals(20f, buffer.getY(0), 0.001f)
        assertEquals(1000L, buffer.getTimestamp(0))
        assertEquals(0.8f, buffer.getPressure(0), 0.001f)

        assertEquals(30f, buffer.getX(1), 0.001f)
        assertEquals(40f, buffer.getY(1), 0.001f)
        assertEquals(1050L, buffer.getTimestamp(1))
        assertEquals(1.0f, buffer.getPressure(1), 0.001f)
    }

    @Test
    fun testPruneExpiredRollingWindow() {
        val buffer = LaserTrailBuffer(capacity = 16)
        // Push 3 points at different timestamps
        buffer.push(10f, 10f, 1000L)
        buffer.push(20f, 20f, 1200L)
        buffer.push(30f, 30f, 1600L)

        assertEquals(3, buffer.size)

        // At now = 2000L with tau = 700ms:
        // Cutoff = 2000 - 700 = 1300L.
        // Points at 1000L (age 1000ms) and 1200L (age 800ms) are expired.
        // Point at 1600L (age 400ms) is active.
        buffer.pruneExpired(now = 2000L, ttlMs = 700L)

        assertEquals(1, buffer.size)
        assertEquals(30f, buffer.getX(0), 0.001f)
        assertEquals(1600L, buffer.getTimestamp(0))

        // At now = 2400L with tau = 700ms:
        // Cutoff = 1700L. Point at 1600L is now expired.
        buffer.pruneExpired(now = 2400L, ttlMs = 700L)
        assertEquals(0, buffer.size)
        assertTrue(buffer.isEmpty())
    }

    @Test
    fun testRingBufferWrapAroundCapacity() {
        val capacity = 4
        val buffer = LaserTrailBuffer(capacity = capacity)

        // Push 6 points into capacity-4 buffer
        for (i in 1..6) {
            buffer.push(i * 10f, i * 20f, i * 100L)
        }

        // Buffer should be saturated at capacity 4
        assertEquals(4, buffer.size)

        // Oldest two (i=1, i=2) should have been overwritten; active should be 3, 4, 5, 6
        assertEquals(30f, buffer.getX(0), 0.001f)
        assertEquals(300L, buffer.getTimestamp(0))

        assertEquals(60f, buffer.getX(3), 0.001f)
        assertEquals(600L, buffer.getTimestamp(3))
    }

    @Test
    fun testCopyToScratchArrays() {
        val buffer = LaserTrailBuffer(capacity = 8)
        buffer.push(15f, 25f, 500L, 0.5f)
        buffer.push(35f, 45f, 600L, 0.9f)

        val outXs = FloatArray(8)
        val outYs = FloatArray(8)
        val outTimestamps = LongArray(8)
        val outPressures = FloatArray(8)

        val count = buffer.copyTo(outXs, outYs, outTimestamps, outPressures)
        assertEquals(2, count)

        assertEquals(15f, outXs[0], 0.001f)
        assertEquals(25f, outYs[0], 0.001f)
        assertEquals(500L, outTimestamps[0])
        assertEquals(0.5f, outPressures[0], 0.001f)

        assertEquals(35f, outXs[1], 0.001f)
        assertEquals(45f, outYs[1], 0.001f)
        assertEquals(600L, outTimestamps[1])
        assertEquals(0.9f, outPressures[1], 0.001f)
    }

    @Test
    fun testGetLatestPoint() {
        val buffer = LaserTrailBuffer(capacity = 8)
        val out = FloatArray(2)

        assertFalse(buffer.getLatestPoint(out))

        buffer.push(123f, 456f, 1000L)
        assertTrue(buffer.getLatestPoint(out))
        assertEquals(123f, out[0], 0.001f)
        assertEquals(456f, out[1], 0.001f)

        buffer.push(789f, 999f, 1050L)
        assertTrue(buffer.getLatestPoint(out))
        assertEquals(789f, out[0], 0.001f)
        assertEquals(999f, out[1], 0.001f)
    }

    @Test
    fun testClear() {
        val buffer = LaserTrailBuffer(capacity = 8)
        buffer.push(10f, 20f, 100L)
        buffer.push(30f, 40f, 200L)
        assertEquals(2, buffer.size)

        buffer.clear()
        assertEquals(0, buffer.size)
        assertTrue(buffer.isEmpty())
    }

    @Test
    fun testLaserConfigDefaults() {
        val config = LaserConfig()
        assertEquals(LaserMode.TRAIL, config.mode)
        assertEquals(0xFFFF1744L, config.colorHex)
        assertEquals(1400L, config.durationMs)
    }
}
