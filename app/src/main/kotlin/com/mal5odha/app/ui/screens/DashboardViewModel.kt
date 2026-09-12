package com.mal5odha.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mal5odha.core.data.models.DocumentType
import com.mal5odha.core.data.models.UnifiedDocument
import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.repository.DocumentRepository
import com.mal5odha.core.data.repository.NoteFileSystemRepository
import com.mal5odha.core.data.services.BatchOperationsService
import com.mal5odha.core.data.services.ThumbnailGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryFilter {
    ALL,
    RECENTS,
    STARRED,
    TRASH
}

data class DashboardUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedFolderId: String? = null,
    val selectedFolderName: String? = null,
    val folders: List<UnifiedDocument> = emptyList(),
    val displayedDocuments: List<UnifiedDocument> = emptyList(),
    val documents: List<UnifiedDocument> = emptyList(),
    val allNotesCount: Int = 0,
    val starredCount: Int = 0,
    val trashCount: Int = 0,
    val deletedDocuments: List<UnifiedDocument> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val documentPageCounts: Map<String, Int> = emptyMap(),
    val isGridView: Boolean = true
)

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val repository: DocumentRepository,
    private val batchOperationsService: BatchOperationsService,
    private val fileSystemRepository: NoteFileSystemRepository,
    private val thumbnailGenerator: ThumbnailGenerator
) : ViewModel() {

    private val _allDocuments = MutableStateFlow<List<UnifiedDocument>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    private val _selectedFolderId = MutableStateFlow<String?>(null)
    private val _documentPageCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _isGridView = MutableStateFlow(true)
    private var isSeeded = false

    val uiState: StateFlow<DashboardUiState> = combine(
        _allDocuments,
        _searchQuery,
        _isLoading,
        _selectedFilter,
        _selectedFolderId,
        _documentPageCounts,
        _isGridView
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val docs = args[0] as List<UnifiedDocument>
        val query = args[1] as String
        val loading = args[2] as Boolean
        val filter = args[3] as LibraryFilter
        val folderId = args[4] as String?
        @Suppress("UNCHECKED_CAST")
        val pageCounts = args[5] as Map<String, Int>
        val gridView = args[6] as Boolean

        val nonDeletedDocs = docs.filter { !it.isDeleted }
        val folders = nonDeletedDocs.filter { it.type == DocumentType.FOLDER }
        val allNotes = nonDeletedDocs.filter { it.type != DocumentType.FOLDER }
        val starredNotes = allNotes.filter { it.isStarred }
        val trashNotes = docs.filter { it.isDeleted }

        val currentFolderName = folders.find { it.id == folderId }?.title

        // Determine base set of documents before search query filtering
        val baseFiltered = when {
            folderId != null -> {
                nonDeletedDocs.filter { it.parentFolderId == folderId }
            }
            filter == LibraryFilter.STARRED -> {
                starredNotes
            }
            filter == LibraryFilter.TRASH -> {
                trashNotes
            }
            filter == LibraryFilter.RECENTS -> {
                allNotes.sortedByDescending { it.modifiedAt }
            }
            else -> {
                // LibraryFilter.ALL: show root items (or all notes if not in folder)
                nonDeletedDocs.filter { it.parentFolderId == null }
            }
        }

        val displayed = if (query.isBlank()) {
            baseFiltered
        } else {
            baseFiltered.filter { it.title.contains(query, ignoreCase = true) }
        }

        DashboardUiState(
            selectedFilter = filter,
            selectedFolderId = folderId,
            selectedFolderName = currentFolderName,
            folders = folders,
            displayedDocuments = displayed,
            documents = nonDeletedDocs,
            allNotesCount = allNotes.size,
            starredCount = starredNotes.size,
            trashCount = trashNotes.size,
            deletedDocuments = trashNotes,
            isLoading = loading,
            searchQuery = query,
            documentPageCounts = pageCounts,
            isGridView = gridView
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(isLoading = true))

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllDocuments().collect { docs ->
                if (docs.isEmpty() && !isSeeded) {
                    isSeeded = true
                    seedInitialData()
                    return@collect
                }

                _allDocuments.value = docs
                _isLoading.value = false

                // Asynchronously fetch page counts and ensure thumbnails exist
                launch(Dispatchers.IO) {
                    val counts = mutableMapOf<String, Int>()
                    for (doc in docs) {
                        if (doc.type != DocumentType.FOLDER) {
                            val pages = fileSystemRepository.loadNotePages(doc.id)
                            counts[doc.id] = pages.size.coerceAtLeast(1)

                            if (doc.thumbnailPath.isNullOrEmpty() || !File(doc.thumbnailPath).exists()) {
                                val firstPage = pages.firstOrNull()
                                val strokes = firstPage?.let { fileSystemRepository.loadPageStrokes(doc.id, it.id) }
                                val path = thumbnailGenerator.generateThumbnail(
                                    context = context,
                                    documentId = doc.id,
                                    page = firstPage,
                                    strokes = strokes,
                                    paperColor = doc.paperColor
                                )
                                if (path != null) {
                                    repository.updateThumbnail(doc.id, path)
                                }
                            }
                        }
                    }
                    _documentPageCounts.value = counts
                }
            }
        }
    }

    private suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        // 1. Seed generic starter folders
        val starterFolders = listOf(
            Triple("Work", "#2563EB", UUID.randomUUID().toString()),
            Triple("Personal", "#059669", UUID.randomUUID().toString()),
            Triple("Ideas", "#D97706", UUID.randomUUID().toString())
        )

        for ((name, color, id) in starterFolders) {
            val folder = UnifiedDocument(
                id = id,
                title = name,
                type = DocumentType.FOLDER,
                colorHex = color,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            repository.saveDocument(folder)
        }

        // 2. Seed neutral starter notebooks
        val starterNotes = listOf(
            Triple("Quick Start Guide", "BLANK", "#00A6CB"),
            Triple("Meeting Notes", "RULED", "#2563EB"),
            Triple("Journal", "GRID", "#059669")
        )

        for ((title, paperType, colorHex) in starterNotes) {
            val docId = UUID.randomUUID().toString()
            val note = UnifiedDocument(
                id = docId,
                title = title,
                type = DocumentType.NOTE,
                paperType = paperType,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            val initialPage = Ml5Page(
                id = "page_${UUID.randomUUID()}",
                orderIndex = 0,
                backgroundType = paperType,
                backgroundData = null
            )
            fileSystemRepository.saveNotePages(docId, listOf(initialPage))
            val thumbPath = thumbnailGenerator.generateThumbnail(
                context = context,
                documentId = docId,
                page = initialPage,
                strokes = emptyList(),
                paperColor = -1
            )
            repository.saveDocument(if (thumbPath != null) note.copy(thumbnailPath = thumbPath) else note)
        }
    }

    fun setFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
        _selectedFolderId.value = null
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun createFolder(name: String, colorHex: String = "#00A6CB", parentFolderId: String? = null) {
        viewModelScope.launch {
            val newFolder = UnifiedDocument(
                id = UUID.randomUUID().toString(),
                title = name.trim(),
                type = DocumentType.FOLDER,
                colorHex = colorHex,
                parentFolderId = parentFolderId,
                modifiedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            repository.saveDocument(newFolder)
        }
    }

    fun createNewNote(
        title: String = "",
        paperType: String = "BLANK",
        paperColor: Int = -1,
        folderId: String? = _selectedFolderId.value,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val documentId = UUID.randomUUID().toString()
            val resolvedTitle = title.trim().ifBlank {
                com.mal5odha.core.data.factory.DocumentNameFactory.defaultNotebookTitle(context)
            }
            val newDoc = UnifiedDocument(
                id = documentId,
                title = resolvedTitle,
                type = DocumentType.NOTE,
                paperType = paperType,
                paperColor = paperColor,
                parentFolderId = folderId,
                modifiedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            val initialPages = listOf(
                Ml5Page(
                    id = "page_initial_${UUID.randomUUID()}",
                    orderIndex = 0,
                    backgroundType = paperType.uppercase(),
                    backgroundData = null
                )
            )
            fileSystemRepository.saveNotePages(documentId, initialPages)

            val thumbPath = thumbnailGenerator.generateThumbnail(
                context = context,
                documentId = documentId,
                page = initialPages.first(),
                strokes = emptyList(),
                paperColor = paperColor
            )

            val docToSave = if (thumbPath != null) newDoc.copy(thumbnailPath = thumbPath) else newDoc
            repository.saveDocument(docToSave)

            onCreated(documentId)
        }
    }

    fun createQuickNote(onCreated: (String) -> Unit) {
        val quickTitle = com.mal5odha.core.data.factory.DocumentNameFactory.defaultNotebookTitle(context)
        createNewNote(
            title = quickTitle,
            paperType = "RULED",
            paperColor = -1,
            folderId = _selectedFolderId.value,
            onCreated = onCreated
        )
    }

    fun renameDocument(documentId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(documentId, newTitle.trim())
        }
    }

    fun moveDocumentToFolder(documentId: String, folderId: String?) {
        viewModelScope.launch {
            repository.moveDocument(documentId, folderId)
        }
    }

    fun duplicateDocument(documentId: String, onDuplicated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val duplicated = repository.duplicateDocument(documentId)
            if (duplicated != null) {
                onDuplicated(duplicated.id)
            }
        }
    }

    fun toggleStarred(documentId: String) {
        viewModelScope.launch {
            val doc = _allDocuments.value.find { it.id == documentId } ?: return@launch
            repository.setStarred(documentId, !doc.isStarred)
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            repository.moveToTrash(documentId)
        }
    }

    fun restoreDocument(documentId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(documentId)
        }
    }

    fun restoreItems(documentIds: List<String>) {
        viewModelScope.launch {
            for (id in documentIds) {
                repository.restoreFromTrash(id)
            }
        }
    }

    fun deletePermanently(documentId: String) {
        viewModelScope.launch {
            repository.deletePermanently(documentId)
        }
    }

    fun purgeItems(documentIds: List<String>) {
        viewModelScope.launch {
            for (id in documentIds) {
                repository.deletePermanently(id)
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val trashItems = _allDocuments.value.filter { it.isDeleted }
            for (item in trashItems) {
                repository.deletePermanently(item.id)
            }
        }
    }

    fun getNoteDetails(documentId: String, onDetails: (fileSizeBytes: Long, pageCount: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val size = fileSystemRepository.getNoteDirectorySizeBytes(documentId)
            val pages = fileSystemRepository.loadNotePages(documentId)
            withContext(Dispatchers.Main) {
                onDetails(size, pages.size.coerceAtLeast(1))
            }
        }
    }
}
