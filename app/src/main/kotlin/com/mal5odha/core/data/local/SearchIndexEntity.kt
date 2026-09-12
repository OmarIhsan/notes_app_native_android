package com.mal5odha.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * High-performance full-text search (FTS4) entity for universal document search.
 * Indexes handwriting OCR recognition results, extracted PDF text runs,
 * and user-typed rich text annotations.
 */
@Entity(tableName = "document_search_index")
@Fts4
data class SearchIndexEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int = 0,
    val documentId: String,
    val pageIndex: Int,
    val text: String,
    val sourceType: String, // STROKE_OCR, PDF_TEXT, TEXT_ANNOTATION
    val boundsLeft: Float,
    val boundsTop: Float,
    val boundsRight: Float,
    val boundsBottom: Float
)
