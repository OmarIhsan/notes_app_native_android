package com.mal5odha.core.ink.laser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Visual mode of the presentation laser pointer.
 */
enum class LaserMode {
    /**
     * Continuous comet-tail with progressive dissolution over [LaserConfig.durationMs].
     */
    TRAIL,

    /**
     * Stationary spotlight reticle beneath the stylus/touch tip, active only while pressed.
     */
    DOT
}

/**
 * Timestamped point for the laser pointer stream.
 */
data class LaserPoint(
    val offset: Offset,
    val timestampMs: Long,
    val pressure: Float = 1.0f
) {
    val x: Float get() = offset.x
    val y: Float get() = offset.y

    constructor(x: Float, y: Float, timestampMs: Long, pressure: Float = 1.0f) :
        this(Offset(x, y), timestampMs, pressure)
}

/**
 * Isolated stroke segment representing a single contiguous touch-down sequence.
 * Enforces strict boundary isolation to prevent polyline bridging across distinct gestures.
 */
data class LaserStrokeSegment(
    val id: Long = System.currentTimeMillis(),
    val points: MutableList<LaserPoint> = mutableListOf()
)

/**
 * Runtime configuration for the Laser Pointer engine.
 */
data class LaserConfig(
    val mode: LaserMode = LaserMode.TRAIL,
    val colorHex: Long = 0xFFFF1744L, // Default: Neon Electric Red
    val durationMs: Long = 1400L
) {
    val composeColor: Color
        get() = Color(colorHex)
}

/**
 * Visual constants and palette presets for the Laser Pointer.
 */
object LaserConstants {
    // Preset Color Palette
    const val NEON_CRIMSON_HEX: Long = 0xFFFF1744L
    const val EMERALD_LASER_HEX: Long = 0xFF00E676L
    const val ELECTRIC_CYAN_HEX: Long = 0xFF00E5FFL
    const val SUNBURST_AMBER_HEX: Long = 0xFFFFAB00L

    val PALETTE_HEX = listOf(
        NEON_CRIMSON_HEX,
        EMERALD_LASER_HEX,
        ELECTRIC_CYAN_HEX,
        SUNBURST_AMBER_HEX
    )

    // Comet-Tail Geometric & Shading Metrics (2x Sizing)
    const val TRAIL_OUTER_AURA_WIDTH_DP: Float = 20f
    const val TRAIL_INNER_CORE_WIDTH_DP: Float = 7f
    const val TRAIL_MAX_AURA_ALPHA: Float = 0.45f
    const val TRAIL_HEAD_WIDTH_SCALE: Float = 1.0f
    const val TRAIL_TAIL_WIDTH_SCALE: Float = 0.2f
    val TRAIL_CORE_COLOR = Color(0xFFFFF8E1)

    // Spotlight Dot Metrics (2x Sizing)
    const val DOT_AURA_RADIUS_DP: Float = 28f
    const val DOT_CORE_RADIUS_DP: Float = 10f
    const val DOT_AURA_ALPHA: Float = 0.40f

    // Configurable Decay Durations (2x Persistence)
    const val DURATION_FAST_MS: Long = 800L
    const val DURATION_DEFAULT_MS: Long = 1400L
    const val DURATION_LONG_MS: Long = 2400L

    val DURATION_OPTIONS = listOf(
        DURATION_FAST_MS,
        DURATION_DEFAULT_MS,
        DURATION_LONG_MS
    )
}
