package com.mal5odha.core.data.repository

import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.models.Ml5Stroke

interface NoteFileSystemRepository {
    suspend fun saveNotePages(documentId: String, pages: List<Ml5Page>)
    suspend fun loadNotePages(documentId: String): List<Ml5Page>
    suspend fun savePageStrokes(documentId: String, pageId: String, strokes: List<Ml5Stroke>)
    suspend fun loadPageStrokes(documentId: String, pageId: String): List<Ml5Stroke>
    suspend fun savePageTextAnnotations(
            documentId: String,
            pageId: String,
            annotations: List<com.mal5odha.core.data.models.Ml5TextAnnotation>
    )
    suspend fun loadPageTextAnnotations(
            documentId: String,
            pageId: String
    ): List<com.mal5odha.core.data.models.Ml5TextAnnotation>
    suspend fun savePageMediaAnnotations(
            documentId: String,
            pageId: String,
            annotations: List<com.mal5odha.core.data.models.Ml5MediaAnnotation>
    )
    suspend fun loadPageMediaAnnotations(
            documentId: String,
            pageId: String
    ): List<com.mal5odha.core.data.models.Ml5MediaAnnotation>
    suspend fun copyUriToNoteDir(documentId: String, uri: android.net.Uri): String
    suspend fun deleteNoteFile(documentId: String)
    /** Deletes all physical note files for a given document (strokes, pages, texts). */
    suspend fun purgeNoteStructure(documentId: String)
    /** Deletes stroke, text, and media files for a specific page ID. */
    suspend fun deletePageData(documentId: String, pageId: String)
    /** Deep copies all physical note files (strokes, pages, media, text) to target document. */
    suspend fun duplicateNoteDirectory(sourceDocId: String, targetDocId: String)
    /** Computes total disk usage of the note directory in bytes. */
    suspend fun getNoteDirectorySizeBytes(documentId: String): Long
}
