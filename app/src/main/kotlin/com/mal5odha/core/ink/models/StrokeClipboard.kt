package com.mal5odha.core.ink.models

/**
 * Shared in-memory clipboard for vector stroke Cut, Copy, and Paste actions across pages and selections.
 */
object StrokeClipboard {
    var copiedStrokes: List<Stroke> = emptyList()
    val hasContent: Boolean get() = copiedStrokes.isNotEmpty()

    fun copy(strokes: List<Stroke>) {
        copiedStrokes = strokes.map { it.copy(points = it.points.map { p -> p.copy() }.toMutableList()) }
    }

    fun clear() {
        copiedStrokes = emptyList()
    }
}
