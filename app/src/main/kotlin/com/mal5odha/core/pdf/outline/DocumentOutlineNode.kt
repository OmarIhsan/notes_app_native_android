package com.mal5odha.core.pdf.outline

/**
 * Recursive tree node representing a Table of Contents entry or PDF bookmark.
 */
data class DocumentOutlineNode(
    val title: String,
    val targetPageIndex: Int,
    val children: List<DocumentOutlineNode> = emptyList()
)
