package com.mal5odha.core.ink.services

import android.graphics.RectF
import com.mal5odha.core.ink.models.Point
import com.mal5odha.core.ink.models.Stroke
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class RecognizedShapeType {
    NONE,
    LINE,
    CIRCLE,
    OVAL,
    RECTANGLE,
    SQUARE,
    TRIANGLE,
    ARROW
}

data class ShapeRecognitionResult(
    val type: RecognizedShapeType,
    val stroke: Stroke,
    val anchorPoint: Point? = null
)

/**
 * Service to analyze raw ink strokes and perfectly snap them into geometric shapes
 * (Lines, Circles, Ovals, Rectangles, Squares, Triangles) for Hold-to-Snap and Shape tool.
 */
class ShapeDetectionService {

    fun detectShape(stroke: Stroke): ShapeRecognitionResult {
        if (stroke.points.size < 4) {
            return ShapeRecognitionResult(RecognizedShapeType.NONE, stroke)
        }

        val bounds = calculateBounds(stroke.points)
        val width = bounds.width()
        val height = bounds.height()

        // 1. Check for Straight Line
        if (isLine(stroke.points)) {
            val perfected = applyLinePerfection(stroke)
            return ShapeRecognitionResult(RecognizedShapeType.LINE, perfected, stroke.points.first())
        }

        // 2. Closed Loop Check
        val startPoint = stroke.points.first()
        val endPoint = stroke.points.last()
        val closureDistance = hypot(startPoint.x - endPoint.x, startPoint.y - endPoint.y)
        val diagonal = hypot(width, height)

        if (closureDistance < diagonal * 0.35f || closureDistance < 80f) {
            // Douglas-Peucker Polygon Corner Analysis
            val epsilon = max(width, height) * 0.12f
            val simplified = douglasPeucker(stroke.points, epsilon)

            // Triangle: 3 dominant corners (+ 1 closure point)
            if (simplified.size in 4..5 && isPolygonClosed(simplified)) {
                // If 3 distinct corner points
                val corners = getDistinctCorners(simplified, 3)
                if (corners.size == 3) {
                    val perfected = applyTrianglePerfection(stroke, corners)
                    return ShapeRecognitionResult(RecognizedShapeType.TRIANGLE, perfected, corners[0])
                }
            }

            // Rectangle / Square: 4 dominant corners (+ 1 closure point)
            if (simplified.size in 5..7 && isPolygonClosed(simplified)) {
                val corners = getDistinctCorners(simplified, 4)
                if (corners.size == 4) {
                    val ratio = width / height.coerceAtLeast(1f)
                    val isSquare = ratio in 0.82f..1.22f
                    val perfected = if (isSquare) {
                        applySquarePerfection(stroke, bounds)
                    } else {
                        applyRectanglePerfection(stroke, bounds)
                    }
                    val type = if (isSquare) RecognizedShapeType.SQUARE else RecognizedShapeType.RECTANGLE
                    return ShapeRecognitionResult(type, perfected, Point(bounds.centerX(), bounds.centerY()))
                }
            }

            // Circle / Oval
            val ratio = width / height.coerceAtLeast(1f)
            val isCircle = ratio in 0.80f..1.25f
            val perfected = if (isCircle) {
                applyCirclePerfection(stroke, bounds)
            } else {
                applyOvalPerfection(stroke, bounds)
            }
            val type = if (isCircle) RecognizedShapeType.CIRCLE else RecognizedShapeType.OVAL
            return ShapeRecognitionResult(type, perfected, Point(bounds.centerX(), bounds.centerY()))
        }

        return ShapeRecognitionResult(RecognizedShapeType.NONE, stroke)
    }

    fun detectAndPerfectShape(stroke: Stroke): Stroke {
        return detectShape(stroke).stroke
    }

    fun perfectCircle(stroke: Stroke): Stroke {
        if (stroke.points.size < 2) return stroke
        val bounds = calculateBounds(stroke.points)
        return applyCirclePerfection(stroke, bounds)
    }

    fun perfectRectangle(stroke: Stroke): Stroke {
        if (stroke.points.size < 2) return stroke
        val bounds = calculateBounds(stroke.points)
        return applyRectanglePerfection(stroke, bounds)
    }

