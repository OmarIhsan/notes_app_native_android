package com.mal5odha.core.ink.models

import java.util.UUID

/**
 * Text annotation bounded in normalized page space ([0.0, 1.0]).
 * Supports rich font scaling, rotation, container background fills, and alignment.
 */
data class TextAnnotation(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var x: Float = 0.1f,           // Normalized [0.0..1.0] top-left X
    var y: Float = 0.1f,           // Normalized [0.0..1.0] top-left Y
    var width: Float = 0.45f,      // Normalized [0.0..1.0] width
    var height: Float = 0.12f,     // Normalized [0.0..1.0] height
    var fontSize: Float = 18f,     // Point / SP size
    var color: Int = android.graphics.Color.BLACK,
    var rotation: Float = 0f,      // Rotation in degrees
    var backgroundColor: Int? = null, // Optional container fill (e.g. Sticky note)
    var alignment: String = "START",   // START, CENTER, END
    var audioSessionId: String? = null,
    var audioTimestampMs: Long = 0L
)
