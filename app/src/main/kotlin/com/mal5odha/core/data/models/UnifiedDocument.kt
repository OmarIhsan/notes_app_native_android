package com.mal5odha.core.data.models

enum class PageBackground {
    BLANK,
    PDF_PAGE,
    IMAGE,
    RULED,
    GRID,
    DOTTED
}

data class DocumentPage(
    val id: String,
    val order: Int,
    val background: PageBackground = PageBackground.BLANK,
    val backgroundData: String? = null,
    val isBookmarked: Boolean = false,
    val widthPt: Float = 595f,
    val heightPt: Float = 842f,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    val aspectRatio: Float
        get() = if (heightPt > 0f) widthPt / heightPt else (595f / 842f)
}

enum class DocumentType {
    NOTE,
    FOLDER,
    PDF
}

data class UnifiedDocument(
    val id: String,
    val title: String,
    val type: DocumentType = DocumentType.NOTE,
    val parentFolderId: String? = null,
    val pages: List<DocumentPage> = listOf(),
    val currentPageIndex: Int = 0,
    val isDeleted: Boolean = false,
    val paperType: String = "BLANK",
    val paperColor: Int = -1,
    val isStarred: Boolean = false,
    val colorHex: String = "#00A6CB",
    val thumbnailPath: String? = null,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class Folder(
    val id: String,
    val name: String,
    val colorHex: String = "#00A6CB",
    val parentId: String? = null
)
