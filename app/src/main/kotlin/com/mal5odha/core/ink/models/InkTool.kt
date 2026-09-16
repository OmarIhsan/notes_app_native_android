package com.mal5odha.core.ink.models

enum class InkTool {
    PEN,
    HIGHLIGHTER,
    ERASER,
    LASSO,
    TEXT,
    STICKY_CARD,
    STICKY_NOTE,
    SHAPE,
    SHAPE_CIRCLE,
    SHAPE_RECTANGLE,
    SHAPE_ARROW,
    LASER;

    val isStickyCard: Boolean get() = this == STICKY_CARD || this == STICKY_NOTE
    val isText: Boolean get() = this == TEXT
    val isPlacementTool: Boolean get() = this == TEXT || this == STICKY_CARD || this == STICKY_NOTE

    fun matchesAnnotation(isCard: Boolean): Boolean = when {
        isCard -> isStickyCard
        else -> isText
    }
}
