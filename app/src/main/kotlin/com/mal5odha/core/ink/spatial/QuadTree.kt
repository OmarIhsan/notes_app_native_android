package com.mal5odha.core.ink.spatial

import android.graphics.RectF
import com.mal5odha.core.ink.models.Stroke

/**
 * 2D QuadTree spatial indexer for vector ink strokes.
 * Subdivides 2D space into 4 quadrants when a node capacity is exceeded,
 * providing sub-millisecond range queries over thousands of handwritten strokes.
 */
class QuadTree(
    private val bounds: RectF = RectF(0f, 0f, 1f, 1f),
    private val capacity: Int = 16,
    private val maxDepth: Int = 8,
    private val depth: Int = 0
) : SpatialIndex {

    private val entries = mutableListOf<StrokeEntry>()
    private var isDivided = false

    private var northWest: QuadTree? = null
    private var northEast: QuadTree? = null
    private var southWest: QuadTree? = null
    private var southEast: QuadTree? = null

    private data class StrokeEntry(
        val strokeId: String,
        val stroke: Stroke,
        val bounds: RectF
    )

    private fun subdivide() {
        val midX = bounds.centerX()
        val midY = bounds.centerY()

        northWest = QuadTree(RectF(bounds.left, bounds.top, midX, midY), capacity, maxDepth, depth + 1)
        northEast = QuadTree(RectF(midX, bounds.top, bounds.right, midY), capacity, maxDepth, depth + 1)
        southWest = QuadTree(RectF(bounds.left, midY, midX, bounds.bottom), capacity, maxDepth, depth + 1)
        southEast = QuadTree(RectF(midX, midY, bounds.right, bounds.bottom), capacity, maxDepth, depth + 1)

        isDivided = true
    }

    override fun insert(stroke: Stroke) {
        val strokeBounds = stroke.computeBoundingBox()
        if (!RectF.intersects(bounds, strokeBounds)) return

        val entry = StrokeEntry(stroke.id, stroke, strokeBounds)

        if (entries.size < capacity || depth >= maxDepth) {
            entries.add(entry)
            return
        }

        if (!isDivided) {
            subdivide()
        }

        northWest?.insertIfIntersects(entry)
        northEast?.insertIfIntersects(entry)
        southWest?.insertIfIntersects(entry)
        southEast?.insertIfIntersects(entry)
    }

    private fun insertIfIntersects(entry: StrokeEntry) {
        if (RectF.intersects(bounds, entry.bounds)) {
            if (entries.size < capacity || depth >= maxDepth) {
                entries.add(entry)
            } else {
                if (!isDivided) subdivide()
                northWest?.insertIfIntersects(entry)
                northEast?.insertIfIntersects(entry)
                southWest?.insertIfIntersects(entry)
                southEast?.insertIfIntersects(entry)
            }
        }
    }

    override fun query(area: RectF): List<Stroke> {
        val resultSet = mutableSetOf<Stroke>()
        queryInternal(area, resultSet)
        return resultSet.toList()
    }

    private fun queryInternal(area: RectF, results: MutableSet<Stroke>) {
        if (!RectF.intersects(bounds, area)) return

        for (entry in entries) {
            if (RectF.intersects(entry.bounds, area)) {
                results.add(entry.stroke)
            }
        }

        if (isDivided) {
            northWest?.queryInternal(area, results)
            northEast?.queryInternal(area, results)
            southWest?.queryInternal(area, results)
            southEast?.queryInternal(area, results)
        }
    }

    override fun remove(strokeId: String) {
        entries.removeAll { it.strokeId == strokeId }
        if (isDivided) {
            northWest?.remove(strokeId)
            northEast?.remove(strokeId)
            southWest?.remove(strokeId)
            southEast?.remove(strokeId)
        }
    }

    override fun clear() {
        entries.clear()
        northWest = null
        northEast = null
        southWest = null
        southEast = null
        isDivided = false
    }

    override fun size(): Int {
        var count = entries.size
        if (isDivided) {
            count += (northWest?.size() ?: 0) +
                     (northEast?.size() ?: 0) +
                     (southWest?.size() ?: 0) +
                     (southEast?.size() ?: 0)
        }
        return count
    }
}
