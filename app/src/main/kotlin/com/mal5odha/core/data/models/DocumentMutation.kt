package com.mal5odha.core.data.models

import java.util.UUID

/**
 * Atomic mutation types recorded into the append-only document journal.
 */
enum class MutationType {
    ADD_STROKE,
    REMOVE_STROKE,
    ADD_TEXT,
    REMOVE_TEXT,
    CLEAR_PAGE,
    ADD_PAGE,
    DELETE_PAGE
}

/**
 * Represents an immutable, serializable atomic operation performed on a document.
 * Used for delta sync, point-in-time time-travel recovery, and non-destructive version history.
 */
data class DocumentMutation(
    val id: String = UUID.randomUUID().toString(),
    val type: MutationType,
    val pageId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stroke: Ml5Stroke? = null,
    val strokeId: String? = null,
    val textAnnotation: Ml5TextAnnotation? = null,
    val textId: String? = null,
    val description: String = ""
)