    fun perfectArrow(stroke: Stroke): Stroke {
        if (stroke.points.size < 2) return stroke
        return applyArrowPerfection(stroke)
    }

    fun detectShapeForTool(stroke: Stroke, tool: com.mal5odha.core.ink.models.InkTool): Stroke {
        if (stroke.points.size < 2) return stroke
        return when (tool) {
            com.mal5odha.core.ink.models.InkTool.SHAPE_CIRCLE -> perfectCircle(stroke)
            com.mal5odha.core.ink.models.InkTool.SHAPE_RECTANGLE -> perfectRectangle(stroke)
            com.mal5odha.core.ink.models.InkTool.SHAPE_ARROW -> perfectArrow(stroke)
            com.mal5odha.core.ink.models.InkTool.SHAPE -> detectAndPerfectShape(stroke)
            else -> stroke // Zero shape detection for PEN, HIGHLIGHTER, etc.
        }
    }

    fun applyArrowPerfection(stroke: Stroke): Stroke {
        val start = stroke.points.first()
        val farthestPoint = stroke.points.maxByOrNull { hypot(it.x - start.x, it.y - start.y) } ?: stroke.points.last()
        val end = farthestPoint
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        if (length < 0.0001f) return stroke

        val isNormalized = start.x <= 1.05f && start.y <= 1.05f
        val headLen = if (isNormalized) (length * 0.22f).coerceIn(0.015f, 0.08f) else (length * 0.22f).coerceIn(15f, 60f)
        val angle = atan2(dy, dx)
        val barb1Angle = angle + Math.PI - (Math.PI / 6.0)
        val barb2Angle = angle + Math.PI + (Math.PI / 6.0)

        val barb1X = end.x + (headLen * cos(barb1Angle)).toFloat()
        val barb1Y = end.y + (headLen * sin(barb1Angle)).toFloat()
        val barb2X = end.x + (headLen * cos(barb2Angle)).toFloat()
        val barb2Y = end.y + (headLen * sin(barb2Angle)).toFloat()

        val avgPressure = stroke.points.map { it.pressure }.average().toFloat()
        val perfectedPoints = mutableListOf<Point>()

        // 1. Shaft line (multiple segments for stable Catmull-Rom rendering)
        val shaftSegments = 8
        for (i in 0..shaftSegments) {
            val t = i.toFloat() / shaftSegments
            perfectedPoints.add(
                Point(
                    x = start.x + dx * t,
                    y = start.y + dy * t,
                    pressure = avgPressure
                )
            )
        }

        // 2. Barb 1
        perfectedPoints.add(Point((end.x + barb1X) / 2f, (end.y + barb1Y) / 2f, avgPressure))
        perfectedPoints.add(Point(barb1X, barb1Y, avgPressure))
        perfectedPoints.add(Point((end.x + barb1X) / 2f, (end.y + barb1Y) / 2f, avgPressure))

        // 3. Retrace to arrow tip
        perfectedPoints.add(Point(end.x, end.y, avgPressure))

        // 4. Barb 2
        perfectedPoints.add(Point((end.x + barb2X) / 2f, (end.y + barb2Y) / 2f, avgPressure))
        perfectedPoints.add(Point(barb2X, barb2Y, avgPressure))

        return stroke.copy(points = perfectedPoints)
    }

