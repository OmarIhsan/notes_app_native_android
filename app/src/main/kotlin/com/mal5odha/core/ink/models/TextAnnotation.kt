package com.mal5odha.core.ink.models

import androidx.compose.ui.text.style.TextAlign
import java.util.UUID

/**
 * Text annotation bounded in normalized page space ([0.0, 1.0]).
 * Follows the Goodnotes/Notewise floating card paradigm with dynamic auto-expanding height,
 * bounded word wrapping, stationary opposite-anchor transforms, and rich formatting.
 */
data class TextAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val pageId: String = "",
    val xNorm: Float = 0.1f,
    val yNorm: Float = 0.1f,
    val widthNorm: Float = 0.45f,
    val heightNorm: Float = 0f, // 0f indicates intrinsic wrap_content
    val content: String = "",
    val fontSizeSp: Float = 16f,
    val colorHex: Long = 0xFF000000L,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val textAlign: TextAlign = TextAlign.Start,
    val rotation: Float = 0f,
    val backgroundColor: Int? = null,
    var audioSessionId: String? = null,
    var audioTimestampMs: Long = 0L
) {
    // ─── Backwards Compatibility Properties ──────────────────────────────────
    val text: String get() = content
    val x: Float get() = xNorm
    val y: Float get() = yNorm
    val width: Float get() = widthNorm
    val height: Float get() = heightNorm
    val fontSize: Float get() = fontSizeSp
    val color: Int get() = colorHex.toInt()
    val alignment: String get() = when (textAlign) {
        TextAlign.Center -> "CENTER"
        TextAlign.End, TextAlign.Right -> "END"
        else -> "START"
    }

    // ─── Legacy Constructor Support ──────────────────────────────────────────
    constructor(
        id: String = UUID.randomUUID().toString(),
        text: String = "",
        x: Float = 0.1f,
        y: Float = 0.1f,
        width: Float = 0.45f,
        height: Float = 0.12f,
        fontSize: Float = 18f,
        color: Int = android.graphics.Color.BLACK,
        rotation: Float = 0f,
        backgroundColor: Int? = null,
        alignment: String = "START",
        audioSessionId: String? = null,
        audioTimestampMs: Long = 0L
    ) : this(
        id = id,
        pageId = "",
        xNorm = x,
        yNorm = y,
        widthNorm = width,
        heightNorm = height,
        content = text,
        fontSizeSp = fontSize,
        colorHex = (color.toLong() and 0xFFFFFFFFL),
        isBold = false,
        isItalic = false,
        isUnderline = false,
        textAlign = when (alignment) {
            "CENTER" -> TextAlign.Center
            "END" -> TextAlign.End
            else -> TextAlign.Start
        },
        rotation = rotation,
        backgroundColor = backgroundColor,
        audioSessionId = audioSessionId,
        audioTimestampMs = audioTimestampMs
    )
}
