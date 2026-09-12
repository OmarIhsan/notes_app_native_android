package com.mal5odha.core.ink.math

import android.graphics.Path
import android.graphics.RectF
import com.mal5odha.core.ink.models.Point
import kotlin.math.hypot
import kotlin.math.max

/**
 * High-performance Catmull-Rom to Cubic Bézier spline interpolator with dynamic
 * pressure/velocity-based stroke width modulation and anti-spike bounding.
 *
 * Key guarantees:
 * - Anti-spike clamping: Control points are bounded within segment chord distance,
 *   preventing sharp angular overshoot during ultra-rapid stylus handwriting.
 * - Centripetal chordal parameterization (alpha = 0.5) preventing self-intersections.
 * - Zero heap allocations in the hot drawing path.
 */
object CatmullRomInterpolator {

    private const val CENTRIPETAL_ALPHA = 0.5f
    private const val EPSILON = 1e-5f

    // ─── Smooth Path Generation ──────────────────────────────────────────────────

    /**
     * Converts raw stylus points into a smooth Android [Path] using Centripetal
     * Catmull-Rom to Cubic Bézier conversion with anti-spike control point clamping.
     */
    fun createSmoothPath(points: List<Point>, targetPath: Path): Path {
        targetPath.reset()
        val count = points.size
        if (count == 0) return targetPath

        if (count == 1) {
            val p = points[0]
            targetPath.moveTo(p.x, p.y)
            targetPath.lineTo(p.x + 0.01f, p.y)
            return targetPath
        }

        if (count == 2) {
            targetPath.moveTo(points[0].x, points[0].y)
            targetPath.lineTo(points[1].x, points[1].y)
            return targetPath
        }

        targetPath.moveTo(points[0].x, points[0].y)

        val isNormalized = if (count > 0) points[0].x <= 1.05f && points[0].y <= 1.05f else false
        val minChord = if (isNormalized) 0.0001f else 0.2f

        for (i in 0 until count - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < count) points[i + 2] else p2

            val chordLen = hypot(p2.x - p1.x, p2.y - p1.y)
            if (chordLen < minChord) {
                targetPath.lineTo(p2.x, p2.y)
                continue
            }

            val d01 = chordalDistance(p0, p1)
            val d12 = chordalDistance(p1, p2)
            val d23 = chordalDistance(p2, p3)

            // Centripetal tangent derivation with Bessel weighting
            val w1 = (2f * d12) / (d01 + d12 + EPSILON)
            val w2 = (2f * d12) / (d12 + d23 + EPSILON)

            var c1x = p1.x + (p2.x - p0.x) / 6f * w1
            var c1y = p1.y + (p2.y - p0.y) / 6f * w1
            var c2x = p2.x - (p3.x - p1.x) / 6f * w2
            var c2y = p2.y - (p3.y - p1.y) / 6f * w2

            // Anti-spike bounding: clamp control point displacement to chord length
            // This prevents wild overshoot spikes during fast, sharp direction changes
            val maxDist = chordLen * 0.9f

            val distC1 = hypot(c1x - p1.x, c1y - p1.y)
            if (distC1 > maxDist && distC1 > EPSILON) {
                val scale = maxDist / distC1
                c1x = p1.x + (c1x - p1.x) * scale
                c1y = p1.y + (c1y - p1.y) * scale
            }

            val distC2 = hypot(c2x - p2.x, c2y - p2.y)
            if (distC2 > maxDist && distC2 > EPSILON) {
                val scale = maxDist / distC2
                c2x = p2.x + (c2x - p2.x) * scale
                c2y = p2.y + (c2y - p2.y) * scale
            }

            targetPath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }

        return targetPath
    }

    /**
     * Appends predicted/in-flight segments to the target path for low-latency preview.
     */
    fun appendPredictedSegment(
        lastCommittedPoint: Point,
        predictedPoints: List<Pair<Float, Float>>,
        targetPath: Path
    ) {
        if (predictedPoints.isEmpty()) return
        targetPath.moveTo(lastCommittedPoint.x, lastCommittedPoint.y)
        for ((px, py) in predictedPoints) {
            targetPath.lineTo(px, py)
        }
    }

    // ─── Dynamic Width Calculation ───────────────────────────────────────────────

    /**
     * Computes dynamic stroke width balancing stylus pressure and drawing velocity.
     */
    fun calculateDynamicWidth(
        baseWidth: Float,
        pressure: Float,
        p1: Point,
        p2: Point,
        previousWidth: Float = baseWidth,
        alpha: Float = 0.65f
    ): Float {
        return calculateDynamicWidth(
            baseWidth = baseWidth,
            pressure = pressure,
            x1 = p1.x,
            y1 = p1.y,
            t1 = p1.timestamp,
            x2 = p2.x,
            y2 = p2.y,
            t2 = p2.timestamp,
            previousWidth = previousWidth,
            alpha = alpha
        )
    }

    /**
     * Zero-allocation scalar overload for dynamic stroke width calculation.
     */
    fun calculateDynamicWidth(
        baseWidth: Float,
        pressure: Float,
        x1: Float,
        y1: Float,
        t1: Long,
        x2: Float,
        y2: Float,
        t2: Long,
        previousWidth: Float = baseWidth,
        alpha: Float = 0.65f
    ): Float {
        val dt = max(1L, t2 - t1).toFloat()
        val distance = hypot(x2 - x1, y2 - y1)
        val velocity = distance / dt

        val normalizedPressure = pressure.coerceIn(0.1f, 1.0f)
        val velocityFactor = 1f / (1f + 0.12f * velocity)

        val rawFactor = (alpha * normalizedPressure) + ((1f - alpha) * velocityFactor)
        val targetWidth = (baseWidth * rawFactor).coerceIn(baseWidth * 0.35f, baseWidth * 1.8f)

        return previousWidth * 0.35f + targetWidth * 0.65f
    }

    /**
     * Calculates the AABB bounding box of a point list with stroke-width padding.
     */
    fun calculateBoundingBox(points: List<Point>, strokeWidth: Float): RectF {
        if (points.isEmpty()) return RectF()
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
        }
        val isNormalized = (maxX <= 1.05f && maxY <= 1.05f && minX >= -0.05f && minY >= -0.05f)
        val pad = if (isNormalized) {
            (strokeWidth / 1080f) * 0.5f + 0.002f
        } else {
            strokeWidth * 0.5f + 2f
        }
        return RectF(minX - pad, minY - pad, maxX + pad, maxY + pad)
    }

    // ─── Internal Math ────────────────────────────────────────────────────────────

    private fun chordalDistance(p0: Point, p1: Point): Float {
        val d = hypot(p1.x - p0.x, p1.y - p0.y)
        return if (d < EPSILON) EPSILON else Math.pow(d.toDouble(), CENTRIPETAL_ALPHA.toDouble()).toFloat()
    }
}
