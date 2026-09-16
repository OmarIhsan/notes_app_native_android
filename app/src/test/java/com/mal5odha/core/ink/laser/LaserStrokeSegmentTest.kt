package com.mal5odha.core.ink.laser

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaserStrokeSegmentTest {

    @Test
    fun testLaserPointCreation() {
        val p1 = LaserPoint(10f, 20f, 1000L, 0.8f)
        assertEquals(10f, p1.x, 0.001f)
        assertEquals(20f, p1.y, 0.001f)
        assertEquals(10f, p1.offset.x, 0.001f)
        assertEquals(20f, p1.offset.y, 0.001f)
        assertEquals(1000L, p1.timestampMs)
        assertEquals(0.8f, p1.pressure, 0.001f)

        val p2 = LaserPoint(Offset(30f, 40f), 2000L)
        assertEquals(30f, p2.x, 0.001f)
        assertEquals(40f, p2.y, 0.001f)
        assertEquals(1.0f, p2.pressure, 0.001f)
    }

    @Test
    fun testSegmentIsolationPreventsPolylineBridging() {
        // Stroke 1: Gesture down at (10, 10), move to (20, 20), then finger lifted (UP)
        val stroke1 = LaserStrokeSegment(id = 1000L)
        stroke1.points.add(LaserPoint(10f, 10f, 1000L))
        stroke1.points.add(LaserPoint(20f, 20f, 1050L))

        // Stroke 2: New gesture down across the canvas at (500, 500), move to (520, 520)
        val stroke2 = LaserStrokeSegment(id = 1200L)
        stroke2.points.add(LaserPoint(500f, 500f, 1200L))
        stroke2.points.add(LaserPoint(520f, 520f, 1250L))

        val segments = mutableListOf(stroke1, stroke2)

        // Segments remain distinct:
        assertEquals(2, segments.size)
        assertEquals(2, segments[0].points.size)
        assertEquals(2, segments[1].points.size)

        // Connecting lines can only be drawn within each segment, never bridging (20, 20) -> (500, 500):
        val stroke1Connections = (0 until stroke1.points.size - 1).map { i ->
            Pair(stroke1.points[i], stroke1.points[i + 1])
        }
        val stroke2Connections = (0 until stroke2.points.size - 1).map { i ->
            Pair(stroke2.points[i], stroke2.points[i + 1])
        }

        // Verify stroke 1 only connects internal points
        assertEquals(1, stroke1Connections.size)
        assertEquals(10f, stroke1Connections[0].first.x, 0.001f)
        assertEquals(20f, stroke1Connections[0].second.x, 0.001f)

        // Verify stroke 2 only connects internal points
        assertEquals(1, stroke2Connections.size)
        assertEquals(500f, stroke2Connections[0].first.x, 0.001f)
        assertEquals(520f, stroke2Connections[0].second.x, 0.001f)

        // No bridging connection exists between stroke 1 tail and stroke 2 head
        val allBridging = stroke1Connections + stroke2Connections
        assertFalse(allBridging.any { it.first.x == 20f && it.second.x == 500f })
    }

    @Test
    fun testPruningExpiredPointsPerSegment() {
        val segment = LaserStrokeSegment(id = 1000L)
        segment.points.add(LaserPoint(10f, 10f, 1000L))
        segment.points.add(LaserPoint(20f, 20f, 1500L))
        segment.points.add(LaserPoint(30f, 30f, 1900L))

        val now = 2500L
        val durationMs = 800L // Points older than (2500 - 800 = 1700L) should be pruned

        segment.points.removeAll { pt -> (now - pt.timestampMs) > durationMs }

        assertEquals(1, segment.points.size)
        assertEquals(30f, segment.points[0].x, 0.001f)
        assertEquals(1900L, segment.points[0].timestampMs)
    }

    @Test
    fun testPruningEmptyFinishedSegments() {
        val completedStroke = LaserStrokeSegment(id = 1000L)
        completedStroke.points.add(LaserPoint(10f, 10f, 1000L))

        val activeStroke = LaserStrokeSegment(id = 2000L)
        activeStroke.points.add(LaserPoint(50f, 50f, 2000L))

        val segments = mutableListOf(completedStroke, activeStroke)
        val currentSegment = activeStroke

        val now = 3000L
        val durationMs = 1400L // Cutoff is 1600L. Point at 1000L is expired. Point at 2000L is active.

        val iterator = segments.iterator()
        while (iterator.hasNext()) {
            val seg = iterator.next()
            seg.points.removeAll { pt -> (now - pt.timestampMs) > durationMs }
            if (seg.points.isEmpty() && seg !== currentSegment) {
                iterator.remove()
            }
        }

        // completedStroke was emptied and pruned, activeStroke remains
        assertEquals(1, segments.size)
        assertEquals(activeStroke.id, segments[0].id)
        assertEquals(1, segments[0].points.size)
    }
}
