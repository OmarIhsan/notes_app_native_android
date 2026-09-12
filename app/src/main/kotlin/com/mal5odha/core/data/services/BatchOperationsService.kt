package com.mal5odha.core.data.services

import com.mal5odha.core.data.repository.DocumentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

interface BatchOperationsService {
    suspend fun moveItems(itemIds: List<String>, targetFolderId: String?)
    suspend fun trashItems(itemIds: List<String>)
    suspend fun restoreItems(itemIds: List<String>)
    suspend fun purgeItems(itemIds: List<String>)
}

@Singleton
class BatchOperationsServiceImpl
@Inject
constructor(
        private val documentRepository: DocumentRepository,
        private val pageManagerService: PageManagerService
) : BatchOperationsService {

    /**
     * Moves a list of documents or folders into a new destination folder. Use targetFolderId = null
     * to move back to the root Dashboard.
     */
    override suspend fun moveItems(itemIds: List<String>, targetFolderId: String?) {
        for (id in itemIds) {
            val document = documentRepository.getDocumentById(id).firstOrNull()
            if (document != null) {
                // Prevent moving a folder into itself
                if (id == targetFolderId) continue

                val updatedDocument =
                        document.copy(
                                parentFolderId = targetFolderId,
                                modifiedAt = System.currentTimeMillis()
                        )
                documentRepository.saveDocument(updatedDocument)
            }
        }
    }

    /** Moves a list of documents to the Recycle Bin. */
    override suspend fun trashItems(itemIds: List<String>) {
        for (id in itemIds) {
            documentRepository.moveToTrash(id)
        }
    }

    /** Restores a list of documents from the Recycle Bin. */
    override suspend fun restoreItems(itemIds: List<String>) {
        for (id in itemIds) {
            documentRepository.restoreFromTrash(id)
        }
    }

    /** Permanently deletes documents and their physical files. */
    override suspend fun purgeItems(itemIds: List<String>) {
        for (id in itemIds) {
            documentRepository.deletePermanently(id)
        }
    }

    // copyItems is deferred as it requires recursive tree generation and new UUIDs
}
