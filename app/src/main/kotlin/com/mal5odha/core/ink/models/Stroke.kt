package com.mal5odha.core.ink.models

import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import com.mal5odha.core.ink.math.CatmullRomInterpolator

data class Point(var x: Float, var y: Float, val pressure: Float = 1f, val timestamp: Long = System.currentTimeMillis())

data class Stroke(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: MutableList<Point> = mutableListOf(),
    val color: Int = Color.BLACK,
    val width: Float = 5f,
    val tool: InkTool = InkTool.PEN,
    val audioSessionId: String? = null,
    val audioTimestampMs: Long = 0L,
    val timestampMs: Long = audioTimestampMs,
    val layerId: String? = null,
    var isActivelyPlaying: Boolean = false
) {
    // Hardware-accelerated pre-baked vector path in normalized [0..1] coordinates
    @Transient
    var cachedNormalizedPath: Path? = null
        private set

    @Transient
    var cachedBoundingBox: RectF? = null
        private set

    /**
     * Bakes points into a permanent normalized path and bounding box.
     * Called once when the stroke is committed on ACTION_UP.
     */
    fun precomputePathGeometry() {
        val path = Path()
        CatmullRomInterpolator.createSmoothPath(points, path)
        cachedNormalizedPath = path
        cachedBoundingBox = CatmullRomInterpolator.calculateBoundingBox(points, width)
    }

    fun computeBoundingBox(): RectF {
        val cached = cachedBoundingBox
        if (cached != null) return cached
        val calculated = CatmullRomInterpolator.calculateBoundingBox(points, width)
        cachedBoundingBox = calculated
        return calculated
    }
}
