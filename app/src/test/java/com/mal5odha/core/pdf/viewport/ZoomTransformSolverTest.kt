package com.mal5odha.core.pdf.viewport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomTransformSolverTest {

    private val viewportWidth = 1000f

    @Test
    fun `scale cannot be reduced below 1_0f regardless of inward pinch magnitude`() {
        val initialState = ZoomTransformSolver.ZoomState(scale = 1.0f, panX = 0f)

        val resultSmallPinch = ZoomTransformSolver.calculateZoomStep(
            previousState = initialState,
            zoomChange = 0.8f,
            panDeltaX = 0f,
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(1.0f, resultSmallPinch.zoomState.scale, 0.0001f)

        val resultExtremePinch = ZoomTransformSolver.calculateZoomStep(
            previousState = initialState,
            zoomChange = 0.01f,
            panDeltaX = 0f,
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(1.0f, resultExtremePinch.zoomState.scale, 0.0001f)
    }

    @Test
    fun `at scale 1_0f panX is always identically 0_0f`() {
        val initialState = ZoomTransformSolver.ZoomState(scale = 1.0f, panX = 0f)

        // Attempt horizontal pan while at scale 1.0
        val resultPanRight = ZoomTransformSolver.calculateZoomStep(
            previousState = initialState,
            zoomChange = 1.0f,
            panDeltaX = 150f,
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(0.0f, resultPanRight.zoomState.panX, 0.0001f)

        val resultPanLeft = ZoomTransformSolver.calculateZoomStep(
            previousState = initialState,
            zoomChange = 1.0f,
            panDeltaX = -150f,
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(0.0f, resultPanLeft.zoomState.panX, 0.0001f)
    }

    @Test
    fun `maximum zoom stops cleanly at 5_0f`() {
        val highZoomState = ZoomTransformSolver.ZoomState(scale = 4.5f, panX = -500f)

        val resultOverZoom = ZoomTransformSolver.calculateZoomStep(
            previousState = highZoomState,
            zoomChange = 2.0f,
            panDeltaX = 0f,
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(5.0f, resultOverZoom.zoomState.scale, 0.0001f)
    }

    @Test
    fun `horizontal pan clamps strictly between negative width times scale minus one and zero`() {
        // At scale 2.0, viewportWidth = 1000f, valid pan range is [-1000f * (2 - 1), 0] = [-1000f, 0f]
        val stateAt2x = ZoomTransformSolver.ZoomState(scale = 2.0f, panX = -500f)

        // Test over-panning to the right (positive direction)
        val overPanRight = ZoomTransformSolver.calculateZoomStep(
            previousState = stateAt2x,
            zoomChange = 1.0f,
            panDeltaX = 800f, // would reach +300f without clamp
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(0.0f, overPanRight.zoomState.panX, 0.0001f)

        // Test over-panning to the left (negative direction)
        val overPanLeft = ZoomTransformSolver.calculateZoomStep(
            previousState = stateAt2x,
            zoomChange = 1.0f,
            panDeltaX = -800f, // would reach -1300f without clamp
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(-1000f, overPanLeft.zoomState.panX, 0.0001f)

        // Test valid pan within bounds
        val validPan = ZoomTransformSolver.calculateZoomStep(
            previousState = stateAt2x,
            zoomChange = 1.0f,
            panDeltaX = 100f,
            panDeltaY = 0f,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )
        assertEquals(-400f, validPan.zoomState.panX, 0.0001f)
    }

    @Test
    fun `vertical pan delta is completely separated from viewport translation with Ty invariant 0`() {
        val state = ZoomTransformSolver.ZoomState(scale = 2.0f, panX = -200f)
        val deltaY = 75.5f

        val result = ZoomTransformSolver.calculateZoomStep(
            previousState = state,
            zoomChange = 1.0f,
            panDeltaX = 0f,
            panDeltaY = deltaY,
            focalPointX = 500f,
            viewportWidth = viewportWidth
        )

        // Viewport ZoomState contains only scale and panX; container Ty is strictly omitted/0
        assertEquals(state.scale, result.zoomState.scale, 0.0001f)
        assertEquals(deltaY, result.verticalScrollDelta, 0.0001f)
    }

    @Test
    fun `pinch to zoom preserves focal anchor point relative to document coordinates`() {
        val focalX = 400f
        val initialState = ZoomTransformSolver.ZoomState(scale = 1.5f, panX = -300f)
        // Normalized document coordinate under focal anchor:
        // docX = (focalX - panX) / scale = (400 - (-300)) / 1.5 = 700 / 1.5 = 466.667
        val docXBefore = (focalX - initialState.panX) / initialState.scale

        val result = ZoomTransformSolver.calculateZoomStep(
            previousState = initialState,
            zoomChange = 1.5f, // scale increases to 2.25f
            panDeltaX = 0f,
            panDeltaY = 0f,
            focalPointX = focalX,
            viewportWidth = viewportWidth
        )

        val docXAfter = (focalX - result.zoomState.panX) / result.zoomState.scale
        assertEquals(docXBefore, docXAfter, 0.001f)
    }

    @Test
    fun `arbitrator begins in UNDECIDED and suppresses all deltas within touch slop window`() {
        val arbitrator = TwoFingerGestureArbitrator(touchSlop = 20f, zoomSlop = 20f)
        val initialCenter = androidx.compose.ui.geometry.Offset(500f, 500f)
        val initialSpan = 200f

        arbitrator.onPointersDown(initialCenter, initialSpan)
        assertEquals(TwoFingerGestureMode.UNDECIDED, arbitrator.mode)

        // Micro movement within slop window (deltaY = 10f < 20f, deltaSpan = 5f < 20f)
        val step = arbitrator.onPointersMove(
            currentCentroid = androidx.compose.ui.geometry.Offset(500f, 510f),
            currentSpan = 205f
        )
        assertEquals(TwoFingerGestureMode.UNDECIDED, step.mode)
        assertEquals(1.0f, step.zoomChange, 0.0001f)
        assertEquals(0.0f, step.panDeltaX, 0.0001f)
        assertEquals(0.0f, step.verticalScrollDelta, 0.0001f)
    }

    @Test
    fun `vertical displacement exceeding touch slop strictly locks mode to SCROLL_LOCKED`() {
        val arbitrator = TwoFingerGestureArbitrator(touchSlop = 20f, zoomSlop = 20f)
        val initialCenter = androidx.compose.ui.geometry.Offset(500f, 500f)
        val initialSpan = 200f

        arbitrator.onPointersDown(initialCenter, initialSpan)

        // Exceed vertical touch slop (deltaY = 30f >= 20f)
        val lockStep = arbitrator.onPointersMove(
            currentCentroid = androidx.compose.ui.geometry.Offset(500f, 530f),
            currentSpan = 202f
        )
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, lockStep.mode)
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, arbitrator.mode)
        assertEquals(30.0f, lockStep.verticalScrollDelta, 0.0001f)
        assertEquals(1.0f, lockStep.zoomChange, 0.0001f)
        assertEquals(0.0f, lockStep.panDeltaX, 0.0001f)

        // Subsequent pinch while in SCROLL_LOCKED must NOT trigger zoom change or horizontal pan
        val subsequentStep = arbitrator.onPointersMove(
            currentCentroid = androidx.compose.ui.geometry.Offset(520f, 550f),
            currentSpan = 350f // large pinch spread
        )
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, subsequentStep.mode)
        assertEquals(20.0f, subsequentStep.verticalScrollDelta, 0.0001f)
        assertEquals(1.0f, subsequentStep.zoomChange, 0.0001f) // locked!
        assertEquals(0.0f, subsequentStep.panDeltaX, 0.0001f)   // locked!
    }

    @Test
    fun `pinch span delta exceeding zoom slop strictly locks mode to ZOOM_LOCKED`() {
        val arbitrator = TwoFingerGestureArbitrator(touchSlop = 20f, zoomSlop = 20f)
        val initialCenter = androidx.compose.ui.geometry.Offset(500f, 500f)
        val initialSpan = 200f

        arbitrator.onPointersDown(initialCenter, initialSpan)

        // Exceed zoom slop (deltaSpan = 30f >= 20f, deltaY = 5f < 20f)
        val lockStep = arbitrator.onPointersMove(
            currentCentroid = androidx.compose.ui.geometry.Offset(500f, 505f),
            currentSpan = 230f
        )
        assertEquals(TwoFingerGestureMode.ZOOM_LOCKED, lockStep.mode)
        assertEquals(TwoFingerGestureMode.ZOOM_LOCKED, arbitrator.mode)
        assertEquals(0.0f, lockStep.verticalScrollDelta, 0.0001f) // vertical scroll suppressed!
        assertEquals(230f / 200f, lockStep.zoomChange, 0.0001f)

        // Subsequent vertical hand movement while in ZOOM_LOCKED must produce ZERO vertical scroll
        val subsequentStep = arbitrator.onPointersMove(
            currentCentroid = androidx.compose.ui.geometry.Offset(500f, 600f), // 95px vertical drag
            currentSpan = 250f
        )
        assertEquals(TwoFingerGestureMode.ZOOM_LOCKED, subsequentStep.mode)
        assertEquals(0.0f, subsequentStep.verticalScrollDelta, 0.0001f) // completely suppressed!
        assertEquals(250f / 230f, subsequentStep.zoomChange, 0.0001f)
    }

    @Test
    fun `pointers up cleanly resets arbitrator back to UNDECIDED`() {
        val arbitrator = TwoFingerGestureArbitrator(touchSlop = 20f, zoomSlop = 20f)
        arbitrator.onPointersDown(androidx.compose.ui.geometry.Offset(500f, 500f), 200f)
        arbitrator.onPointersMove(androidx.compose.ui.geometry.Offset(500f, 540f), 200f)
        assertEquals(TwoFingerGestureMode.SCROLL_LOCKED, arbitrator.mode)

        arbitrator.onPointersUp()
        assertEquals(TwoFingerGestureMode.UNDECIDED, arbitrator.mode)
        assertTrue(arbitrator.isInitialState())
    }
}