    /**
     * Manipulates an existing snapped shape dynamically when user drags before lifting.
     */
    fun manipulateSnappedShape(
        original: Stroke,
        type: RecognizedShapeType,
        anchor: Point,
        current: Point
    ): Stroke {
        return when (type) {
            RecognizedShapeType.LINE -> {
                val avgPressure = original.points.map { it.pressure }.average().toFloat()
                val points = mutableListOf<Point>()
                for (i in 0..10) {
                    val t = i / 10f
                    points.add(
                        Point(
                            x = anchor.x + (current.x - anchor.x) * t,
                            y = anchor.y + (current.y - anchor.y) * t,
                            pressure = avgPressure
                        )
                    )
                }
                original.copy(points = points)
            }
            RecognizedShapeType.CIRCLE, RecognizedShapeType.OVAL -> {
                val radius = hypot(current.x - anchor.x, current.y - anchor.y)
                val bounds = if (type == RecognizedShapeType.CIRCLE) {
                    RectF(anchor.x - radius, anchor.y - radius, anchor.x + radius, anchor.y + radius)
                } else {
                    val rx = abs(current.x - anchor.x).coerceAtLeast(10f)
                    val ry = abs(current.y - anchor.y).coerceAtLeast(10f)
                    RectF(anchor.x - rx, anchor.y - ry, anchor.x + rx, anchor.y + ry)
                }
                applyOvalPerfection(original, bounds)
            }
            RecognizedShapeType.RECTANGLE, RecognizedShapeType.SQUARE -> {
                val left = min(anchor.x, current.x)
                val right = max(anchor.x, current.x)
                val top = min(anchor.y, current.y)
                val bottom = max(anchor.y, current.y)
                val bounds = if (type == RecognizedShapeType.SQUARE) {
                    val side = max(right - left, bottom - top)
                    RectF(left, top, left + side, top + side)
                } else {
                    RectF(left, top, right, bottom)
                }
                applyRectanglePerfection(original, bounds)
            }
            RecognizedShapeType.TRIANGLE -> {
                val baseLeft = Point(min(anchor.x, current.x), max(anchor.y, current.y))
                val baseRight = Point(max(anchor.x, current.x), max(anchor.y, current.y))
                val apex = Point((baseLeft.x + baseRight.x) / 2f, min(anchor.y, current.y))
                applyTrianglePerfection(original, listOf(apex, baseRight, baseLeft))
            }
            RecognizedShapeType.ARROW -> {
                val tempStroke = original.copy(points = mutableListOf(anchor, current))
                applyArrowPerfection(tempStroke)
            }
            RecognizedShapeType.NONE -> original
        }
    }

    private fun isLine(points: List<Point>): Boolean {
        if (points.size < 3) return true
        val start = points.first()
        val end = points.last()
        val lineLength = hypot(end.x - start.x, end.y - start.y)
        if (lineLength < 30f) return false

        var maxDeviation = 0f
        for (p in points) {
            val dist = pointLineDistance(p, start, end)
            if (dist > maxDeviation) {
                maxDeviation = dist
            }
        }
        return maxDeviation < max(28f, lineLength * 0.12f)
    }

    private fun applyLinePerfection(stroke: Stroke): Stroke {
        val start = stroke.points.first()
        val end = stroke.points.last()
        val avgPressure = stroke.points.map { it.pressure }.average().toFloat()

        val perfectedPoints = mutableListOf<Point>()
        for (i in 0..10) {
            val t = i / 10f
            perfectedPoints.add(
                Point(
                    x = start.x + (end.x - start.x) * t,
                    y = start.y + (end.y - start.y) * t,
                    pressure = avgPressure,
                    timestamp = start.timestamp + ((end.timestamp - start.timestamp) * t).toLong()
                )
            )
        }
        return stroke.copy(points = perfectedPoints)
    }

    private fun applyCirclePerfection(stroke: Stroke, bounds: RectF): Stroke {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val r = (bounds.width() + bounds.height()) / 4f
        val circleBounds = RectF(cx - r, cy - r, cx + r, cy + r)
        return applyOvalPerfection(stroke, circleBounds)
    }

    private fun applyOvalPerfection(stroke: Stroke, bounds: RectF): Stroke {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val rx = bounds.width() / 2f
        val ry = bounds.height() / 2f
        val avgPressure = stroke.points.map { it.pressure }.average().toFloat()

        val perfectedPoints = mutableListOf<Point>()
        val segments = 48
        for (i in 0..segments) {
            val theta = (i.toFloat() / segments) * 2f * Math.PI
            perfectedPoints.add(
                Point(
                    x = cx + rx * cos(theta).toFloat(),
                    y = cy + ry * sin(theta).toFloat(),
                    pressure = avgPressure
                )
            )
        }
        return stroke.copy(points = perfectedPoints)
    }

