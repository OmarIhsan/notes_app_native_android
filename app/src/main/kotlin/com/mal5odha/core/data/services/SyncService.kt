package com.mal5odha.core.data.services

import com.mal5odha.core.data.repository.DocumentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull

interface SyncService {
    suspend fun syncNow(): Result<Unit>
    fun getSyncStatus(): Flow<String>
}

@Singleton
class SyncServiceImpl
@Inject
constructor(
        private val documentApiService: DocumentApiService,
        private val documentRepository: DocumentRepository
) : SyncService {

    override suspend fun syncNow(): Result<Unit> {
        try {
            // 1. Fetch remote changes
            val remoteChangesResult = documentApiService.fetchRemoteChanges(0L)
            if (remoteChangesResult.isFailure)
                    return Result.failure(remoteChangesResult.exceptionOrNull()!!)

            // 2. Upload local unsynced changes (MVP: Just upload all for demonstration)
            val allLocalDocs = documentRepository.getAllDocuments().firstOrNull() ?: emptyList()
            for (doc in allLocalDocs) {
                // In a real app, only upload if doc.modifiedAt > doc.lastSyncedAt
                // And zip the physical note directory payload to `fileBytes`
                documentApiService.uploadDocument(doc, null)
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override fun getSyncStatus(): Flow<String> {
        // Return a flow emitting sync status (e.g. "Syncing...", "Up to date", "Error")
        return emptyFlow()
    }
}
