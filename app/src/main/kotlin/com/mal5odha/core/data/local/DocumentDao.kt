package com.mal5odha.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE parentFolderId IS NULL AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getRootDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE parentFolderId = :folderId AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getDocumentsInFolder(folderId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 0 ORDER BY lastModified DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 1 ORDER BY lastModified DESC")
    fun getDeletedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentById(id: String): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("UPDATE documents SET coverImage = :thumbnailPath, lastModified = :timestamp WHERE id = :id")
    suspend fun updateThumbnail(id: String, thumbnailPath: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isDeleted = 1 WHERE id = :id")
    suspend fun moveToTrash(id: String)

    @Query("UPDATE documents SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("UPDATE documents SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: String, isStarred: Boolean)

    @Query("UPDATE documents SET title = :title, lastModified = :timestamp WHERE id = :id")
    suspend fun renameDocument(id: String, title: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET parentFolderId = :parentFolderId, lastModified = :timestamp WHERE id = :id")
    suspend fun moveDocument(id: String, parentFolderId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("SELECT title FROM documents WHERE parentFolderId IS NULL AND isDeleted = 0")
    suspend fun getRootTitles(): List<String>

    @Query("SELECT title FROM documents WHERE parentFolderId = :folderId AND isDeleted = 0")
    suspend fun getTitlesInFolder(folderId: String): List<String>
}
