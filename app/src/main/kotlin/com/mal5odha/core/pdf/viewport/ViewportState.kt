package com.mal5odha.core.pdf.viewport

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import com.mal5odha.core.ink.models.Point

/**
 * Manages document viewport zoom scale, pan translation offsets, and bidirectional
 * Screen-to-Page (M⁻¹) and Page-to-Screen (M) coordinate transformation matrices.
 *
 * Thread-safe implementation ensuring inking overlays and background document layers
 * remain in strict 1:1 mathematical spatial alignment during multi-touch zoom and pan.
 */
class ViewportState(
    initialScale: Float = 1.0f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
    val minScale: Float = 0.5f,
    val maxScale: Float = 5.0f
) {
    @Volatile
    var scale: Float = initialScale
        private set

    @Volatile
    var offsetX: Float = initialOffsetX
        private set

    @Volatile
    var offsetY: Float = initialOffsetY
        private set

    private val lock = Any()
    private val documentToScreenMatrix = Matrix()
    private val screenToDocumentMatrix = Matrix()

    var onViewportChanged: ((scale: Float, offsetX: Float, offsetY: Float) -> Unit)? = null

    init {
        updateMatricesInternal()
    }

    private fun updateMatricesInternal() {
        synchronized(lock) {
            documentToScreenMatrix.reset()
            documentToScreenMatrix.postScale(scale, scale)
            documentToScreenMatrix.postTranslate(offsetX, offsetY)

            documentToScreenMatrix.invert(screenToDocumentMatrix)
        }
    }

    /**
     * Updates pan offset by delta amounts (in screen pixels).
     */
    fun pan(dx: Float, dy: Float) {
        synchronized(lock) {
            offsetX += dx
            offsetY += dy
            updateMatricesInternal()
        }
        onViewportChanged?.invoke(scale, offsetX, offsetY)
    }

    /**
     * Updates zoom level around a focal/centroid point (in screen pixels).
     *
     * Transformation math:
     *   Offset_new = Focal - (Focal - Offset_old) * (Scale_new / Scale_old)
     */
    fun zoom(zoomFactor: Float, focalX: Float, focalY: Float) {
        var changed = false
        synchronized(lock) {
            val newScale = (scale * zoomFactor).coerceIn(minScale, maxScale)
            if (newScale != scale) {
                val factor = newScale / scale
                offsetX = focalX - (focalX - offsetX) * factor
                offsetY = focalY - (focalY - offsetY) * factor
                scale = newScale
                updateMatricesInternal()
                changed = true
            }
        }
        if (changed) {
            onViewportChanged?.invoke(scale, offsetX, offsetY)
        }
    }

    /**
     * Sets arbitrary scale and offsets in a single atomic transaction.
     */
    fun setTransform(newScale: Float, newOffsetX: Float, newOffsetY: Float) {
        synchronized(lock) {
            scale = newScale.coerceIn(minScale, maxScale)
            offsetX = newOffsetX
            offsetY = newOffsetY
            updateMatricesInternal()
        }
        onViewportChanged?.invoke(scale, offsetX, offsetY)
    }

    /**
     * Resets viewport back to 100% scale and center offset.
     */
    fun reset(targetOffsetX: Float = 0f, targetOffsetY: Float = 0f) {
        setTransform(1.0f, targetOffsetX, targetOffsetY)
    }

    // ─── Bidirectional Coordinate Mapping (M and M⁻¹) ─────────────────────────────

    /**
     * Maps raw screen coordinate (X_screen, Y_screen) to normalized page coordinate (X_page, Y_page)
     * using the inverse matrix M⁻¹.
     */
    fun screenToPagePoint(screenX: Float, screenY: Float): PointF {
        val pts = floatArrayOf(screenX, screenY)
        synchronized(lock) {
            screenToDocumentMatrix.mapPoints(pts)
        }
        return PointF(pts[0], pts[1])
    }

    /**
     * Zero-allocation in-place mapping of screen coordinates to document coordinates.
     * [outPts] must be at least FloatArray(2).
     */
    fun screenToPagePointInPlace(screenX: Float, screenY: Float, outPts: FloatArray) {
        outPts[0] = screenX
        outPts[1] = screenY
        synchronized(lock) {
            screenToDocumentMatrix.mapPoints(outPts)
        }
    }

    /**
     * Maps a [Point] in screen space to document/page space using M⁻¹.
     */
    fun screenToPagePoint(point: Point): Point {
        val pts = floatArrayOf(point.x, point.y)
        synchronized(lock) {
            screenToDocumentMatrix.mapPoints(pts)
        }
        return Point(
            x = pts[0],
            y = pts[1],
            pressure = point.pressure,
            timestamp = point.timestamp
        )
    }

    /**
     * Maps page coordinate (X_page, Y_page) to screen coordinate (X_screen, Y_screen)
     * using the forward matrix M.
     */
    fun pageToScreenPoint(pageX: Float, pageY: Float): PointF {
        val pts = floatArrayOf(pageX, pageY)
        synchronized(lock) {
            documentToScreenMatrix.mapPoints(pts)
        }
        return PointF(pts[0], pts[1])
    }

    /**
     * Maps a [Point] in page space to screen space using M.
     */
    fun pageToScreenPoint(point: Point): Point {
        val pts = floatArrayOf(point.x, point.y)
        synchronized(lock) {
            documentToScreenMatrix.mapPoints(pts)
        }
        return Point(
            x = pts[0],
            y = pts[1],
            pressure = point.pressure,
            timestamp = point.timestamp
        )
    }

    /**
     * Computes visible document-space viewport bounds given the screen viewport dimensions.
     */
    fun getVisibleDocumentBounds(screenWidth: Float, screenHeight: Float): RectF {
        val topLeft = screenToPagePoint(0f, 0f)
        val bottomRight = screenToPagePoint(screenWidth, screenHeight)
        return RectF(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    /**
     * Returns a snapshot copy of the forward transformation matrix M (Document -> Screen).
     */
    fun getTransformationMatrix(): Matrix {
        synchronized(lock) {
            return Matrix(documentToScreenMatrix)
        }
    }

    /**
     * Returns a snapshot copy of the inverse transformation matrix M⁻¹ (Screen -> Document).
     */
    fun getInverseMatrix(): Matrix {
        synchronized(lock) {
            return Matrix(screenToDocumentMatrix)
        }
    }
}
