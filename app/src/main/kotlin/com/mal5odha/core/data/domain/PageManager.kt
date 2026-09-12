package com.mal5odha.core.data.domain

import com.mal5odha.core.data.models.DocumentPage
import com.mal5odha.core.data.models.PageBackground
import com.mal5odha.core.data.models.UnifiedDocument
import java.util.UUID
import javax.inject.Inject

class PageManager @Inject constructor() {

    fun addPage(document: UnifiedDocument, index: Int = document.pages.size, background: PageBackground = PageBackground.BLANK): UnifiedDocument {
        val newPage = DocumentPage(
            id = UUID.randomUUID().toString(),
            order = index,
            background = background
        )
        val updatedPages = document.pages.toMutableList()
        updatedPages.add(index, newPage)
        
        // Re-calculate ordering
        val reorderedPages = updatedPages.mapIndexed { idx, page ->
            page.copy(order = idx)
        }

        return document.copy(
            pages = reorderedPages,
            currentPageIndex = index,
            modifiedAt = System.currentTimeMillis()
        )
    }

    fun deletePage(document: UnifiedDocument, index: Int): UnifiedDocument {
        if (index < 0 || index >= document.pages.size) return document
        if (document.pages.size <= 1) return document // Prevent deleting the last page

        val updatedPages = document.pages.toMutableList()
        updatedPages.removeAt(index)

        // Re-calculate ordering
        val reorderedPages = updatedPages.mapIndexed { idx, page ->
            page.copy(order = idx)
        }
        
        val newCurrentIndex = if (index >= reorderedPages.size) reorderedPages.size - 1 else index

        return document.copy(
            pages = reorderedPages,
            currentPageIndex = newCurrentIndex,
            modifiedAt = System.currentTimeMillis()
        )
    }

    fun duplicatePage(document: UnifiedDocument, index: Int): UnifiedDocument {
        if (index < 0 || index >= document.pages.size) return document
        
        val pageToDuplicate = document.pages[index]
        val newPage = pageToDuplicate.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        
        val updatedPages = document.pages.toMutableList()
        updatedPages.add(index + 1, newPage)

        val reorderedPages = updatedPages.mapIndexed { idx, page ->
            page.copy(order = idx)
        }

        return document.copy(
            pages = reorderedPages,
            currentPageIndex = index + 1,
            modifiedAt = System.currentTimeMillis()
        )
    }
}
