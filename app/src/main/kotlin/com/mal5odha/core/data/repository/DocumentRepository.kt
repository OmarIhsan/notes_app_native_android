package com.mal5odha.core.data.repository

import com.mal5odha.core.data.local.DocumentDao
import com.mal5odha.core.data.local.DocumentEntity
import com.mal5odha.core.data.models.UnifiedDocument
import com.mal5odha.core.data.services.PageManagerService
import com.mal5odha.core.data.repository.NoteFileSystemRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

@Singleton
class DocumentRepository
@Inject
constructor(
        private val documentDao: DocumentDao,
        private val pageManagerService: PageManagerService,
        private val fileSystemRepository: NoteFileSystemRepository
) {
    fun getRootDocuments(): Flow<List<UnifiedDocument>> {
        return documentDao.getRootDocuments().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getDocumentsInFolder(folderId: String): Flow<List<UnifiedDocument>> {
        return documentDao.getDocumentsInFolder(folderId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getAllDocuments(): Flow<List<UnifiedDocument>> {
        return documentDao.getAllDocuments().map { entities -> entities.map { it.toDomainModel() } }
    }

    fun getDeletedDocuments(): Flow<List<UnifiedDocument>> {
        return documentDao.getDeletedDocuments().map { entities -> entities.map { it.toDomainModel() } }
    }

    fun getDocumentById(id: String): Flow<UnifiedDocument?> {
        return documentDao.getDocumentById(id).map { it?.toDomainModel() }
    }

    suspend fun saveDocument(document: UnifiedDocument) {
        documentDao.insertDocument(document.toEntity())
        pageManagerService.createNoteStructure(document.id)
    }

    suspend fun updateThumbnail(documentId: String, thumbnailPath: String) {
        documentDao.updateThumbnail(documentId, thumbnailPath)
    }

    suspend fun setStarred(id: String, isStarred: Boolean) {
        documentDao.setStarred(id, isStarred)
    }

    suspend fun renameDocument(id: String, title: String) {
        documentDao.renameDocument(id, title)
    }

    suspend fun moveDocument(id: String, parentFolderId: String?) {
        documentDao.moveDocument(id, parentFolderId)
    }

    suspend fun duplicateDocument(id: String): UnifiedDocument? {
        val original = documentDao.getDocumentById(id).firstOrNull() ?: return null
        val newId = UUID.randomUUID().toString()
        val existingTitles = if (original.parentFolderId == null) {
            documentDao.getRootTitles()
        } else {
            documentDao.getTitlesInFolder(original.parentFolderId)
        }
        val newTitle = com.mal5odha.core.data.factory.DocumentNameFactory.forDuplicate(original.title, existingTitles)
        val newDoc = original.copy(
            id = newId,
            title = newTitle,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        fileSystemRepository.duplicateNoteDirectory(id, newId)
        documentDao.insertDocument(newDoc)
        return newDoc.toDomainModel()
    }

    suspend fun createBlankDocument(
        title: String? = null,
        parentFolderId: String? = null,
        paperType: String = "BLANK",
        paperColor: Int = -1
    ): UnifiedDocument {
        val newId = UUID.randomUUID().toString()
        val finalTitle = title?.trim()?.ifBlank { null }
            ?: com.mal5odha.core.data.factory.DocumentNameFactory.defaultNotebookTitle()
        val doc = UnifiedDocument(
            id = newId,
            title = finalTitle,
            type = com.mal5odha.core.data.models.DocumentType.NOTE,
            parentFolderId = parentFolderId,
            paperType = paperType,
            paperColor = paperColor,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        saveDocument(doc)
        return doc
    }

    suspend fun moveToTrash(id: String) {
        documentDao.moveToTrash(id)
    }

    suspend fun restoreFromTrash(id: String) {
        documentDao.restoreFromTrash(id)
    }

    suspend fun deletePermanently(id: String) {
        documentDao.deletePermanently(id)
        pageManagerService.purgeNoteStructure(id)
    }

    // Extension functions for mapping
    private fun DocumentEntity.toDomainModel(): UnifiedDocument {
        return UnifiedDocument(
                id = id,
                title = title,
                type = when (type) {
                    "FOLDER" -> com.mal5odha.core.data.models.DocumentType.FOLDER
                    "PDF" -> com.mal5odha.core.data.models.DocumentType.PDF
                    else -> com.mal5odha.core.data.models.DocumentType.NOTE
                },
                parentFolderId = parentFolderId,
                modifiedAt = lastModified,
                createdAt = createdAt,
                isDeleted = isDeleted,
                isStarred = isStarred,
                paperType = paperType,
                paperColor = paperColor,
                colorHex = colorHex,
                thumbnailPath = coverImage,
                pages = emptyList()
        )
    }

    private fun UnifiedDocument.toEntity(): DocumentEntity {
        return DocumentEntity(
                id = id,
                title = title,
                type = type.name,
                parentFolderId = parentFolderId,
                lastModified = modifiedAt,
                createdAt = createdAt,
                coverImage = thumbnailPath,
                isDeleted = isDeleted,
                isStarred = isStarred,
                paperType = paperType,
                paperColor = paperColor,
                colorHex = colorHex
        )
    }
}
