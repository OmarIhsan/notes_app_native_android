package com.mal5odha.core.ink.math

import android.graphics.Path
import android.graphics.RectF
import com.mal5odha.core.ink.models.Point
import kotlin.math.hypot
import kotlin.math.max

/**
 * High-performance spline interpolator for stylus strokes.
 * Converts raw discrete stylus touch coordinates into continuous, smoothly curved cubic Bézier paths
 * using Catmull-Rom spline interpolation and calculates dynamic velocity/pressure-based widths.
 */
object SplineInterpolator {

    /**
     * Converts an ordered sequence of stylus points into a smoothly curved Android [Path]
     * using Centripetal / Standard Catmull-Rom to Cubic Bézier conversion.
     */
    fun createSmoothPath(points: List<Point>, targetPath: Path = Path()): Path {
        targetPath.reset()
        if (points.isEmpty()) return targetPath

        if (points.size == 1) {
            val p = points[0]
            targetPath.moveTo(p.x, p.y)
            targetPath.lineTo(p.x + 0.1f, p.y + 0.1f)
            return targetPath
        }

        if (points.size == 2) {
            targetPath.moveTo(points[0].x, points[0].y)
            targetPath.lineTo(points[1].x, points[1].y)
            return targetPath
        }

        targetPath.moveTo(points[0].x, points[0].y)

        val count = points.size
        for (i in 0 until count - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < count) points[i + 2] else p2

            // Standard Catmull-Rom to Cubic Bézier control point transformation:
            // C1 = P1 + (P2 - P0) / 6
            // C2 = P2 - (P3 - P1) / 6
            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f

            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f

            targetPath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }

        return targetPath
    }

    /**
     * Calculates dynamic stroke width for a given point pair based on stylus pressure and drawing velocity.
     * Formula: Width = BaseWidth * (alpha * Pressure + (1 - alpha) * (1 / (1 + beta * Velocity)))
     *
     * @param baseWidth Base width defined by the user/tool
     * @param pressure Stylus pressure (0.0 to 1.0)
     * @param p1 Previous point with timestamp
     * @param p2 Current point with timestamp
     * @param alpha Weight factor balancing pressure vs velocity [0.0..1.0]
     */
    fun calculateDynamicWidth(
        baseWidth: Float,
        pressure: Float,
        p1: Point,
        p2: Point,
        alpha: Float = 0.65f
    ): Float {
        val dt = max(1L, p2.timestamp - p1.timestamp).toFloat() // ms
        val distance = hypot(p2.x - p1.x, p2.y - p1.y) // px
        val velocity = distance / dt // px/ms

        // Velocity normalization factor: higher velocity reduces width for natural ink taper
        val velocityFactor = 1f / (1f + 0.15f * velocity)
        val normalizedPressure = pressure.coerceIn(0.1f, 1.0f)

        val dynamicFactor = (alpha * normalizedPressure) + ((1f - alpha) * velocityFactor)
        val finalWidth = baseWidth * dynamicFactor

        // Clamp between 35% and 180% of base width to prevent extreme line degradation
        return finalWidth.coerceIn(baseWidth * 0.35f, baseWidth * 1.8f)
    }

    /**
     * Computes the axis-aligned bounding box (AABB) of a sequence of points, padded by stroke width.
     */
    fun calculateBoundingBox(points: List<Point>, strokeWidth: Float): RectF {
        if (points.isEmpty()) return RectF(0f, 0f, 0f, 0f)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }

        val padding = strokeWidth * 0.5f + 2f
        return RectF(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }
}