    private fun applyRectanglePerfection(stroke: Stroke, bounds: RectF): Stroke {
        val avgPressure = stroke.points.map { it.pressure }.average().toFloat()
        val perfectedPoints = mutableListOf<Point>()

        perfectedPoints.add(Point(bounds.left, bounds.top, avgPressure))
        perfectedPoints.add(Point(bounds.right, bounds.top, avgPressure))
        perfectedPoints.add(Point(bounds.right, bounds.bottom, avgPressure))
        perfectedPoints.add(Point(bounds.left, bounds.bottom, avgPressure))
        perfectedPoints.add(Point(bounds.left, bounds.top, avgPressure))

        return stroke.copy(points = perfectedPoints)
    }

    private fun applySquarePerfection(stroke: Stroke, bounds: RectF): Stroke {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val side = max(bounds.width(), bounds.height()) / 2f
        val squareBounds = RectF(cx - side, cy - side, cx + side, cy + side)
        return applyRectanglePerfection(stroke, squareBounds)
    }

    private fun applyTrianglePerfection(stroke: Stroke, corners: List<Point>): Stroke {
        val avgPressure = stroke.points.map { it.pressure }.average().toFloat()
        val perfectedPoints = mutableListOf<Point>()

        for (i in corners.indices) {
            val p1 = corners[i]
            val p2 = corners[(i + 1) % corners.size]
            for (step in 0..6) {
                val t = step / 6f
                perfectedPoints.add(
                    Point(
                        x = p1.x + (p2.x - p1.x) * t,
                        y = p1.y + (p2.y - p1.y) * t,
                        pressure = avgPressure
                    )
                )
            }
        }
        return stroke.copy(points = perfectedPoints)
    }

