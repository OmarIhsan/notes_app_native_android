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
    var audioTimestampMs: Long = 0L,
    val isCard: Boolean = false,
    val title: String = "",
    val isPinned: Boolean = false
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
        audioTimestampMs = audioTimestampMs,
        isCard = false,
        title = "",
        isPinned = false
    )

    companion object {
        fun createStickyCard(
            pageId: String,
            xNorm: Float,
            yNorm: Float,
            widthNorm: Float = 0.32f,
            title: String = "",
            body: String = "",
            cardColorHex: Long = 0xFFFFF9C4L,
            isPinned: Boolean = false,
            textAlign: TextAlign = TextAlign.Start
        ): TextAnnotation = TextAnnotation(
            id = UUID.randomUUID().toString(),
            pageId = pageId,
            xNorm = xNorm,
            yNorm = yNorm,
            widthNorm = widthNorm,
            heightNorm = 0f,
            content = body,
            title = title,
            fontSizeSp = 14f,
            colorHex = 0xFF1C1B1FL,
            backgroundColor = cardColorHex.toInt(),
            textAlign = textAlign,
            isCard = true,
            isPinned = isPinned
        )
    }
}

/**
 * Minimal Title + Body "Sticky Card" Sticker metadata model.
 */
data class StickyCardAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val pageId: String = "",
    val xNorm: Float = 0.1f,
    val yNorm: Float = 0.1f,
    val widthNorm: Float = 0.32f, // Default card width (~240dp - 280dp)
    val title: String = "",
    val body: String = "",
    val cardColorHex: Long = 0xFFFFF9C4L, // Default: Pastel Post-it Yellow
    val isPinned: Boolean = false,
    val alignment: String = "START"
)

fun StickyCardAnnotation.toTextAnnotation(): TextAnnotation = TextAnnotation(
    id = id,
    pageId = pageId,
    xNorm = xNorm,
    yNorm = yNorm,
    widthNorm = widthNorm,
    heightNorm = 0f,
    content = body,
    title = title,
    fontSizeSp = 14f,
    colorHex = 0xFF1C1B1FL,
    backgroundColor = cardColorHex.toInt(),
    isCard = true,
    isPinned = isPinned,
    textAlign = when (alignment) {
        "CENTER" -> TextAlign.Center
        "END" -> TextAlign.End
        else -> TextAlign.Start
    }
)

fun TextAnnotation.toStickyCardAnnotation(): StickyCardAnnotation = StickyCardAnnotation(
    id = id,
    pageId = pageId,
    xNorm = xNorm,
    yNorm = yNorm,
    widthNorm = widthNorm,
    title = title,
    body = content,
    cardColorHex = (backgroundColor?.toLong()?.and(0xFFFFFFFFL)) ?: 0xFFFFF9C4L,
    isPinned = isPinned,
    alignment = alignment
)
