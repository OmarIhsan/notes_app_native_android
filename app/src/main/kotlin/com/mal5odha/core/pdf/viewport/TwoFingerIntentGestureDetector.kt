package com.mal5odha.core.pdf.viewport

import androidx.compose.ui.geometry.Offset

/**
 * Two-finger intent-locked gesture detector (Apple Notes paradigm).
 *
 * Arbitrates between pure continuous vertical scrolling and focal pinch-zoom/pan,
 * with inverted vertical scroll delta to match natural scrolling polarity in Jetpack Compose.
 */
class TwoFingerIntentGestureDetector(
    val touchSlop: Float = 18f,
    val zoomSlop: Float = 18f,
    val invertScrollDirection: Boolean = true
) {
    private val arbitrator = TwoFingerGestureArbitrator(touchSlop, zoomSlop)

    val mode: TwoFingerGestureMode
        get() = arbitrator.mode

    fun isInitialState(): Boolean = arbitrator.isInitialState()

    fun onPointersDown(centroid: Offset, span: Float) {
        arbitrator.onPointersDown(centroid, span)
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
        val rawStep = arbitrator.onPointersMove(currentCentroid, currentSpan, isZoomed)
        val scrollDelta = if (invertScrollDirection) {
            -rawStep.verticalScrollDelta
        } else {
            rawStep.verticalScrollDelta
        }

        return GestureStepResult(
            mode = rawStep.mode,
            zoomChange = rawStep.zoomChange,
            panDeltaX = rawStep.panDeltaX,
            verticalScrollDelta = scrollDelta
        )
    }

    fun onPointersUp() {
        arbitrator.onPointersUp()
    }
}
