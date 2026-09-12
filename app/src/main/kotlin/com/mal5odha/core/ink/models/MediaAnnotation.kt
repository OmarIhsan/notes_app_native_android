package com.mal5odha.core.ink.models

import java.util.UUID

/**
 * Image / Sticker media annotation anchored in normalized page coordinates ([0.0, 1.0]).
 * Supports 2D affine transformations (drag, scale, rotate) and aspect ratio persistence.
 */
data class MediaAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val localPath: String,
    var x: Float = 0.2f,          // Normalized [0.0..1.0] top-left X
    var y: Float = 0.2f,          // Normalized [0.0..1.0] top-left Y
    var width: Float = 0.45f,     // Normalized [0.0..1.0] width
    var height: Float = 0.35f,    // Normalized [0.0..1.0] height
    var rotation: Float = 0f      // Rotation in degrees
)
