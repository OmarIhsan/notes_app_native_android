package com.mal5odha.core.pdf.viewport

import androidx.compose.ui.geometry.Offset

/**
 * Pure mathematical solver for continuous multi-page viewport zoom and pan.
 *
 * Core Physical & Mathematical Invariants:
 * 1. Scale Floor Clamping: Scale factor s is strictly in [1.0f, 5.0f]. No sub-1.0 rubber-banding.
 * 2. At scale = 1.0f: Page fits viewport width flush; panX is locked identically to 0.0f.
 * 3. Decoupled Continuous Vertical Scroll: Container-level vertical translation Ty is strictly 0.
 *    Vertical displacement is extracted to dispatch directly to LazyListState.
 * 4. Horizontal Panning & Edge Clamping: Bounded strictly to [-viewportWidth * (s - 1), 0f] with zero margin gap.
 * 5. Pinch-to-Zoom Focal Anchor Preservation: The visual content directly beneath the touch focal
 *    point remains stationary during scaling.
 */
object ZoomTransformSolver {
    const val MIN_SCALE: Float = 1.0f
    const val MAX_SCALE: Float = 5.0f

    data class ZoomState(
        val scale: Float = 1.0f,
        val panX: Float = 0.0f
    )

    data class ZoomStepResult(
        val zoomState: ZoomState,
        val verticalScrollDelta: Float
    )

    /**
     * Calculates the new [ZoomState] and extracted vertical scroll delta given the gesture step inputs.
     *
     * @param previousState The existing scale and panX.
     * @param zoomChange The incremental zoom ratio (scale delta) from the multi-touch gesture.
     * @param panDeltaX The horizontal finger displacement delta (in screen pixels).
     * @param panDeltaY The vertical finger displacement delta (in screen pixels).
     * @param focalPointX The horizontal focal center (centroid) of active touches (in screen pixels).
     * @param viewportWidth The visible screen/container width (in screen pixels).
     */
    fun calculateZoomStep(
        previousState: ZoomState,
        zoomChange: Float,
        panDeltaX: Float,
        panDeltaY: Float,
        focalPointX: Float,
        viewportWidth: Float
    ): ZoomStepResult {
        val newScale = (previousState.scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)

        val newPanX = if (newScale <= MIN_SCALE || viewportWidth <= 0f) {
            0.0f
        } else {
            // Anchor preservation around focal point:
            // Content under focalPointX before scale is X_doc = (focalPointX - previousPanX) / previousScale
            // Content under focalPointX after scale is X_screen = focalPointX - (focalPointX - previousPanX) * (newScale / previousScale)
            val scaleRatio = newScale / previousState.scale
            val scaledPanX = focalPointX - (focalPointX - previousState.panX) * scaleRatio
            val candidatePanX = scaledPanX + panDeltaX
            val minPanX = -viewportWidth * (newScale - 1.0f)
            candidatePanX.coerceIn(minPanX, 0.0f)
        }

        return ZoomStepResult(
            zoomState = ZoomState(scale = newScale, panX = newPanX),
            verticalScrollDelta = panDeltaY
        )
    }

    /**
     * Overload accepting Compose [Offset] for focal point convenience.
     */
    fun calculateZoomStep(
        previousState: ZoomState,
        zoomChange: Float,
        panDeltaX: Float,
        panDeltaY: Float,
        focalPoint: Offset,
        viewportWidth: Float
    ): ZoomStepResult = calculateZoomStep(
        previousState = previousState,
        zoomChange = zoomChange,
        panDeltaX = panDeltaX,
        panDeltaY = panDeltaY,
        focalPointX = focalPoint.x,
        viewportWidth = viewportWidth
    )
}

/**
 * Mutually exclusive two-finger interaction modes (Apple Notes paradigm).
 */
