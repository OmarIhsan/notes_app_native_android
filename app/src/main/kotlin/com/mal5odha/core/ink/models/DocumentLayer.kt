package com.mal5odha.core.ink.models

import java.util.UUID

/**
 * Represents an isolated vector layer within a document page.
 * Conforms to Notewise and Adobe Photoshop multi-tier vector standards:
 * - Independent visibility toggling
 * - Padlock locking to protect inking from accidental erasure
 * - Layer-specific opacity / alpha compositing
 */
data class DocumentLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val alpha: Float = 1.0f
) {
    companion object {
        const val LAYER_HIGHLIGHTER_ID = "layer_highlighter"
        const val LAYER_PEN_ID = "layer_pen"
        const val LAYER_ANNOTATIONS_ID = "layer_annotations"

        fun defaultLayers(): List<DocumentLayer> = listOf(
            DocumentLayer(id = LAYER_HIGHLIGHTER_ID, name = "Highlighter", alpha = 0.85f),
            DocumentLayer(id = LAYER_PEN_ID, name = "Pen Inking", alpha = 1.0f),
            DocumentLayer(id = LAYER_ANNOTATIONS_ID, name = "Text & Media", alpha = 1.0f)
        )
    }
}
