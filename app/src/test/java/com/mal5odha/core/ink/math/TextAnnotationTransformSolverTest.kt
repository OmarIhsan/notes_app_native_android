package com.mal5odha.core.ink.math

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextAnnotationTransformSolverTest {

    private val epsilon = 0.0001f

    @Test
    fun `right handle resize maintains stationary left anchor`() {
        val initialX = 0.2f
        val initialWidth = 0.3f
        val deltaX = 0.15f

        val (newX, newWidth) = TextAnnotationTransformSolver.resizeHorizontal(
            currentX = initialX,
            currentWidth = initialWidth,
            deltaXNorm = deltaX,
            isLeftHandle = false
        )

        // Left anchor must be strictly stationary
        assertEquals(initialX, newX, epsilon)
        // Width must expand by deltaX
        assertEquals(initialWidth + deltaX, newWidth, epsilon)
    }

    @Test
    fun `left handle resize maintains stationary right anchor`() {
        val initialX = 0.3f
        val initialWidth = 0.4f
        val initialRightAnchor = initialX + initialWidth // 0.7f
        val deltaX = -0.1f // dragging to the left, expands box

        val (newX, newWidth) = TextAnnotationTransformSolver.resizeHorizontal(
            currentX = initialX,
            currentWidth = initialWidth,
            deltaXNorm = deltaX,
            isLeftHandle = true
        )

        // The right anchor must remain strictly stationary
        val newRightAnchor = newX + newWidth
        assertEquals(initialRightAnchor, newRightAnchor, epsilon)
        assertEquals(0.2f, newX, epsilon)
        assertEquals(0.5f, newWidth, epsilon)
    }

    @Test
    fun `left handle resize dragging right maintains stationary right anchor and clamps min width`() {
        val initialX = 0.2f
        val initialWidth = 0.3f
        val initialRightAnchor = initialX + initialWidth // 0.5f
        val deltaX = 0.5f // large drag to the right past min width

        val (newX, newWidth) = TextAnnotationTransformSolver.resizeHorizontal(
            currentX = initialX,
            currentWidth = initialWidth,
            deltaXNorm = deltaX,
            isLeftHandle = true,
            minWidthNorm = 0.1f
        )

        val newRightAnchor = newX + newWidth
        assertEquals(initialRightAnchor, newRightAnchor, epsilon)
        assertEquals(0.1f, newWidth, epsilon)
        assertEquals(0.4f, newX, epsilon)
    }

    @Test
    fun `right handle resize clamps to page right margin`() {
        val initialX = 0.7f
        val initialWidth = 0.2f
        val deltaX = 0.5f // would exceed page margin 0.99f

        val (newX, newWidth) = TextAnnotationTransformSolver.resizeHorizontal(
            currentX = initialX,
            currentWidth = initialWidth,
            deltaXNorm = deltaX,
            isLeftHandle = false,
            pageMarginRightNorm = 0.99f
        )

        assertEquals(initialX, newX, epsilon)
        assertEquals(0.99f - 0.7f, newWidth, epsilon)
    }

    @Test
    fun `translate clamps box within normalized bounds`() {
        val width = 0.3f
        val height = 0.2f

        // Normal translation
        val (x1, y1) = TextAnnotationTransformSolver.translate(
            currentX = 0.2f,
            currentY = 0.3f,
            width = width,
            height = height,
            deltaXNorm = 0.1f,
            deltaYNorm = -0.1f
        )
        assertEquals(0.3f, x1, epsilon)
        assertEquals(0.2f, y1, epsilon)

        // Translation overflowing bottom-right
        val (x2, y2) = TextAnnotationTransformSolver.translate(
            currentX = 0.6f,
            currentY = 0.7f,
            width = width,
            height = height,
            deltaXNorm = 0.5f,
            deltaYNorm = 0.5f
        )
        assertEquals(1.0f - width, x2, epsilon)
        assertEquals(1.0f - height, y2, epsilon)

        // Translation overflowing top-left
        val (x3, y3) = TextAnnotationTransformSolver.translate(
            currentX = 0.1f,
            currentY = 0.1f,
            width = width,
            height = height,
            deltaXNorm = -0.5f,
            deltaYNorm = -0.5f
        )
        assertEquals(0f, x3, epsilon)
        assertEquals(0f, y3, epsilon)
    }

    @Test
    fun `calculateNormalizedTapPlacement places and clamps card within page margins`() {
        val pageWidth = 1000f
        val pageHeight = 1500f
        val defaultWidthPx = 300f

        // Mid-page tap
        val placement1 = TextAnnotationTransformSolver.calculateNormalizedTapPlacement(
            tapPx = Offset(200f, 400f),
            pageWidthPx = pageWidth,
            pageHeightPx = pageHeight,
            defaultWidthPx = defaultWidthPx
        )
        assertEquals(0.2f, placement1.xNorm, epsilon)
        assertEquals(400f / 1500f, placement1.yNorm, epsilon)
        assertEquals(0.3f, placement1.widthNorm, epsilon)

        // Tap near the right edge nudges left so width fits inside maxMarginNorm (0.98f)
        val placement2 = TextAnnotationTransformSolver.calculateNormalizedTapPlacement(
            tapPx = Offset(900f, 500f),
            pageWidthPx = pageWidth,
            pageHeightPx = pageHeight,
            defaultWidthPx = defaultWidthPx
        )
        assertTrue(placement2.xNorm + placement2.widthNorm <= 0.98f + epsilon)
        assertTrue(placement2.xNorm >= 0.02f)
    }

    @Test
    fun `computeNormalizedHeight computes normalized height maintaining stationary top`() {
        val measuredPx = 150f
        val pageHeight = 1500f
        val normH = TextAnnotationTransformSolver.computeNormalizedHeight(measuredPx, pageHeight)
        assertEquals(0.1f, normH, epsilon)
    }

    @Test
    fun `calculateCenteredToolbarOffset aligns midpoints mid-page`() {
        val boxLeft = 400f
        val boxWidth = 200f
        val toolbarWidth = 400f
        val pageWidth = 1000f

        val offset = TextAnnotationTransformSolver.calculateCenteredToolbarOffset(
            boxLeftPx = boxLeft,
            boxWidthPx = boxWidth,
            toolbarWidthPx = toolbarWidth,
            pageWidthPx = pageWidth,
            marginPaddingPx = 12f
        )

        val actualBarLeft = boxLeft + offset
        val boxCenter = boxLeft + boxWidth / 2f
        val barCenter = actualBarLeft + toolbarWidth / 2f

        assertEquals(boxCenter, barCenter, epsilon)
        assertEquals(-100f, offset, epsilon)
    }

    @Test
    fun `calculateCenteredToolbarOffset clamps left margin when box is at far left`() {
        val boxLeft = 10f
        val boxWidth = 200f
        val toolbarWidth = 400f
        val pageWidth = 1000f
        val margin = 12f

        val offset = TextAnnotationTransformSolver.calculateCenteredToolbarOffset(
            boxLeftPx = boxLeft,
            boxWidthPx = boxWidth,
            toolbarWidthPx = toolbarWidth,
            pageWidthPx = pageWidth,
            marginPaddingPx = margin
        )

        val actualBarLeft = boxLeft + offset
        assertEquals(margin, actualBarLeft, epsilon)
    }

    @Test
    fun `calculateCenteredToolbarOffset clamps right margin when box is at far right`() {
        val boxLeft = 850f
        val boxWidth = 140f
        val toolbarWidth = 400f
        val pageWidth = 1000f
        val margin = 12f

        val offset = TextAnnotationTransformSolver.calculateCenteredToolbarOffset(
            boxLeftPx = boxLeft,
            boxWidthPx = boxWidth,
            toolbarWidthPx = toolbarWidth,
            pageWidthPx = pageWidth,
            marginPaddingPx = margin
        )

        val actualBarLeft = boxLeft + offset
        val expectedMaxLeft = pageWidth - toolbarWidth - margin
        assertEquals(expectedMaxLeft, actualBarLeft, epsilon)
    }
}
