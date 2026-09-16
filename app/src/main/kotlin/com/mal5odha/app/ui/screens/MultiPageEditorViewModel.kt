package com.mal5odha.app.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mal5odha.core.data.models.DocumentPage
import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.models.Ml5Point
import com.mal5odha.core.data.models.Ml5Stroke
import com.mal5odha.core.data.models.Ml5MediaAnnotation
import com.mal5odha.core.ink.models.MediaAnnotation
import com.mal5odha.core.data.models.PageBackground
import com.mal5odha.core.data.models.UnifiedDocument
import com.mal5odha.core.data.repository.DocumentRepository
import com.mal5odha.core.data.models.DocumentType
import com.mal5odha.core.data.repository.NoteFileSystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
        val isLoading: Boolean = true,
        val document: UnifiedDocument? = null,
        val error: String? = null,
        val isReadOnly: Boolean = false
)

data class ExportState(
        val isExporting: Boolean = false,
        val progress: Float = 0f,
        val exportedUri: android.net.Uri? = null,
        val error: String? = null
)

data class SearchUiState(
        val isSearching: Boolean = false,
        val query: String = "",
        val results: List<com.mal5odha.core.search.SearchResultItem> = emptyList(),
        val currentResultIndex: Int = -1,
        val isSearchBarVisible: Boolean = false
)

