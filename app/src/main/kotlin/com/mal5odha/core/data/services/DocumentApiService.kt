package com.mal5odha.core.data.services

import com.mal5odha.core.data.models.UnifiedDocument
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

interface DocumentApiService {
    suspend fun uploadDocument(document: UnifiedDocument, fileBytes: ByteArray?): Result<Unit>
    suspend fun downloadDocument(documentId: String): Result<Pair<UnifiedDocument, ByteArray?>>
    suspend fun fetchRemoteChanges(lastSyncTimestamp: Long): Result<List<UnifiedDocument>>
}

@Singleton
class DocumentApiServiceImpl @Inject constructor() : DocumentApiService {

    override suspend fun uploadDocument(
            document: UnifiedDocument,
            fileBytes: ByteArray?
    ): Result<Unit> {
        // MVP: Simulate network delay
        delay(1000)
        return Result.success(Unit)
    }

    override suspend fun downloadDocument(
            documentId: String
    ): Result<Pair<UnifiedDocument, ByteArray?>> {
        // MVP: Simulate network delay
        delay(1000)
        return Result.failure(Exception("Not implemented on MVP backend"))
    }

    override suspend fun fetchRemoteChanges(
            lastSyncTimestamp: Long
    ): Result<List<UnifiedDocument>> {
        // MVP: Simulate network delay
        delay(800)
        return Result.success(emptyList()) // Return empty list for now until backend is real
    }
}
