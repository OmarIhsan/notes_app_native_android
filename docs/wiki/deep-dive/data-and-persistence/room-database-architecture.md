# Room Database & Metadata Architecture

- **File Path**: [`app/src/main/kotlin/com/mal5odha/core/data/local/AppDatabase.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/data/local/AppDatabase.kt#L1-L25)
- **DAO**: [`app/src/main/kotlin/com/mal5odha/core/data/local/DocumentDao.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/data/local/DocumentDao.kt#L9-L41)
- **Entity**: [`app/src/main/kotlin/com/mal5odha/core/data/local/DocumentEntity.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/data/local/DocumentEntity.kt#L1-L35)
- **Subsystem**: Data Persistence & Storage
- **Primary Interface**: `com.mal5odha.core.data.local.DocumentDao`

---

## 1. Architectural Strategy: Hybrid Storage Separation

Mal5odha employs a strict separation between **Relational Document Metadata** and **Unbounded Vector Ink Data**:

```mermaid
graph TD
    App[Mal5odha Application] --> Room[(Room SQLite Database)]
    App --> Disk[(Sandboxed Filesystem Storage)]
    
    subgraph Room Database (Metadata & Indexing)
        Room --> Docs[DocumentEntity Table]
        Docs -.-> RootDocs[Root Documents Query]
        Docs -.-> FolderDocs[Folder Contents Query]
        Docs -.-> TrashDocs[Recycle Bin Query]
    end
    
    subgraph Sandboxed Disk (/files/notes/{id}/)
        Disk --> PagesJson[pages.json: Page order & background info]
        Disk --> StrokesBin[strokes_{pageId}.bin: Binary Stroke Vector Streams]
        Disk --> Thumbs[cover.jpg: Generated Document Thumbnail]
    end
```

By keeping heavyweight stroke coordinates out of SQLite tables, Room queries execute in sub-millisecond times, preventing SQLite cursor window exhaustion ($2\text{ MB}$ limit) when loading note lists on the Dashboard.

---

## 2. Room Entity Specification (`DocumentEntity`)

The schema represents notes, folders, and PDF imports uniformly:

```kotlin
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "NOTE", "FOLDER", "PDF"
    val parentFolderId: String?,
    val coverImage: String?,
    val paperType: String,
    val paperColor: Int,
    val pageCount: Int,
    val isDeleted: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
```

---

## 3. Data Access Object (`DocumentDao`)

The DAO defines reactive `kotlinx.coroutines.flow.Flow` queries for continuous UI updates:

```kotlin
// DocumentDao.kt:10-40
@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE parentFolderId IS NULL AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getRootDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE parentFolderId = :folderId AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getDocumentsInFolder(folderId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isDeleted = 1 ORDER BY lastModified DESC")
    fun getDeletedDocuments(): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isDeleted = 1 WHERE id = :id")
    suspend fun moveToTrash(id: String)

    @Query("UPDATE documents SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deletePermanently(id: String)
}
```

---

## 4. Key Capabilities Supported by Room Schema

### 4.1 Hierarchical Folder Tree
Folders and documents share the `documents` table with self-referencing `parentFolderId`. Nested folders are queried recursively without schema divergence.

### 4.2 Soft Deletion & Two-Stage Recycle Bin
Calling `moveToTrash(id)` sets `isDeleted = 1`. The note is immediately hidden from the Dashboard and Folder views while remaining fully restorable from the `RecycleBinScreen` until `deletePermanently(id)` is executed.
