package com.mal5odha.core.ink.spatial

import android.graphics.RectF
import com.mal5odha.core.ink.models.Stroke

/**
 * Interface contract for 2D spatial indexing of vector strokes to enable O(log N) bounding box queries,
 * lasso hit detection, eraser clipping, and viewport frustum culling.
 */
interface SpatialIndex {
    fun insert(stroke: Stroke)
    fun remove(strokeId: String)
    fun query(area: RectF): List<Stroke>
    fun clear()
    fun size(): Int
}
