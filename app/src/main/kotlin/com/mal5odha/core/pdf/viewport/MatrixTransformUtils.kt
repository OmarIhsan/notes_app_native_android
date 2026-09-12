package com.mal5odha.core.pdf.viewport

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.mal5odha.core.ink.models.Point
import com.mal5odha.core.ink.models.Stroke

/**
 * Utility functions for transforming vectors, points, and strokes between screen and document space.
 */
object MatrixTransformUtils {

    /**
     * Transforms a stroke's coordinates using the specified matrix.
     */
    fun transformStroke(stroke: Stroke, matrix: Matrix): Stroke {
        val pts = FloatArray(stroke.points.size * 2)
        stroke.points.forEachIndexed { index, p ->
            pts[index * 2] = p.x
            pts[index * 2 + 1] = p.y
        }

        matrix.mapPoints(pts)

        val transformedPoints = stroke.points.mapIndexed { index, p ->
            Point(
                x = pts[index * 2],
                y = pts[index * 2 + 1],
                pressure = p.pressure,
                timestamp = p.timestamp
            )
        }.toMutableList()

        return stroke.copy(points = transformedPoints)
    }

    /**
     * Transforms a point from screen coordinates to document coordinates.
     */
    fun mapPoint(point: Point, matrix: Matrix): Point {
        val pts = floatArrayOf(point.x, point.y)
        matrix.mapPoints(pts)
        return Point(
            x = pts[0],
            y = pts[1],
            pressure = point.pressure,
            timestamp = point.timestamp
        )
    }

    /**
     * Transforms an Axis-Aligned Bounding Box using the specified matrix.
     */
    fun mapRect(rect: RectF, matrix: Matrix): RectF {
        val result = RectF()
        matrix.mapRect(result, rect)
        return result
    }
}
