package com.mal5odha.core.ink.ui

import android.graphics.*
import kotlin.math.hypot

enum class HandleType {
    NONE, INSIDE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, ROTATE
}

/** Handles the visual rendering of the Lasso selection bounds, resize handles, and rotation pin. */
class LassoToolPainter {

    private val lassoPathPaint =
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = Color.parseColor("#007AFF")
                strokeWidth = 2.5f
                pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
            }

    private val boundsPaint =
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = Color.parseColor("#007AFF") // Standard selection blue
                strokeWidth = 2f
                pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
            }

    private val guideLinePaint =
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = Color.parseColor("#007AFF")
                strokeWidth = 1.5f
            }

    private val handlePaint =
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.WHITE
            }

    private val handleBorderPaint =
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = Color.parseColor("#007AFF")
                strokeWidth = 2.5f
            }

    private val rotateHandlePaint =
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.parseColor("#007AFF")
            }

    fun drawLassoPath(canvas: Canvas, selectionPath: Path) {
        if (!selectionPath.isEmpty) {
            canvas.drawPath(selectionPath, lassoPathPaint)
        }
    }

    fun drawSelectionBounds(canvas: Canvas, selectionPath: Path, hasSelectedStrokes: Boolean) {
        if (!selectionPath.isEmpty && hasSelectedStrokes) {
            val rectF = RectF()
            selectionPath.computeBounds(rectF, true)
            rectF.inset(-16f, -16f)

            // Draw bounding box
            canvas.drawRect(rectF, boundsPaint)

            // Draw 4 corner resize handles
            val handleRadius = 10f
            drawHandle(canvas, rectF.left, rectF.top, handleRadius)
            drawHandle(canvas, rectF.right, rectF.top, handleRadius)
            drawHandle(canvas, rectF.left, rectF.bottom, handleRadius)
            drawHandle(canvas, rectF.right, rectF.bottom, handleRadius)

            // Draw top rotation pin
            val rotateX = rectF.centerX()
            val rotateY = rectF.top - 32f
            canvas.drawLine(rotateX, rectF.top, rotateX, rotateY, guideLinePaint)
            canvas.drawCircle(rotateX, rotateY, 9f, handlePaint)
            canvas.drawCircle(rotateX, rotateY, 9f, handleBorderPaint)
            canvas.drawCircle(rotateX, rotateY, 4f, rotateHandlePaint)
        }
    }

    private fun drawHandle(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        canvas.drawCircle(cx, cy, radius, handlePaint)
        canvas.drawCircle(cx, cy, radius, handleBorderPaint)
    }

    fun hitTestHandle(
        x: Float,
        y: Float,
        selectionPath: Path,
        hasSelectedStrokes: Boolean,
        touchRadius: Float = 36f
    ): HandleType {
        if (selectionPath.isEmpty || !hasSelectedStrokes) return HandleType.NONE

        val rectF = RectF()
        selectionPath.computeBounds(rectF, true)
        rectF.inset(-16f, -16f)

        // Test rotation pin
        val rotateX = rectF.centerX()
        val rotateY = rectF.top - 32f
        if (hypot(x - rotateX, y - rotateY) <= touchRadius) return HandleType.ROTATE

        // Test 4 corner handles
        if (hypot(x - rectF.left, y - rectF.top) <= touchRadius) return HandleType.TOP_LEFT
        if (hypot(x - rectF.right, y - rectF.top) <= touchRadius) return HandleType.TOP_RIGHT
        if (hypot(x - rectF.left, y - rectF.bottom) <= touchRadius) return HandleType.BOTTOM_LEFT
        if (hypot(x - rectF.right, y - rectF.bottom) <= touchRadius) return HandleType.BOTTOM_RIGHT

        // Test inside bounding box
        if (rectF.contains(x, y)) return HandleType.INSIDE

        return HandleType.NONE
    }
}
