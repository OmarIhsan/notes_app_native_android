package com.mal5odha.core.ink.math

import androidx.compose.ui.geometry.Offset

/**
 * Pure mathematical solver for interactive text annotations in page-normalized space [0.0, 1.0].
 * Implements Goodnotes/Notewise stationary opposite-anchor transforms, bounded horizontal sizing,
 * and page-clamped translation.
 */
object TextAnnotationTransformSolver {

    data class TextPlacement(
        val xNorm: Float,
        val yNorm: Float,
        val widthNorm: Float
    )

    /**
     * Solves horizontal resize with strict stationary opposite-anchor invariance.
     *
     * - Right Handle: Left edge remains strictly stationary (X_anchor = X).
     *                 Width expands/contracts towards the right margin.
     * - Left Handle:  Right edge remains strictly stationary (X_anchor = X + W).
     *                 Width expands/contracts, shifting X such that X + W == X_anchor.
     *
     * @param currentX Current normalized X [0.0..1.0]
     * @param currentWidth Current normalized width [0.0..1.0]
     * @param deltaXNorm Change in normalized X from drag gesture
     * @param isLeftHandle True if left pill handle is dragged, false if right pill handle
     * @param minWidthNorm Minimum allowed normalized width
     * @param pageMarginRightNorm Right boundary limit (defaults to 1.0f)
     * @return Pair(newXNorm, newWidthNorm)
     */
    fun resizeHorizontal(
        currentX: Float,
        currentWidth: Float,
        deltaXNorm: Float,
        isLeftHandle: Boolean,
        minWidthNorm: Float = 0.08f,
        pageMarginRightNorm: Float = 0.99f
    ): Pair<Float, Float> {
        val minW = minWidthNorm.coerceAtLeast(0.02f)

        return if (!isLeftHandle) {
            // Right handle: Anchor is the left edge (currentX)
            val anchorLeft = currentX
            val maxWidth = (pageMarginRightNorm - anchorLeft).coerceAtLeast(minW)
            val targetWidth = (currentWidth + deltaXNorm).coerceIn(minW, maxWidth)
            Pair(anchorLeft, targetWidth)
        } else {
            // Left handle: Anchor is the right edge (currentX + currentWidth)
            val anchorRight = currentX + currentWidth
            val candidateLeft = currentX + deltaXNorm
            val candidateWidth = anchorRight - candidateLeft

            // Width cannot exceed anchorRight (so left doesn't cross x = 0)
            val maxAllowedWidth = anchorRight.coerceAtLeast(minW)
            val finalWidth = candidateWidth.coerceIn(minW, maxAllowedWidth)
            val finalX = anchorRight - finalWidth
            Pair(finalX, finalWidth)
        }
    }

    /**
     * Translates the annotation card across the page, strictly clamped within [0.0, 1.0] bounds.
     */
    fun translate(
        currentX: Float,
        currentY: Float,
        width: Float,
        height: Float,
        deltaXNorm: Float,
        deltaYNorm: Float
    ): Pair<Float, Float> {
        val maxX = (1.0f - width).coerceAtLeast(0f)
        val maxY = (1.0f - height).coerceAtLeast(0f)
        val newX = (currentX + deltaXNorm).coerceIn(0f, maxX)
        val newY = (currentY + deltaYNorm).coerceIn(0f, maxY)
        return Pair(newX, newY)
    }

    /**
     * Converts a pixel tap position (X_tap, Y_tap) on a page of dimensions (pageWidthPx, pageHeightPx)
     * into a normalized placement [0.0..1.0] with an initial default width (~220dp equivalent),
     * clamped so the card remains fully within page bounds.
     */
    fun calculateNormalizedTapPlacement(
        tapPx: Offset,
        pageWidthPx: Float,
        pageHeightPx: Float,
        defaultWidthPx: Float,
        minMarginNorm: Float = 0.02f,
        maxMarginNorm: Float = 0.98f
    ): TextPlacement {
        val safePageW = pageWidthPx.coerceAtLeast(1f)
        val safePageH = pageHeightPx.coerceAtLeast(1f)

        var xNorm = (tapPx.x / safePageW).coerceIn(minMarginNorm, maxMarginNorm)
        val yNorm = (tapPx.y / safePageH).coerceIn(minMarginNorm, maxMarginNorm)

        var widthNorm = (defaultWidthPx / safePageW).coerceIn(0.10f, 0.90f)

        // Ensure the box does not bleed over the right margin
        if (xNorm + widthNorm > maxMarginNorm) {
            val overflow = (xNorm + widthNorm) - maxMarginNorm
            if (xNorm - overflow >= minMarginNorm) {
                // Nudge left if there's room
                xNorm -= overflow
            } else {
                // Shrink width to fit
                xNorm = minMarginNorm
                widthNorm = (maxMarginNorm - minMarginNorm).coerceAtLeast(0.10f)
            }
        }

        return TextPlacement(
            xNorm = xNorm,
            yNorm = yNorm,
            widthNorm = widthNorm
        )
    }

    /**
     * Converts measured pixel height to normalized height, keeping the top coordinate stationary.
     */
    fun computeNormalizedHeight(
        measuredHeightPx: Float,
        pageHeightPx: Float,
        minHeightNorm: Float = 0.02f
    ): Float {
        val safeH = pageHeightPx.coerceAtLeast(1f)
        return (measuredHeightPx / safeH).coerceAtLeast(minHeightNorm)
    }

    /**
     * Calculates the horizontal offset (relative to boxLeftPx) required to align the horizontal
     * midpoint of a floating toolbar directly with the horizontal midpoint of a text box,
     * while clamping the toolbar's page-relative bounds inside page margins.
     *
     * @param boxLeftPx Text box left coordinate in page pixels
     * @param boxWidthPx Text box width in pixels
     * @param toolbarWidthPx Toolbar width in pixels
     * @param pageWidthPx Page width in pixels
     * @param marginPaddingPx Minimum margin padding in pixels
     * @return Local horizontal offset (in pixels) relative to boxLeftPx
     */
    fun calculateCenteredToolbarOffset(
        boxLeftPx: Float,
        boxWidthPx: Float,
        toolbarWidthPx: Float,
        pageWidthPx: Float,
        marginPaddingPx: Float = 12f
    ): Float {
        val minBarX = marginPaddingPx
        val maxBarX = (pageWidthPx - toolbarWidthPx - marginPaddingPx).coerceAtLeast(minBarX)
        val targetBarPageX = boxLeftPx + (boxWidthPx - toolbarWidthPx) / 2f
        val clampedBarPageX = targetBarPageX.coerceIn(minBarX, maxBarX)
        return clampedBarPageX - boxLeftPx
    }
}
