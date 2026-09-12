package com.mal5odha.core.data.services

import com.mal5odha.core.data.repository.NoteFileSystemRepository
import com.mal5odha.core.data.models.Ml5Page
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PageManagerService {
    suspend fun createNoteStructure(documentId: String)
    suspend fun addPage(documentId: String)
    suspend fun duplicatePage(documentId: String, pageId: String)
    suspend fun reorderPages(documentId: String, newOrder: List<String>)
    suspend fun deletePage(documentId: String, pageId: String)
    suspend fun purgeNoteStructure(documentId: String)
}

@Singleton
class PageManagerServiceImpl @Inject constructor(
    private val fileSystemRepository: NoteFileSystemRepository
) : PageManagerService {

    override suspend fun createNoteStructure(documentId: String) {
        // Only initialize if no pages exist yet.
        // This prevents overwriting a caller-provided page setup (e.g. from DashboardViewModel
        // which writes the user-chosen paper type BEFORE saveDocument is called).
        val existing = fileSystemRepository.loadNotePages(documentId)
        if (existing.isEmpty()) {
            val initialPages = listOf(
                Ml5Page(
                    id = UUID.randomUUID().toString(),
                    orderIndex = 0,
                    backgroundType = "BLANK"
                )
            )
            fileSystemRepository.saveNotePages(documentId, initialPages)
        }
    }

    override suspend fun addPage(documentId: String) {
        val existingPages = fileSystemRepository.loadNotePages(documentId).toMutableList()
        val newPage = Ml5Page(
            id = UUID.randomUUID().toString(),
            orderIndex = existingPages.size,
            backgroundType = "BLANK"
        )
        existingPages.add(newPage)
        fileSystemRepository.saveNotePages(documentId, existingPages)
    }

    override suspend fun duplicatePage(documentId: String, pageId: String) {
        val pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
        val source = pages.firstOrNull { it.id == pageId } ?: return
        val newPageId = UUID.randomUUID().toString()
        val newPage = source.copy(id = newPageId, orderIndex = pages.size)
        pages.add(newPage)
        fileSystemRepository.saveNotePages(documentId, pages)

        // Sync copy stroke data, text annotation data, and media annotations
        val strokes = fileSystemRepository.loadPageStrokes(documentId, pageId)
        if (strokes.isNotEmpty()) {
            fileSystemRepository.savePageStrokes(documentId, newPageId, strokes)
        }
        val texts = fileSystemRepository.loadPageTextAnnotations(documentId, pageId)
        if (texts.isNotEmpty()) {
            fileSystemRepository.savePageTextAnnotations(documentId, newPageId, texts)
        }
        val media = fileSystemRepository.loadPageMediaAnnotations(documentId, pageId)
        if (media.isNotEmpty()) {
            fileSystemRepository.savePageMediaAnnotations(documentId, newPageId, media)
        }
    }

    override suspend fun reorderPages(documentId: String, newOrder: List<String>) {
        val existingPages = fileSystemRepository.loadNotePages(documentId)
        // Sort by desired order and re-assign orderIndex to reflect new positions
        val reordered = existingPages
            .sortedBy { page -> newOrder.indexOf(page.id).takeIf { it != -1 } ?: Int.MAX_VALUE }
            .mapIndexed { index, page -> page.copy(orderIndex = index) }
        fileSystemRepository.saveNotePages(documentId, reordered)
    }

    override suspend fun deletePage(documentId: String, pageId: String) {
        val pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
        val pageToRemove = pages.firstOrNull { it.id == pageId } ?: return
        pages.remove(pageToRemove)
        // Re-index remaining pages
        val reindexed = pages.mapIndexed { i, p -> p.copy(orderIndex = i) }
        fileSystemRepository.saveNotePages(documentId, reindexed)
        // Delete stroke and text files for the removed page
        fileSystemRepository.deletePageData(documentId, pageId)
    }

    override suspend fun purgeNoteStructure(documentId: String) {
        fileSystemRepository.purgeNoteStructure(documentId)
    }
}