enum class TwoFingerGestureMode {
    UNDECIDED,      // Accumulating touch slop; zero transformation dispatched
    SCROLL_LOCKED,  // Pure vertical continuous document navigation
    ZOOM_LOCKED     // Pure focal scaling & horizontal margin pan
}

/**
 * Arbitrates two-finger gestures to strictly lock into SCROLL_LOCKED or ZOOM_LOCKED.
 * Prevents accidental zoom jitter during scrolling and eliminates scroll drift during pinch-to-zoom.
 */
class TwoFingerGestureArbitrator(
    val touchSlop: Float = 18f,
    val zoomSlop: Float = 18f
) {
    var mode: TwoFingerGestureMode = TwoFingerGestureMode.UNDECIDED
        private set

    private var initialCentroid: Offset? = null
    private var initialSpan: Float = 0f
    private var prevCentroid: Offset? = null
    private var prevSpan: Float = 0f

    fun isInitialState(): Boolean = initialCentroid == null

    fun onPointersDown(centroid: Offset, span: Float) {
        mode = TwoFingerGestureMode.UNDECIDED
        initialCentroid = centroid
        initialSpan = span
        prevCentroid = centroid
        prevSpan = span
    }

    data class GestureStepResult(
        val mode: TwoFingerGestureMode,
        val zoomChange: Float = 1.0f,
        val panDeltaX: Float = 0.0f,
        val verticalScrollDelta: Float = 0.0f
    )

    fun onPointersMove(
        currentCentroid: Offset,
        currentSpan: Float,
        isZoomed: Boolean = false
    ): GestureStepResult {
        val startCentroid = initialCentroid ?: currentCentroid
        val startSpan = if (initialSpan > 0f) initialSpan else currentSpan
        val lastCentroid = prevCentroid ?: currentCentroid
        val lastSpan = if (prevSpan > 0f) prevSpan else currentSpan

        val totalDeltaY = kotlin.math.abs(currentCentroid.y - startCentroid.y)
        val totalDeltaX = kotlin.math.abs(currentCentroid.x - startCentroid.x)
        val totalSpanDelta = kotlin.math.abs(currentSpan - startSpan)

        if (mode == TwoFingerGestureMode.UNDECIDED) {
            val isScrollIntent = totalDeltaY >= touchSlop && totalDeltaY > totalSpanDelta
            val isZoomIntent = totalSpanDelta >= zoomSlop || (isZoomed && totalDeltaX >= touchSlop && totalDeltaX > totalDeltaY)

            if (isScrollIntent) {
                mode = TwoFingerGestureMode.SCROLL_LOCKED
            } else if (isZoomIntent) {
                mode = TwoFingerGestureMode.ZOOM_LOCKED
            }
        }

        val stepPanX = currentCentroid.x - lastCentroid.x
        val stepPanY = currentCentroid.y - lastCentroid.y
        val stepZoomChange = if (lastSpan > 0f) currentSpan / lastSpan else 1.0f

        prevCentroid = currentCentroid
        prevSpan = currentSpan

        return when (mode) {
            TwoFingerGestureMode.UNDECIDED -> {
                GestureStepResult(mode = TwoFingerGestureMode.UNDECIDED)
            }
            TwoFingerGestureMode.SCROLL_LOCKED -> {
                GestureStepResult(
                    mode = TwoFingerGestureMode.SCROLL_LOCKED,
                    zoomChange = 1.0f,
                    panDeltaX = 0.0f,
                    verticalScrollDelta = stepPanY
                )
            }
            TwoFingerGestureMode.ZOOM_LOCKED -> {
                GestureStepResult(
                    mode = TwoFingerGestureMode.ZOOM_LOCKED,
                    zoomChange = stepZoomChange,
                    panDeltaX = stepPanX,
                    verticalScrollDelta = 0.0f
                )
            }
        }
    }

    fun onPointersUp() {
        mode = TwoFingerGestureMode.UNDECIDED
        initialCentroid = null
        initialSpan = 0f
        prevCentroid = null
        prevSpan = 0f
    }
}
