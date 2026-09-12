package com.mal5odha.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndex(entry: SearchIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndices(entries: List<SearchIndexEntity>)

    @Query("""
        SELECT rowid, documentId, pageIndex, text, sourceType, boundsLeft, boundsTop, boundsRight, boundsBottom
        FROM document_search_index 
        WHERE document_search_index MATCH :query AND documentId = :documentId
    """)
    suspend fun searchInDocument(documentId: String, query: String): List<SearchIndexEntity>

    @Query("""
        SELECT rowid, documentId, pageIndex, text, sourceType, boundsLeft, boundsTop, boundsRight, boundsBottom
        FROM document_search_index 
        WHERE document_search_index MATCH :query
    """)
    suspend fun searchAll(query: String): List<SearchIndexEntity>

    @Query("DELETE FROM document_search_index WHERE documentId = :documentId AND pageIndex = :pageIndex AND sourceType = :sourceType")
    suspend fun clearPageIndices(documentId: String, pageIndex: Int, sourceType: String)

    @Query("DELETE FROM document_search_index WHERE documentId = :documentId")
    suspend fun clearDocumentIndices(documentId: String)
}
