package com.mal5odha.core.ink.models

/**
 * Operating mode for the eraser tool:
 * - STROKE: Removes the entire vector stroke on collision with eraser boundary (fast QuadTree query).
 * - PRECISION: Erases pixels/segments at exact touch intersection path.
 */
enum class EraserMode {
    STROKE,
    PRECISION
}

/**
 * Layer filtering target for the eraser:
 * - ALL: Erases all unlocked strokes across all layers.
 * - ACTIVE_LAYER_ONLY: Erases only strokes on the currently active layer.
 * - HIGHLIGHTER_ONLY: Erases highlighter strokes, leaving pen inking intact.
 */
enum class EraserTarget {
    ALL,
    ACTIVE_LAYER_ONLY,
    HIGHLIGHTER_ONLY
}
