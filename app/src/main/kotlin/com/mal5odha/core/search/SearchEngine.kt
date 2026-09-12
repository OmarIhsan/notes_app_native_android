package com.mal5odha.core.search

import android.graphics.RectF
import com.mal5odha.core.data.local.SearchDao
import com.mal5odha.core.data.local.SearchIndexEntity
import com.mal5odha.core.ink.models.TextAnnotation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchResultItem(
    val documentId: String,
    val pageIndex: Int,
    val matchedText: String,
    val bounds: RectF,
    val sourceType: String
)

/**
 * Universal Tri-Layer Document Search Engine querying:
 * 1. Offline Handwriting OCR (ML Kit Digital Ink)
 * 2. Movable Text Annotations
 * 3. Extracted PDF Text Runs
 */
@Singleton
class SearchEngine @Inject constructor(
    private val searchDao: SearchDao
) {

    /**
     * Executes high-speed full-text search across all content layers within a document.
     */
    suspend fun searchDocument(documentId: String, query: String): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) return@withContext emptyList()

            // FTS wildcard matching query: "query*"
            val ftsQuery = if (cleanQuery.endsWith("*")) cleanQuery else "$cleanQuery*"

            val entities = try {
                searchDao.searchInDocument(documentId, ftsQuery)
            } catch (e: Exception) {
                // Fallback to exact match if FTS syntax error
                searchDao.searchInDocument(documentId, cleanQuery)
            }

            entities.map { entity ->
                SearchResultItem(
                    documentId = entity.documentId,
                    pageIndex = entity.pageIndex,
                    matchedText = entity.text,
                    bounds = RectF(
                        entity.boundsLeft,
                        entity.boundsTop,
                        entity.boundsRight,
                        entity.boundsBottom
                    ),
                    sourceType = entity.sourceType
                )
            }
        }

    /**
     * Indexes or refreshes movable typed text annotations for a page.
     */
    suspend fun indexTextAnnotations(
        documentId: String,
        pageIndex: Int,
        annotations: List<TextAnnotation>
    ) = withContext(Dispatchers.IO) {
        searchDao.clearPageIndices(documentId, pageIndex, "TEXT_ANNOTATION")

        val entities = annotations
            .filter { it.text.isNotBlank() }
            .map { annot ->
                SearchIndexEntity(
                    documentId = documentId,
                    pageIndex = pageIndex,
                    text = annot.text,
                    sourceType = "TEXT_ANNOTATION",
                    boundsLeft = annot.x,
                    boundsTop = annot.y,
                    boundsRight = annot.x + annot.width,
                    boundsBottom = annot.y + annot.height
                )
            }

        if (entities.isNotEmpty()) {
            searchDao.insertIndices(entities)
        }
    }

    /**
     * Indexes extracted PDF text blocks for a page.
     */
    suspend fun indexPdfTextRuns(
        documentId: String,
        pageIndex: Int,
        textRuns: List<Pair<String, RectF>>
    ) = withContext(Dispatchers.IO) {
        searchDao.clearPageIndices(documentId, pageIndex, "PDF_TEXT")

        val entities = textRuns
            .filter { it.first.isNotBlank() }
            .map { (text, bounds) ->
                SearchIndexEntity(
                    documentId = documentId,
                    pageIndex = pageIndex,
                    text = text,
                    sourceType = "PDF_TEXT",
                    boundsLeft = bounds.left,
                    boundsTop = bounds.top,
                    boundsRight = bounds.right,
                    boundsBottom = bounds.bottom
                )
            }

        if (entities.isNotEmpty()) {
            searchDao.insertIndices(entities)
        }
    }

    /**
     * Removes all index entries for a deleted document.
     */
    suspend fun clearDocumentIndex(documentId: String) = withContext(Dispatchers.IO) {
        searchDao.clearDocumentIndices(documentId)
    }
}
