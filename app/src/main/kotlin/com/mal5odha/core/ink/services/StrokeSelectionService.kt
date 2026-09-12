package com.mal5odha.core.ink.services

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import com.mal5odha.core.ink.models.Point
import com.mal5odha.core.ink.models.Stroke

/**
 * Service responsible for calculating stroke intersections with a lasso selection path
 * using ray-casting point-in-polygon and geometric region hit-testing.
 */
class StrokeSelectionService {

    /**
     * Ray-casting algorithm to test whether point (x, y) is inside a closed polygon.
     */
    fun isPointInPolygon(x: Float, y: Float, polygon: List<Point>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.y > y) != (pj.y > y) &&
                x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Calculates all strokes that have at least one vertex inside the lasso selection loop.
     */
    fun calculateSelectedStrokes(
        selectionPath: Path,
        allStrokes: List<Stroke>,
        polygonPoints: List<Point> = emptyList()
    ): List<Stroke> {
        val selectedStrokes = mutableListOf<Stroke>()
        if (selectionPath.isEmpty || allStrokes.isEmpty()) return selectedStrokes

        val rectF = RectF()
        selectionPath.computeBounds(rectF, true)
        val region = Region()
        region.setPath(
            selectionPath,
            Region(
                rectF.left.toInt() - 2,
                rectF.top.toInt() - 2,
                rectF.right.toInt() + 2,
                rectF.bottom.toInt() + 2
            )
        )

        for (stroke in allStrokes) {
            val strokeBounds = stroke.computeBoundingBox()
            if (!RectF.intersects(rectF, strokeBounds)) continue

            val isInside = if (polygonPoints.size >= 3) {
                stroke.points.any { p ->
                    region.contains(p.x.toInt(), p.y.toInt()) || isPointInPolygon(p.x, p.y, polygonPoints)
                }
            } else {
                stroke.points.any { p -> region.contains(p.x.toInt(), p.y.toInt()) }
            }
            if (isInside) {
                selectedStrokes.add(stroke)
            }
        }
        return selectedStrokes
    }

    fun isPointInSelection(
        x: Float,
        y: Float,
        selectionPath: Path,
        selectedStrokes: List<Stroke>
    ): Boolean {
        if (selectedStrokes.isEmpty() || selectionPath.isEmpty) return false
        val rectF = RectF()
        selectionPath.computeBounds(rectF, true)
        rectF.inset(-24f, -24f)
        return rectF.contains(x, y)
    }
}