@HiltViewModel
class MultiPageEditorViewModel
@Inject
constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
        private val documentRepository: DocumentRepository,
        private val fileSystemRepository: NoteFileSystemRepository,
        private val thumbnailGenerator: com.mal5odha.core.data.services.ThumbnailGenerator,
        private val pdfExportService: com.mal5odha.core.pdf.PdfExportService,
        private val audioSyncManager: com.mal5odha.core.audio.AudioSyncManager,
        private val searchEngine: com.mal5odha.core.search.SearchEngine,
        private val handwritingIndexingService: com.mal5odha.core.ink.recognition.HandwritingIndexingService,
        private val pdfOutlineService: com.mal5odha.core.pdf.outline.PdfOutlineService,
        private val documentSyncEngine: com.mal5odha.core.data.repository.DocumentSyncEngine
) : ViewModel() {

    private val TAG = "MultiPageEditorVM"

    private val _uiState = MutableStateFlow(EditorUiState())
        val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

        // Map of PageId to List of Strokes
        private val _pageStrokes =
                MutableStateFlow<Map<String, List<com.mal5odha.core.ink.models.Stroke>>>(emptyMap())
        val pageStrokes: StateFlow<Map<String, List<com.mal5odha.core.ink.models.Stroke>>> =
                _pageStrokes.asStateFlow()

        // Map of PageId to List of TextAnnotations
        private val _pageTextAnnotations =
                MutableStateFlow<Map<String, List<com.mal5odha.core.ink.models.TextAnnotation>>>(
                        emptyMap()
                )
        val pageTextAnnotations:
                StateFlow<Map<String, List<com.mal5odha.core.ink.models.TextAnnotation>>> =
                _pageTextAnnotations.asStateFlow()

        // Map of PageId to List of MediaAnnotations
        private val _pageMediaAnnotations =
                MutableStateFlow<Map<String, List<MediaAnnotation>>>(
                        emptyMap()
                )
        val pageMediaAnnotations:
                StateFlow<Map<String, List<MediaAnnotation>>> =
                _pageMediaAnnotations.asStateFlow()

        val audioSyncState = audioSyncManager.syncState

        private val _audioPlaybackPositionMs = MutableStateFlow(0L)
        val audioPlaybackPositionMs: StateFlow<Long> = _audioPlaybackPositionMs.asStateFlow()

        private val _effectiveTool = MutableStateFlow(com.mal5odha.core.ink.models.InkTool.PEN)
        val effectiveTool: StateFlow<com.mal5odha.core.ink.models.InkTool> = _effectiveTool.asStateFlow()

        fun setEffectiveTool(tool: com.mal5odha.core.ink.models.InkTool) {
            _effectiveTool.value = tool
        }

        init {
            viewModelScope.launch {
                audioSyncManager.syncState.collect { sync ->
                    _audioPlaybackPositionMs.value = sync.elapsedMs
                }
            }
        }

        // Binary Read-Only / Inking Mode State
        private val _isReadOnly = MutableStateFlow(false)
        val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()
        val isReadOnlyMode: StateFlow<Boolean> = _isReadOnly.asStateFlow()

        // In-Editor Collapsible Left Panel State (Thumbnails, Outline, Bookmarks)
        private val _isLeftPanelOpen = MutableStateFlow(false)
        val isLeftPanelOpen: StateFlow<Boolean> = _isLeftPanelOpen.asStateFlow()

        fun toggleLeftPanel() {
            _isLeftPanelOpen.update { !it }
        }

        fun setLeftPanelOpen(open: Boolean) {
            _isLeftPanelOpen.value = open
        }

        val folders: StateFlow<List<UnifiedDocument>> = documentRepository.getAllDocuments()
            .map { list -> list.filter { !it.isDeleted && it.type == DocumentType.FOLDER } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun toggleStarred() {
            val doc = _uiState.value.document ?: return
            viewModelScope.launch {
                val newStarred = !doc.isStarred
                documentRepository.setStarred(doc.id, newStarred)
                _uiState.update { it.copy(document = it.document?.copy(isStarred = newStarred)) }
            }
        }

        fun renameDocument(newTitle: String) {
            val doc = _uiState.value.document ?: return
            viewModelScope.launch {
                documentRepository.renameDocument(doc.id, newTitle)
                _uiState.update { it.copy(document = it.document?.copy(title = newTitle)) }
            }
        }

        fun moveDocument(targetFolderId: String?) {
            val doc = _uiState.value.document ?: return
            viewModelScope.launch {
                documentRepository.moveDocument(doc.id, targetFolderId)
                _uiState.update { it.copy(document = it.document?.copy(parentFolderId = targetFolderId)) }
            }
        }

        fun duplicateDocument() {
            val doc = _uiState.value.document ?: return
            viewModelScope.launch {
                documentRepository.duplicateDocument(doc.id)
            }
        }

        fun deleteDocument() {
            val doc = _uiState.value.document ?: return
            viewModelScope.launch {
                documentRepository.moveToTrash(doc.id)
            }
        }

        fun getNoteDetails(onResult: (fileSizeBytes: Long, pageCount: Int) -> Unit) {
            val doc = _uiState.value.document ?: return
            viewModelScope.launch(Dispatchers.IO) {
                val size = fileSystemRepository.getNoteDirectorySizeBytes(doc.id)
                val pages = doc.pages.size
                withContext(Dispatchers.Main) {
                    onResult(size, pages)
                }
            }
        }

        // Universal Tri-Layer Search State (Handwriting OCR, Text Annotations, PDF Text)
        private val _searchUiState = MutableStateFlow(SearchUiState())
        val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

        // PDF Document Outline / Table of Contents Tree
        private val _outlineTree = MutableStateFlow<List<com.mal5odha.core.pdf.outline.DocumentOutlineNode>>(emptyList())
        val outlineTree: StateFlow<List<com.mal5odha.core.pdf.outline.DocumentOutlineNode>> = _outlineTree.asStateFlow()

        // Phase 3: Discrete Multi-Layer Vector Inking State
        private val _layers = MutableStateFlow<List<com.mal5odha.core.ink.models.DocumentLayer>>(
            com.mal5odha.core.ink.models.DocumentLayer.defaultLayers()
        )
        val layers: StateFlow<List<com.mal5odha.core.ink.models.DocumentLayer>> = _layers.asStateFlow()

        private val _activeLayerId = MutableStateFlow<String?>(com.mal5odha.core.ink.models.DocumentLayer.LAYER_PEN_ID)
        val activeLayerId: StateFlow<String?> = _activeLayerId.asStateFlow()

        private val _eraserTarget = MutableStateFlow(com.mal5odha.core.ink.models.EraserTarget.ALL)
        val eraserTarget: StateFlow<com.mal5odha.core.ink.models.EraserTarget> = _eraserTarget.asStateFlow()

        // Phase 4: Dual-Page Spread State (Book Mode)
        private val _isDualPageSpread = MutableStateFlow(false)
        val isDualPageSpread: StateFlow<Boolean> = _isDualPageSpread.asStateFlow()

        // Phase 5: Append-Only Mutation Journal & Revision History
        private val _mutations = MutableStateFlow<List<com.mal5odha.core.data.models.DocumentMutation>>(emptyList())
        val mutations: StateFlow<List<com.mal5odha.core.data.models.DocumentMutation>> = _mutations.asStateFlow()

        fun selectActiveLayer(layerId: String) {
            _activeLayerId.value = layerId
        }

        fun toggleLayerVisibility(layerId: String) {
            _layers.update { list ->
                list.map { if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it }
            }
        }

        fun toggleLayerLock(layerId: String) {
            _layers.update { list ->
                list.map { if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it }
            }
        }

        fun updateLayerOpacity(layerId: String, alpha: Float) {
            _layers.update { list ->
                list.map { if (it.id == layerId) it.copy(alpha = alpha.coerceIn(0.05f, 1f)) else it }
            }
        }

        fun addCustomLayer(name: String) {
            val newLayer = com.mal5odha.core.ink.models.DocumentLayer(name = name)
            _layers.update { it + newLayer }
            _activeLayerId.value = newLayer.id
        }

        fun reorderLayers(fromIndex: Int, toIndex: Int) {
            _layers.update { list ->
                if (fromIndex in list.indices && toIndex in list.indices) {
                    val mutable = list.toMutableList()
                    val item = mutable.removeAt(fromIndex)
                    mutable.add(toIndex, item)
                    mutable
                } else list
            }
        }

        fun deleteLayer(layerId: String) {
            _layers.update { list ->
                if (list.size <= 1) list
                else list.filter { it.id != layerId }
            }
            if (_activeLayerId.value == layerId) {
                _activeLayerId.value = _layers.value.firstOrNull()?.id
            }
        }

        fun setEraserTarget(target: com.mal5odha.core.ink.models.EraserTarget) {
            _eraserTarget.value = target
        }

        fun toggleDualPageSpread() {
            _isDualPageSpread.update { !it }
        }

        fun setDualPageSpread(enabled: Boolean) {
            _isDualPageSpread.value = enabled
        }

        fun loadRevisionHistory() {
            val docId = uiState.value.document?.id ?: return
            viewModelScope.launch {
                val history = documentSyncEngine.loadHistory(docId)
                _mutations.value = history
            }
        }

        fun rollbackToMutation(mutation: com.mal5odha.core.data.models.DocumentMutation) {
            val docId = uiState.value.document?.id ?: return
            viewModelScope.launch {
                val doc = uiState.value.document ?: return@launch
                val updatedStrokesMap = _pageStrokes.value.toMutableMap()
                for (page in doc.pages) {
                    val restoredStrokes = documentSyncEngine.getStrokesAtTimestamp(docId, page.id, mutation.timestamp)
                    fileSystemRepository.savePageStrokes(docId, page.id, restoredStrokes)
                    updatedStrokesMap[page.id] = restoredStrokes.map { it.toDomain() }
                }
                _pageStrokes.value = updatedStrokesMap
                loadRevisionHistory()
            }
        }

        fun toggleBookmark(pageId: String) {
            val documentId = uiState.value.document?.id ?: return
            viewModelScope.launch {
                val currentMl5Pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
                val targetIdx = currentMl5Pages.indexOfFirst { it.id == pageId }
                if (targetIdx == -1) return@launch
                val updated = currentMl5Pages[targetIdx].copy(isBookmarked = !currentMl5Pages[targetIdx].isBookmarked)
                currentMl5Pages[targetIdx] = updated
                fileSystemRepository.saveNotePages(documentId, currentMl5Pages)

                // Update domain model
                val updatedDomain = uiState.value.document?.pages?.map {
                    if (it.id == pageId) it.copy(isBookmarked = updated.isBookmarked) else it
                } ?: emptyList()
                _uiState.update { it.copy(document = it.document?.copy(pages = updatedDomain)) }
            }
        }

        private val pageUndoRedoManagers = mutableMapOf<String, com.mal5odha.core.ink.UndoRedoManager>()

        fun getUndoRedoManager(pageId: String): com.mal5odha.core.ink.UndoRedoManager {
            return pageUndoRedoManagers.getOrPut(pageId) {
                com.mal5odha.core.ink.UndoRedoManager().apply {
                    val initial = _pageStrokes.value[pageId] ?: emptyList()
                    setInitialState(initial)
                }
            }
        }

        fun undo(pageId: String) {
            val documentId = uiState.value.document?.id ?: return
            val undone = getUndoRedoManager(pageId).undo() ?: return
            viewModelScope.launch {
                val currentMap = _pageStrokes.value.toMutableMap()
                val pageStrokesList = currentMap[pageId]?.toMutableList() ?: mutableListOf()
                pageStrokesList.removeAll { it.id == undone.id }
                currentMap[pageId] = pageStrokesList
                _pageStrokes.value = currentMap

                val ml5Strokes = pageStrokesList.map { it.toMl5() }
                fileSystemRepository.savePageStrokes(documentId, pageId, ml5Strokes)
            }
        }

        fun redo(pageId: String) {
            val documentId = uiState.value.document?.id ?: return
            val redone = getUndoRedoManager(pageId).redo() ?: return
            viewModelScope.launch {
                val currentMap = _pageStrokes.value.toMutableMap()
                val pageStrokesList = currentMap[pageId]?.toMutableList() ?: mutableListOf()
                pageStrokesList.add(redone)
                currentMap[pageId] = pageStrokesList
                _pageStrokes.value = currentMap

                val ml5Strokes = pageStrokesList.map { it.toMl5() }
                fileSystemRepository.savePageStrokes(documentId, pageId, ml5Strokes)
            }
        }

        fun clearCanvas(pageId: String) {
            val documentId = uiState.value.document?.id ?: return
            getUndoRedoManager(pageId).clear()
            viewModelScope.launch {
                val currentMap = _pageStrokes.value.toMutableMap()
                currentMap[pageId] = emptyList()
                _pageStrokes.value = currentMap

                fileSystemRepository.savePageStrokes(documentId, pageId, emptyList())
            }
        }

        fun setReadOnly(isReadOnly: Boolean) {
            _isReadOnly.value = isReadOnly
            _uiState.update { it.copy(isReadOnly = isReadOnly) }
        }

        fun toggleReadOnly() {
            setReadOnly(!_isReadOnly.value)
        }

        fun toggleReadOnlyMode() {
            toggleReadOnly()
        }

        fun setReadOnlyMode(enabled: Boolean) {
            setReadOnly(enabled)
        }

        fun toggleSearchBar() {
            _searchUiState.update { current ->
                val newVisible = !current.isSearchBarVisible
                if (!newVisible) {
                    SearchUiState()
                } else {
                    current.copy(isSearchBarVisible = true)
                }
            }
        }

        fun updateSearchQuery(query: String) {
            _searchUiState.update { it.copy(query = query, isSearching = true) }
            val docId = uiState.value.document?.id ?: return
            viewModelScope.launch {
                if (query.isBlank()) {
                    _searchUiState.update { it.copy(isSearching = false, results = emptyList(), currentResultIndex = -1) }
                    return@launch
                }
                val hits = searchEngine.searchDocument(docId, query)
                _searchUiState.update {
                    it.copy(
                        isSearching = false,
                        results = hits,
                        currentResultIndex = if (hits.isNotEmpty()) 0 else -1
                    )
                }
            }
        }

        fun nextSearchResult(): Int? {
            val current = _searchUiState.value
            if (current.results.isEmpty()) return null
            val nextIdx = (current.currentResultIndex + 1) % current.results.size
            _searchUiState.update { it.copy(currentResultIndex = nextIdx) }
            return current.results[nextIdx].pageIndex
        }

        fun previousSearchResult(): Int? {
            val current = _searchUiState.value
            if (current.results.isEmpty()) return null
            val prevIdx = if (current.currentResultIndex <= 0) current.results.size - 1 else current.currentResultIndex - 1
            _searchUiState.update { it.copy(currentResultIndex = prevIdx) }
            return current.results[prevIdx].pageIndex
        }

        fun clearSearch() {
            _searchUiState.value = SearchUiState()
        }

        fun loadDocument(documentId: String) {
                viewModelScope.launch {
                        try {
                                Log.d(TAG, "Loading document: $documentId")
                                _uiState.update { it.copy(isLoading = true, document = null, error = null) }
                                _pageStrokes.value = emptyMap()
                                _pageTextAnnotations.value = emptyMap()
                                _pageMediaAnnotations.value = emptyMap()
                                _outlineTree.value = emptyList()

                                // Use first() instead of collectLatest to prevent Room re-emissions
                                // from cancelling the in-flight IO operations (page/stroke loading).
                                val document = documentRepository.getDocumentById(documentId).first()
                                Log.d(TAG, "Document from DB: ${document?.id}")

                                if (document != null) {
                                        // Load pages from FileSystem
                                        var ml5Pages =
                                                fileSystemRepository.loadNotePages(documentId)
                                        Log.d(TAG, "Loaded ${ml5Pages.size} pages from FS")

                                        // Auto-recovery: if no pages exist, create at least one blank page
                                        if (ml5Pages.isEmpty()) {
                                            Log.d(TAG, "No pages found, creating recovery page")
                                            val recoveryPage = com.mal5odha.core.data.models.Ml5Page(
                                                id = "page_recovered_${UUID.randomUUID()}",
                                                orderIndex = 0,
                                                backgroundType = document.paperType.uppercase().let {
                                                    // validate it's a known type
                                                    try { PageBackground.valueOf(it); it } catch (e: Exception) { "BLANK" }
                                                },
                                                backgroundData = null
                                            )
                                            ml5Pages = listOf(recoveryPage)
                                            fileSystemRepository.saveNotePages(documentId, ml5Pages)
                                        }

                                        // Ensure pages are strictly sorted by orderIndex and normalized (0, 1, 2, ...)
                                        val sortedPages = ml5Pages.sortedBy { it.orderIndex }
                                        val normalizedPages = sortedPages.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
                                        if (normalizedPages != ml5Pages) {
                                            fileSystemRepository.saveNotePages(documentId, normalizedPages)
                                        }
                                        ml5Pages = normalizedPages

                                        val domainPages =
                                                ml5Pages.map { ml5 ->
                                                        val bg = try {
                                                            PageBackground.valueOf(ml5.backgroundType)
                                                        } catch (e: Exception) {
                                                            PageBackground.BLANK
                                                        }
                                                        DocumentPage(
                                                                id = ml5.id,
                                                                order = ml5.orderIndex,
                                                                background = bg,
                                                                backgroundData = ml5.backgroundData,
                                                                isBookmarked = ml5.isBookmarked,
                                                                widthPt = if (ml5.widthPt > 0f) ml5.widthPt else 595f,
                                                                heightPt = if (ml5.heightPt > 0f) ml5.heightPt else 842f
                                                        )
                                                }

                                        // Load PDF outline hierarchy if source PDF is available
                                        val pdfFile = java.io.File(context.filesDir, "notes/$documentId/source.pdf")
                                        if (pdfFile.exists()) {
                                            _outlineTree.value = pdfOutlineService.extractOutline(pdfFile)
                                        }

                                        // Load strokes, text, and media for each page before revealing document
                                        val strokesMap =
                                                mutableMapOf<
                                                        String,
                                                        List<com.mal5odha.core.ink.models.Stroke>>()
                                        val textMap =
                                                mutableMapOf<
                                                        String,
                                                        List<
                                                                com.mal5odha.core.ink.models.TextAnnotation>>()
                                        val mediaMap =
                                                mutableMapOf<
                                                        String,
                                                        List<MediaAnnotation>>()
                                        for (page in ml5Pages) {
                                                Log.d(TAG, "Loading strokes/text/media for page ${page.id}")
                                                val ml5Strokes =
                                                        fileSystemRepository.loadPageStrokes(
                                                                documentId,
                                                                page.id
                                                        )
                                                strokesMap[page.id] =
                                                        ml5Strokes.map { it.toDomain() }

                                                val ml5Texts =
                                                        fileSystemRepository
                                                                .loadPageTextAnnotations(
                                                                        documentId,
                                                                        page.id
                                                                )
                                                textMap[page.id] = ml5Texts.map { it.toDomain() }

                                                val ml5Media =
                                                        fileSystemRepository
                                                                .loadPageMediaAnnotations(
                                                                        documentId,
                                                                        page.id
                                                                )
                                                mediaMap[page.id] = ml5Media.map { it.toDomain() }
                                        }
                                        _pageStrokes.value = strokesMap
                                        for ((pId, pStrokes) in strokesMap) {
                                            getUndoRedoManager(pId).setInitialState(pStrokes)
                                        }
                                        _pageTextAnnotations.value = textMap
                                        _pageMediaAnnotations.value = mediaMap

                                        val clampedIndex = document.currentPageIndex.coerceIn(0, (domainPages.size - 1).coerceAtLeast(0))
                                        val updatedDoc = document.copy(pages = domainPages, currentPageIndex = clampedIndex)
                                        _uiState.update {
                                                it.copy(isLoading = false, document = updatedDoc)
                                        }
                                        Log.d(TAG, "Document fully loaded and state updated")
                                } else {
                                        Log.e(TAG, "Document not found in DB: $documentId")
                                        _uiState.update {
                                                it.copy(
                                                        isLoading = false,
                                                        document = null,
                                                        error = "Document not found"
                                                )
                                        }
                                }
                        } catch (e: Exception) {
                                Log.e(TAG, "Error loading document: ${e.message}", e)
                                _uiState.update {
                                        it.copy(
                                                isLoading = false,
                                                error = "Load failed: ${e.message}"
                                        )
                                }
                        }
                }
        }

        fun saveStroke(
                pageId: String,
                stroke: com.mal5odha.core.ink.models.Stroke
        ) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        val activeAudio = audioSyncManager.syncState.value
                        val elapsed = audioSyncManager.getCurrentAudioElapsedMs()
                        val stampedStroke = if (activeAudio.state == com.mal5odha.core.audio.AudioState.RECORDING &&
                                activeAudio.currentSessionId != null && stroke.audioSessionId == null
                        ) {
                                stroke.copy(
                                        audioSessionId = activeAudio.currentSessionId,
                                        audioTimestampMs = elapsed,
                                        timestampMs = elapsed
                                )
                        } else if (stroke.timestampMs > 0L && stroke.audioTimestampMs == 0L) {
                                stroke.copy(audioTimestampMs = stroke.timestampMs)
                        } else if (stroke.audioTimestampMs > 0L && stroke.timestampMs == 0L) {
                                stroke.copy(timestampMs = stroke.audioTimestampMs)
                        } else {
                                stroke
                        }

                        // Update local state
                        val currentMap = _pageStrokes.value.toMutableMap()
                        val pageStrokesList = currentMap[pageId]?.toMutableList() ?: mutableListOf()
                        pageStrokesList.add(stampedStroke)
                        currentMap[pageId] = pageStrokesList
                        _pageStrokes.value = currentMap

                        // Record to UndoRedoManager
                        getUndoRedoManager(pageId).recordAction(stampedStroke)

                        // Persist to FileSystem
                        val ml5Strokes = pageStrokesList.map { it.toMl5() }
                        fileSystemRepository.savePageStrokes(documentId, pageId, ml5Strokes)

                        // Record mutation to journal
                        documentSyncEngine.recordMutation(
                            documentId,
                            com.mal5odha.core.data.models.DocumentMutation(
                                type = com.mal5odha.core.data.models.MutationType.ADD_STROKE,
                                pageId = pageId,
                                stroke = ml5Strokes.lastOrNull(),
                                strokeId = stampedStroke.id,
                                description = "Inked stroke"
                            )
                        )

                        // Index handwriting via ML Kit OCR on background thread
                        val pageIdx = uiState.value.document?.pages?.indexOfFirst { it.id == pageId } ?: 0
                        if (pageIdx >= 0) {
                            handwritingIndexingService.indexStrokes(documentId, pageIdx, pageStrokesList)
                        }
                }
        }

        fun removeStroke(pageId: String, strokeId: String) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        val currentMap = _pageStrokes.value.toMutableMap()
                        val pageStrokesList = currentMap[pageId]?.toMutableList() ?: mutableListOf()
                        val removedStroke = pageStrokesList.find { it.id == strokeId }
                        pageStrokesList.removeAll { it.id == strokeId }
                        currentMap[pageId] = pageStrokesList
                        _pageStrokes.value = currentMap

                        if (removedStroke != null) {
                            getUndoRedoManager(pageId).removeStroke(removedStroke)
                        }

                        val ml5Strokes = pageStrokesList.map { it.toMl5() }
                        fileSystemRepository.savePageStrokes(documentId, pageId, ml5Strokes)

                        // Record mutation to journal
                        documentSyncEngine.recordMutation(
                            documentId,
                            com.mal5odha.core.data.models.DocumentMutation(
                                type = com.mal5odha.core.data.models.MutationType.REMOVE_STROKE,
                                pageId = pageId,
                                strokeId = strokeId,
                                description = "Erased stroke"
                            )
                        )
                }
        }

        private val _selectedStrokes = MutableStateFlow<List<com.mal5odha.core.ink.models.Stroke>>(emptyList())
        val selectedStrokes: StateFlow<List<com.mal5odha.core.ink.models.Stroke>> = _selectedStrokes.asStateFlow()
        private var lastActivePageId: String? = null

        fun setSelectedStrokes(strokes: List<com.mal5odha.core.ink.models.Stroke>, pageId: String? = null) {
            _selectedStrokes.value = strokes
            if (pageId != null) {
                lastActivePageId = pageId
            }
        }

        fun updateSelectedStrokesColor(
            newColorArgb: Int,
            pageId: String? = null,
            strokes: List<com.mal5odha.core.ink.models.Stroke>? = null
        ): List<com.mal5odha.core.ink.models.Stroke> {
            val targetPageId = pageId ?: lastActivePageId ?: uiState.value.document?.pages?.firstOrNull()?.id ?: return emptyList()
            val strokesToUpdate = strokes ?: _selectedStrokes.value
            if (strokesToUpdate.isEmpty()) return emptyList()
            val documentId = uiState.value.document?.id ?: return emptyList()

            val currentMap = _pageStrokes.value.toMutableMap()
            val pageStrokesList = currentMap[targetPageId]?.toMutableList() ?: mutableListOf()

            val updatedStrokes = strokesToUpdate.map { stroke ->
                val updated = stroke.copy(color = newColorArgb)
                updated.precomputePathGeometry()
                val idx = pageStrokesList.indexOfFirst { it.id == stroke.id }
                if (idx != -1) {
                    pageStrokesList[idx] = updated
                }
                updated
            }

            currentMap[targetPageId] = pageStrokesList
            _pageStrokes.value = currentMap
            _selectedStrokes.value = updatedStrokes

            viewModelScope.launch {
                val ml5Strokes = pageStrokesList.map { it.toMl5() }
                fileSystemRepository.savePageStrokes(documentId, targetPageId, ml5Strokes)
            }

            return updatedStrokes
        }

        fun updateSelectedStrokesColor(newColorArgb: Int) {
            updateSelectedStrokesColor(newColorArgb, lastActivePageId, _selectedStrokes.value)
        }

        fun saveTextAnnotation(
                pageId: String,
                annotation: com.mal5odha.core.ink.models.TextAnnotation
        ) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        val activeAudio = audioSyncManager.syncState.value
                        if (activeAudio.state == com.mal5odha.core.audio.AudioState.RECORDING &&
                                activeAudio.currentSessionId != null && annotation.audioSessionId == null
                        ) {
                                annotation.audioSessionId = activeAudio.currentSessionId
                                annotation.audioTimestampMs = audioSyncManager.getCurrentAudioElapsedMs()
                        }

                        val currentMap = _pageTextAnnotations.value.toMutableMap()
                        val list = currentMap[pageId]?.toMutableList() ?: mutableListOf()

                        val index = list.indexOfFirst { it.id == annotation.id }
                        if (index != -1) {
                                list[index] = annotation
                        } else {
                                list.add(annotation)
                        }

                        currentMap[pageId] = list
                        _pageTextAnnotations.value = currentMap

                        val ml5Annotations = list.map { it.toMl5() }
                        fileSystemRepository.savePageTextAnnotations(
                                documentId,
                                pageId,
                                ml5Annotations
                        )

                        // Index text annotations for universal search
                        val pageIdx = uiState.value.document?.pages?.indexOfFirst { it.id == pageId } ?: 0
                        if (pageIdx >= 0) {
                            searchEngine.indexTextAnnotations(documentId, pageIdx, list)
                        }
                }
        }

        fun startAudioRecording() {
                val docId = uiState.value.document?.id ?: return
                audioSyncManager.startRecording(docId)
        }

        fun pauseAudioRecording() {
                audioSyncManager.pauseRecording()
        }

        fun resumeAudioRecording() {
                audioSyncManager.resumeRecording()
        }

        fun stopAudioRecording() {
                audioSyncManager.stopRecording()
        }

        fun startAudioPlayback(filePath: String, startMs: Long = 0L) {
                audioSyncManager.startPlayback(filePath, startMs)
        }

        fun playPauseAudioPlayback() {
                val state = audioSyncManager.syncState.value.state
                if (state == com.mal5odha.core.audio.AudioState.PLAYING) {
                        audioSyncManager.pausePlayback()
                } else if (state == com.mal5odha.core.audio.AudioState.PLAYBACK_PAUSED) {
                        audioSyncManager.resumePlayback()
                } else {
                        val docId = uiState.value.document?.id ?: return
                        val audioDir = java.io.File(context.filesDir, "documents/$docId/audio")
                        val latestAudio = audioDir.listFiles()?.sortedByDescending { it.lastModified() }?.firstOrNull()
                        if (latestAudio != null) {
                                audioSyncManager.startPlayback(latestAudio.absolutePath, 0L)
                        }
                }
        }

        fun seekAudioTo(positionMs: Long) {
                audioSyncManager.seekTo(positionMs)
        }

        fun skipAudio(deltaMs: Long) {
                audioSyncManager.skipBy(deltaMs)
        }

        fun stopAudioPlayback() {
                audioSyncManager.stopPlayback()
        }

        fun seekAudioToStroke(strokeTimestampMs: Long) {
                _audioPlaybackPositionMs.value = strokeTimestampMs
                audioSyncManager.seekTo(strokeTimestampMs)
        }

        fun seekToStrokeAudio(stroke: com.mal5odha.core.ink.models.Stroke) {
                val docId = uiState.value.document?.id ?: return
                val targetTimestamp = if (stroke.timestampMs > 0L) stroke.timestampMs else stroke.audioTimestampMs
                val sessionId = stroke.audioSessionId
                val audioDir = java.io.File(context.filesDir, "documents/$docId/audio")
                val audioFile = if (sessionId != null) {
                    java.io.File(audioDir, "$sessionId.m4a")
                } else {
                    audioDir.listFiles { _, name -> name.endsWith(".m4a") }?.firstOrNull()
                }

                if (audioFile != null && audioFile.exists()) {
                        val currentFile = audioSyncManager.syncState.value.currentFilePath
                        if (currentFile == audioFile.absolutePath && audioSyncManager.syncState.value.state != com.mal5odha.core.audio.AudioState.IDLE) {
                                seekAudioToStroke(targetTimestamp)
                        } else {
                                audioSyncManager.startPlayback(audioFile.absolutePath, targetTimestamp)
                                _audioPlaybackPositionMs.value = targetTimestamp
                        }
                } else {
                        seekAudioToStroke(targetTimestamp)
                }
        }

        override fun onCleared() {
                super.onCleared()
                audioSyncManager.destroy()
                val docId = _uiState.value.document?.id
                if (docId != null) {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        documentSyncEngine.checkpointSnapshot(docId)
                    }
                }
        }

        fun saveMediaAnnotation(
                pageId: String,
                annotation: MediaAnnotation
        ) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        val currentMap = _pageMediaAnnotations.value.toMutableMap()
                        val list = currentMap[pageId]?.toMutableList() ?: mutableListOf()

                        val index = list.indexOfFirst { it.id == annotation.id }
                        if (index != -1) {
                                list[index] = annotation
                        } else {
                                list.add(annotation)
                        }

                        currentMap[pageId] = list
                        _pageMediaAnnotations.value = currentMap

                        val ml5Annotations = list.map { it.toMl5() }
                        fileSystemRepository.savePageMediaAnnotations(
                                documentId,
                                pageId,
                                ml5Annotations
                        )
                }
        }

        fun removeMediaAnnotation(pageId: String, annotationId: String) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        val currentMap = _pageMediaAnnotations.value.toMutableMap()
                        val list = currentMap[pageId]?.toMutableList() ?: mutableListOf()
                        val annotationToRemove = list.firstOrNull { it.id == annotationId } ?: return@launch
                        list.remove(annotationToRemove)
                        currentMap[pageId] = list
                        _pageMediaAnnotations.value = currentMap

                        try {
                                val file = java.io.File(annotationToRemove.localPath)
                                if (file.exists() && file.parentFile?.name == documentId) {
                                        file.delete()
                                }
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to delete image file: ${e.message}")
                        }

                        val ml5Annotations = list.map { it.toMl5() }
                        fileSystemRepository.savePageMediaAnnotations(
                                documentId,
                                pageId,
                                ml5Annotations
                        )
                }
        }

        fun removeTextAnnotation(pageId: String, annotationId: String) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        val currentMap = _pageTextAnnotations.value.toMutableMap()
                        val list = currentMap[pageId]?.toMutableList() ?: mutableListOf()
                        list.removeAll { it.id == annotationId }
                        currentMap[pageId] = list
                        _pageTextAnnotations.value = currentMap

                        val ml5Annotations = list.map { it.toMl5() }
                        fileSystemRepository.savePageTextAnnotations(
                                documentId,
                                pageId,
                                ml5Annotations
                        )
                }
        }

        fun addImageAnnotation(pageId: String, uri: android.net.Uri) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch(Dispatchers.IO) {
                        try {
                                val localPath = fileSystemRepository.copyUriToNoteDir(documentId, uri)
                                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                android.graphics.BitmapFactory.decodeFile(localPath, options)
                                val imgW = options.outWidth.coerceAtLeast(1)
                                val imgH = options.outHeight.coerceAtLeast(1)
                                val normW = 0.45f
                                val normH = (normW * (imgH.toFloat() / imgW.toFloat()) * (1f / 1.414f)).coerceIn(0.1f, 0.8f)

                                val newAnnotation = MediaAnnotation(
                                        localPath = localPath,
                                        x = 0.25f,
                                        y = 0.25f,
                                        width = normW,
                                        height = normH
                                )
                                saveMediaAnnotation(pageId, newAnnotation)
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to add image annotation: ${e.message}", e)
                        }
                }
        }

        private val _isExporting = MutableStateFlow(false)
        val isExporting = _isExporting.asStateFlow()

        private val _exportError = MutableStateFlow<String?>(null)
        val exportError = _exportError.asStateFlow()

        /** Switches the editor to display the page at the given index. */
        fun setCurrentPage(index: Int) {
                val doc = _uiState.value.document ?: return
                if (index < 0 || index >= doc.pages.size) return
                _uiState.update { it.copy(document = doc.copy(currentPageIndex = index)) }
        }

    private fun Ml5Page.toDomainPage(): DocumentPage {
        val bg = try {
            PageBackground.valueOf(backgroundType)
        } catch (e: Exception) {
            PageBackground.BLANK
        }
        return DocumentPage(
            id = id,
            order = orderIndex,
            background = bg,
            backgroundData = backgroundData,
            isBookmarked = isBookmarked,
            widthPt = if (widthPt > 0f) widthPt else 595f,
            heightPt = if (heightPt > 0f) heightPt else 842f
        )
    }

    /** Dynamically adds a new page to the document and updates state/persistence. */
    fun addNewPage(backgroundType: PageBackground = PageBackground.BLANK, atIndex: Int? = null) {
        val documentId = uiState.value.document?.id ?: return
        viewModelScope.launch {
            try {
                val currentMl5Pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
                val newPageId = "page_${UUID.randomUUID()}"
                val insertIndex = atIndex?.coerceIn(0, currentMl5Pages.size) ?: currentMl5Pages.size
                val newMl5Page = com.mal5odha.core.data.models.Ml5Page(
                    id = newPageId,
                    orderIndex = insertIndex,
                    backgroundType = backgroundType.name,
                    backgroundData = null,
                    widthPt = 595f,
                    heightPt = 842f
                )
                currentMl5Pages.add(insertIndex, newMl5Page)
                val reindexed = currentMl5Pages.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
                fileSystemRepository.saveNotePages(documentId, reindexed)

                val domainPages = reindexed.map { it.toDomainPage() }
                _uiState.update { state ->
                    val doc = state.document ?: return@update state
                    state.copy(document = doc.copy(pages = domainPages, currentPageIndex = insertIndex))
                }
                refreshThumbnail()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add new page: ${e.message}", e)
            }
        }
    }

        /** Reorders pages within the document and updates persistence. */
        fun reorderPages(fromIndex: Int, toIndex: Int) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        try {
                                val currentMl5Pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
                                if (fromIndex !in currentMl5Pages.indices || toIndex !in currentMl5Pages.indices) return@launch
                                val moved = currentMl5Pages.removeAt(fromIndex)
                                currentMl5Pages.add(toIndex, moved)
                                val reindexed = currentMl5Pages.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
                                fileSystemRepository.saveNotePages(documentId, reindexed)

                                val domainPages = reindexed.map { it.toDomainPage() }
                                _uiState.update { state ->
                                        val doc = state.document ?: return@update state
                                        state.copy(document = doc.copy(pages = domainPages, currentPageIndex = toIndex))
                                }
                                refreshThumbnail()
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to reorder pages", e)
                        }
                }
        }

        /** Duplicates an existing page along with its vector strokes and annotations. */
        fun duplicatePage(pageId: String) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        try {
                                val currentMl5Pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
                                val targetIndex = currentMl5Pages.indexOfFirst { it.id == pageId }
                                if (targetIndex == -1) return@launch
                                val sourcePage = currentMl5Pages[targetIndex]
                                val newPageId = "page_${UUID.randomUUID()}"
                                val newMl5Page = sourcePage.copy(id = newPageId, orderIndex = targetIndex + 1)
                                currentMl5Pages.add(targetIndex + 1, newMl5Page)
                                val reindexed = currentMl5Pages.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
                                fileSystemRepository.saveNotePages(documentId, reindexed)

                                // Clone strokes
                                val sourceStrokes = _pageStrokes.value[pageId] ?: emptyList()
                                val clonedStrokes = sourceStrokes.map { it.copy(id = UUID.randomUUID().toString()) }
                                _pageStrokes.update { it + (newPageId to clonedStrokes) }
                                fileSystemRepository.savePageStrokes(documentId, newPageId, clonedStrokes.map { it.toMl5() })

                                // Clone text annotations
                                val sourceTexts = _pageTextAnnotations.value[pageId] ?: emptyList()
                                val clonedTexts = sourceTexts.map { it.copy(id = UUID.randomUUID().toString()) }
                                _pageTextAnnotations.update { it + (newPageId to clonedTexts) }
                                fileSystemRepository.savePageTextAnnotations(documentId, newPageId, clonedTexts.map { it.toMl5() })

                                val domainPages = reindexed.map { it.toDomainPage() }
                                _uiState.update { state ->
                                        val doc = state.document ?: return@update state
                                        state.copy(document = doc.copy(pages = domainPages, currentPageIndex = targetIndex + 1))
                                }
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to duplicate page", e)
                        }
                }
        }

        /** Deletes a page from the document (if not the sole page). */
        fun deletePage(pageId: String) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        try {
                                val currentMl5Pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
                                if (currentMl5Pages.size <= 1) return@launch
                                val targetIndex = currentMl5Pages.indexOfFirst { it.id == pageId }
                                if (targetIndex == -1) return@launch
                                currentMl5Pages.removeAt(targetIndex)
                                val reindexed = currentMl5Pages.mapIndexed { idx, p -> p.copy(orderIndex = idx) }
                                fileSystemRepository.saveNotePages(documentId, reindexed)

                                _pageStrokes.update { it - pageId }
                                _pageTextAnnotations.update { it - pageId }

                                val domainPages = reindexed.map { it.toDomainPage() }
                                val newCurrentIndex = targetIndex.coerceAtMost(domainPages.size - 1)
                                _uiState.update { state ->
                                        val doc = state.document ?: return@update state
                                        state.copy(document = doc.copy(pages = domainPages, currentPageIndex = newCurrentIndex))
                                }
                                refreshThumbnail()
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to delete page", e)
                        }
                }
        }

        /** Changes an existing page's background template while retaining all drawn vector strokes. */
        fun changePageBackground(pageId: String, newBackground: PageBackground) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        try {
                                val currentMl5Pages = fileSystemRepository.loadNotePages(documentId).toMutableList()
                                val targetIndex = currentMl5Pages.indexOfFirst { it.id == pageId }
                                if (targetIndex == -1) return@launch
                                val updated = currentMl5Pages[targetIndex].copy(backgroundType = newBackground.name)
                                currentMl5Pages[targetIndex] = updated
                                fileSystemRepository.saveNotePages(documentId, currentMl5Pages)

                                val domainPages = currentMl5Pages.map { it.toDomainPage() }
                                _uiState.update { state ->
                                        val doc = state.document ?: return@update state
                                        state.copy(document = doc.copy(pages = domainPages))
                                }
                                refreshThumbnail()
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to change page background", e)
                        }
                }
        }

        /** Generates and persists a fresh Page 1 thumbnail for the document. */
        fun refreshThumbnail() {
                val doc = uiState.value.document ?: return
                val firstPage = doc.pages.firstOrNull() ?: return
                viewModelScope.launch(Dispatchers.IO) {
                        try {
                                val firstPageStrokes = _pageStrokes.value[firstPage.id]?.map { it.toMl5() }
                                val firstPageTexts = _pageTextAnnotations.value[firstPage.id]?.map { it.toMl5() }
                                val firstPageMedia = _pageMediaAnnotations.value[firstPage.id]?.map { it.toMl5() }
                                val ml5Page = com.mal5odha.core.data.models.Ml5Page(
                                        id = firstPage.id,
                                        orderIndex = firstPage.order,
                                        backgroundType = firstPage.background.name,
                                        backgroundData = firstPage.backgroundData
                                )
                                val thumbPath = thumbnailGenerator.generateThumbnail(
                                        context = context,
                                        documentId = doc.id,
                                        page = ml5Page,
                                        strokes = firstPageStrokes,
                                        paperColor = doc.paperColor,
                                        texts = firstPageTexts,
                                        media = firstPageMedia
                                )
                                if (thumbPath != null) {
                                        documentRepository.updateThumbnail(doc.id, thumbPath)
                                }
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to generate thumbnail: ${e.message}", e)
                        }
                }
        }

        fun exportCurrentDocument(context: android.content.Context, fileName: String, onSuccess: () -> Unit) {
                val documentId = uiState.value.document?.id ?: return
                viewModelScope.launch {
                        _isExporting.value = true
                        _exportError.value = null
                        try {
                                // Real Ml5Pages from FS
                                val realMl5Pages = fileSystemRepository.loadNotePages(documentId)

                                // Map strokes and text across all pages
                                val strokesMap =
                                        mutableMapOf<
                                                String,
                                                List<com.mal5odha.core.data.models.Ml5Stroke>>()
                                val textsMap =
                                        mutableMapOf<
                                                String,
                                                List<
                                                        com.mal5odha.core.data.models.Ml5TextAnnotation>>()

                                _pageStrokes.value.forEach { (pageId, list) ->
                                        strokesMap[pageId] = list.map { it.toMl5() }
                                }
                                _pageTextAnnotations.value.forEach { (pageId, list) ->
                                        textsMap[pageId] = list.map { it.toMl5() }
                                }

                                com.mal5odha.core.data.services.FileSystemServices.saveToDownloads(
                                        context = context,
                                        displayName = fileName,
                                        mimeType = "application/pdf"
                                ) { outputStream ->
                                        pdfExportService.exportToPdf(
                                                outputStream = outputStream,
                                                pages = realMl5Pages,
                                                pageStrokesMap = strokesMap,
                                                pageTextsMap = textsMap
                                        )
                                }

                                withContext(Dispatchers.Main) { onSuccess() }
                        } catch (e: Exception) {
                                e.printStackTrace()
                                _exportError.value = e.localizedMessage
                        } finally {
                                _isExporting.value = false
                        }
                }
        }

        // Helper mappings
        private fun com.mal5odha.core.ink.models.Stroke.toMl5(): Ml5Stroke {
                return Ml5Stroke(
                        id = id,
                        color = color,
                        width = width,
                        tool = tool.name,
                        points = points.map { Ml5Point(it.x, it.y, it.pressure, it.timestamp) },
                        audioSessionId = audioSessionId,
                        audioTimestampMs = audioTimestampMs,
                        layerId = layerId
                )
        }

        private fun Ml5Stroke.toDomain(): com.mal5odha.core.ink.models.Stroke {
                return com.mal5odha.core.ink.models.Stroke(
                        id = id,
                        color = color,
                        width = width,
                        tool = com.mal5odha.core.ink.models.InkTool.valueOf(tool),
                        points =
                                points
                                        .map {
                                                com.mal5odha.core.ink.models.Point(
                                                        it.x,
                                                        it.y,
                                                        it.pressure,
                                                        it.timestamp
                                                )
                                        }
                                        .toMutableList(),
                        audioSessionId = audioSessionId,
                        audioTimestampMs = audioTimestampMs,
                        layerId = layerId
                )
        }

        private fun com.mal5odha.core.ink.models.TextAnnotation.toMl5():
                com.mal5odha.core.data.models.Ml5TextAnnotation {
                return com.mal5odha.core.data.models.Ml5TextAnnotation(
                        id = id,
                        text = content,
                        x = xNorm,
                        y = yNorm,
                        width = widthNorm,
                        height = heightNorm,
                        fontSize = fontSizeSp,
                        color = colorHex.toInt(),
                        rotation = rotation,
                        backgroundColor = backgroundColor,
                        alignment = when (textAlign) {
                                androidx.compose.ui.text.style.TextAlign.Center -> "CENTER"
                                androidx.compose.ui.text.style.TextAlign.End, androidx.compose.ui.text.style.TextAlign.Right -> "END"
                                else -> "START"
                        },
                        audioSessionId = audioSessionId,
                        audioTimestampMs = audioTimestampMs,
                        isBold = isBold,
                        isItalic = isItalic,
                        isUnderline = isUnderline,
                        isCard = isCard,
                        title = title,
                        isPinned = isPinned
                )
        }

        private fun com.mal5odha.core.data.models.Ml5TextAnnotation.toDomain():
                com.mal5odha.core.ink.models.TextAnnotation {
                return com.mal5odha.core.ink.models.TextAnnotation(
                        id = id,
                        pageId = "",
                        xNorm = x,
                        yNorm = y,
                        widthNorm = width,
                        heightNorm = height,
                        content = text,
                        fontSizeSp = fontSize,
                        colorHex = (color.toLong() and 0xFFFFFFFFL),
                        isBold = isBold,
                        isItalic = isItalic,
                        isUnderline = isUnderline,
                        textAlign = when (alignment) {
                                "CENTER" -> androidx.compose.ui.text.style.TextAlign.Center
                                "END" -> androidx.compose.ui.text.style.TextAlign.End
                                else -> androidx.compose.ui.text.style.TextAlign.Start
                        },
                        rotation = rotation,
                        backgroundColor = backgroundColor,
                        audioSessionId = audioSessionId,
                        audioTimestampMs = audioTimestampMs,
                        isCard = isCard,
                        title = title,
                        isPinned = isPinned
                )
        }

        private fun MediaAnnotation.toMl5(): Ml5MediaAnnotation {
                return Ml5MediaAnnotation(
                        id = id,
                        localPath = localPath,
                        x = x,
                        y = y,
                        width = width,
                        height = height,
                        rotation = rotation
                )
        }

        private fun Ml5MediaAnnotation.toDomain(): MediaAnnotation {
                return MediaAnnotation(
                        id = id,
                        localPath = localPath,
                        x = x,
                        y = y,
                        width = width,
                        height = height,
                        rotation = rotation
                )
        }

        fun exportDocument(
                mode: com.mal5odha.core.pdf.PdfExportMode,
                onFinished: (android.net.Uri) -> Unit
        ) {
                viewModelScope.launch {
                        val doc = _uiState.value.document ?: return@launch
                        _exportState.value = ExportState(isExporting = true, progress = 0.05f)
                        try {
                                val uri = pdfExportService.exportDocument(
                                        documentTitle = doc.title,
                                        pages = doc.pages,
                                        pageStrokesMap = _pageStrokes.value,
                                        pageTextsMap = _pageTextAnnotations.value,
                                        pageMediaMap = _pageMediaAnnotations.value,
                                        mode = mode,
                                        onProgress = { p ->
                                                _exportState.value = _exportState.value.copy(progress = p)
                                        }
                                )
                                _exportState.value = ExportState(isExporting = false, progress = 1f, exportedUri = uri)
                                onFinished(uri)
                        } catch (e: Exception) {
                                Log.e(TAG, "Export document failed", e)
                                _exportState.value = ExportState(isExporting = false, error = e.localizedMessage ?: "Failed to export PDF")
                        }
                }
        }

        fun dismissExportState() {
                _exportState.value = ExportState()
        }
}
