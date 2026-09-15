package com.mal5odha.core.pdf.viewport

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoFingerIntentGestureDetectorTest {

    @Test
    fun `initial state is UNDECIDED and vertical delta is 0f`() {
        val detector = TwoFingerIntentGestureDetector(touchSlop = 20f, zoomSlop = 20f)
        val initialCenter = Offset(500f, 500f)
        val initialSpan = 200f

        detector.onPointersDown(initialCenter, initialSpan)
        assertEquals(TwoFingerGestureMode.UNDECIDED, detector.mode)

        val step = detector.onPointersMove(
            currentCentroid = Offset(500f, 510f),
            currentSpan = 205f
        )
        assertEquals(TwoFingerGestureMode.UNDECIDED, step.mode)
        assertEquals(0.0f, step.verticalScrollDelta, 0.0001f)
    }

    @Test
    fun `SCROLL_LOCKED mode inverts vertical scroll delta for natural scrolling polarity`() {
        val detector = TwoFingerIntentGestureDetector(
            touchSlop = 20f,
            zoomSlop = 20f,
            invertScrollDirection = true
        )
        val initialCenter = Offset(500f, 500f)
        val initialSpan = 200f

        detector.onPointersDown(initialCenter, initialSpan)

        // Drag fingers downward by 35px (dy = +35px)
        val downStep = detector.onPointersMove(
            currentCentroid = Offset(500f, 535f),
            currentSpan = 202f
        )
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, downStep.mode)
        // With inverted scroll direction, downward finger displacement (+35) inverts to -35
        assertEquals(-35.0f, downStep.verticalScrollDelta, 0.0001f)

        // Continue dragging downward by 20px
        val continueDownStep = detector.onPointersMove(
            currentCentroid = Offset(500f, 555f),
            currentSpan = 202f
        )
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, continueDownStep.mode)
        assertEquals(-20.0f, continueDownStep.verticalScrollDelta, 0.0001f)

        // Drag fingers upward by 25px (dy = -25px)
        val upStep = detector.onPointersMove(
            currentCentroid = Offset(500f, 530f),
            currentSpan = 202f
        )
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, upStep.mode)
        // Inverted: -(-25) = +25
        assertEquals(25.0f, upStep.verticalScrollDelta, 0.0001f)
    }

    @Test
    fun `ZOOM_LOCKED mode produces verticalScrollDelta == 0f`() {
        val detector = TwoFingerIntentGestureDetector(touchSlop = 20f, zoomSlop = 20f)
        val initialCenter = Offset(500f, 500f)
        val initialSpan = 200f

        detector.onPointersDown(initialCenter, initialSpan)

        // Pinch spread by 40px (exceeds zoomSlop = 20f)
        val zoomStep = detector.onPointersMove(
            currentCentroid = Offset(500f, 505f),
            currentSpan = 240f
        )
        assertEquals(TwoFingerGestureMode.ZOOM_LOCKED, zoomStep.mode)
        assertEquals(0.0f, zoomStep.verticalScrollDelta, 0.0001f)

        // Subsequent vertical hand movement while in ZOOM_LOCKED must produce 0 vertical delta
        val verticalMoveInZoom = detector.onPointersMove(
            currentCentroid = Offset(500f, 600f),
            currentSpan = 250f
        )
        assertEquals(TwoFingerGestureMode.ZOOM_LOCKED, verticalMoveInZoom.mode)
        assertEquals(0.0f, verticalMoveInZoom.verticalScrollDelta, 0.0001f)
    }

    @Test
    fun `pointers up cleanly resets detector back to UNDECIDED`() {
        val detector = TwoFingerIntentGestureDetector(touchSlop = 20f, zoomSlop = 20f)
        detector.onPointersDown(Offset(500f, 500f), 200f)
        detector.onPointersMove(Offset(500f, 540f), 200f)
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, detector.mode)

        detector.onPointersUp()
        assertEquals(TwoFingerGestureMode.UNDECIDED, detector.mode)
        assertTrue(detector.isInitialState())
    }

    @Test
    fun `invertScrollDirection false preserves raw delta direction when configured`() {
        val detector = TwoFingerIntentGestureDetector(
            touchSlop = 20f,
            zoomSlop = 20f,
            invertScrollDirection = false
        )
        detector.onPointersDown(Offset(500f, 500f), 200f)

        val step = detector.onPointersMove(
            currentCentroid = Offset(500f, 530f),
            currentSpan = 200f
        )
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, step.mode)
        assertEquals(30.0f, step.verticalScrollDelta, 0.0001f)
    }
}