    private fun calculateBounds(points: List<Point>): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (p in points) {
            minX = min(minX, p.x)
            minY = min(minY, p.y)
            maxX = max(maxX, p.x)
            maxY = max(maxY, p.y)
        }
        return RectF(minX, minY, maxX, maxY)
    }

    private fun pointLineDistance(p: Point, a: Point, b: Point): Float {
        val normalLength = hypot(b.x - a.x, b.y - a.y)
        if (normalLength < 1e-5f) {
            return hypot(p.x - a.x, p.y - a.y)
        }
        return abs((p.x - a.x) * (b.y - a.y) - (p.y - a.y) * (b.x - a.x)) / normalLength
    }

    private fun isPolygonClosed(points: List<Point>): Boolean {
        if (points.size < 3) return false
        val first = points.first()
        val last = points.last()
        return hypot(first.x - last.x, first.y - last.y) < 80f
    }

    private fun getDistinctCorners(points: List<Point>, targetCount: Int): List<Point> {
        val distinct = mutableListOf<Point>()
        for (p in points) {
            if (distinct.none { hypot(it.x - p.x, it.y - p.y) < 25f }) {
                distinct.add(p)
            }
        }
        return distinct.take(targetCount)
    }

    private fun douglasPeucker(points: List<Point>, epsilon: Float): List<Point> {
        if (points.size < 3) return points

        var dmax = 0f
        var index = 0
        val end = points.size - 1

        for (i in 1 until end) {
            val d = pointLineDistance(points[i], points[0], points[end])
            if (d > dmax) {
                index = i
                dmax = d
            }
        }

        return if (dmax > epsilon) {
            val recResults1 = douglasPeucker(points.subList(0, index + 1), epsilon)
            val recResults2 = douglasPeucker(points.subList(index, end + 1), epsilon)
            val result = mutableListOf<Point>()
            result.addAll(recResults1.dropLast(1))
            result.addAll(recResults2)
            result
        } else {
            listOf(points[0], points[end])
        }
    }

    /**
     * Detects horizontal or near-horizontal sweep trajectories (slope deviation < 15°, length > 40 dp).
     * Snaps the drawn highlighter/pen stroke into a clean rectangular text highlight ribbon.
     */
    fun detectHorizontalHighlightOrUnderline(stroke: Stroke): Stroke? {
        if (stroke.points.size < 4) return null
        val start = stroke.points.first()
        val end = stroke.points.last()
        val dx = abs(end.x - start.x)
        val dy = abs(end.y - start.y)
        val length = hypot(dx, dy)

        // Strict heuristics: minimum length > 90px (~40dp) to prevent capturing hyphens/minus signs
        if (length < 90f) return null

        // Slope deviation < 15 degrees
        val angleRad = atan2(dy, dx)
        if (angleRad > Math.toRadians(15.0)) return null

        // Linearity check
        if (!isLine(stroke.points)) return null

        val avgY = (start.y + end.y) / 2f
        val avgPressure = stroke.points.map { it.pressure }.average().toFloat()
        val minX = min(start.x, end.x)
        val maxX = max(start.x, end.x)

        val snappedPoints = mutableListOf<Point>()
        val count = 12
        for (i in 0..count) {
            val t = i / count.toFloat()
            val x = minX + (maxX - minX) * t
            snappedPoints.add(Point(x, avgY, avgPressure))
        }

        val targetWidth = if (stroke.tool == com.mal5odha.core.ink.models.InkTool.HIGHLIGHTER) {
            max(stroke.width, 26f) // Standard text line highlight ribbon height
        } else {
            stroke.width
        }

        return stroke.copy(
            points = snappedPoints,
            width = targetWidth
        )
    }

    /**
     * Detects freehand loop/circle-to-select gestures.
     * Returns the bounding box if the stroke forms a grouping lasso loop.
     */
    fun detectCircleLoopGesture(points: List<Point>): RectF? {
        if (points.size < 8) return null
        val bounds = calculateBounds(points)
        val width = bounds.width()
        val height = bounds.height()

        // Minimum area > 3500 px^2 prevents capturing cursive letters like 'o', 'e', 'a'
        val area = width * height
        if (area < 3500f) return null

        // Aspect ratio check: between 0.35 and 2.8
        val ratio = width / height.coerceAtLeast(1f)
        if (ratio < 0.35f || ratio > 2.8f) return null

        // Closure check: distance between start and end < 45% of diagonal
        val start = points.first()
        val end = points.last()
        val closureDist = hypot(start.x - end.x, start.y - end.y)
        val diag = hypot(width, height)
        if (closureDist > diag * 0.45f) return null

        // Total angular sweep around center must be >= 5.0 radians (~286°)
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        var totalAngle = 0.0
        var prevAngle = atan2((points[0].y - cy).toDouble(), (points[0].x - cx).toDouble())

        for (i in 1 until points.size) {
            val currAngle = atan2((points[i].y - cy).toDouble(), (points[i].x - cx).toDouble())
            var diff = currAngle - prevAngle
            while (diff > Math.PI) diff -= 2 * Math.PI
            while (diff < -Math.PI) diff += 2 * Math.PI
            totalAngle += diff
            prevAngle = currAngle
        }

        if (abs(totalAngle) < 5.0) return null

        return bounds
    }

    /**
     * Detects bracket gestures '[' or ']' used to select adjacent paragraphs and annotations.
     * Returns the selection bounding box if detected.
     */
    fun detectBracketGesture(points: List<Point>): RectF? {
        if (points.size < 6) return null
        val bounds = calculateBounds(points)
        val width = bounds.width()
        val height = bounds.height()

        // Brackets are predominantly vertical: height must be significantly larger than width
        if (height < 90f || height < width * 1.2f) return null

        val epsilon = height * 0.12f
        val simplified = douglasPeucker(points, epsilon)
        if (simplified.size !in 4..6) return null

        val pStart = simplified.first()
        val pEnd = simplified.last()

        val startDirX = simplified[1].x - pStart.x
        val endDirX = pEnd.x - simplified[simplified.size - 2].x

        if (abs(startDirX) > 12f && abs(endDirX) > 12f) {
            val isSameDir = (startDirX > 0 && endDirX > 0) || (startDirX < 0 && endDirX < 0)
            if (isSameDir) {
                val expandWidth = max(width * 2.5f, 250f)
                return if (startDirX > 0) {
                    RectF(bounds.left, bounds.top - 10f, bounds.left + expandWidth, bounds.bottom + 10f)
                } else {
                    RectF(bounds.right - expandWidth, bounds.top - 10f, bounds.right, bounds.bottom + 10f)
                }
            }
        }

        return null
    }
}
