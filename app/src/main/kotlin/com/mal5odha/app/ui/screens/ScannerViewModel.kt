package com.mal5odha.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.mal5odha.core.data.models.DocumentType
import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.models.UnifiedDocument
import com.mal5odha.core.data.repository.DocumentRepository
import com.mal5odha.core.data.repository.NoteFileSystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Processing : ScannerUiState()
    data class Success(val documentId: String) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    private val noteFileSystemRepository: NoteFileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState

    fun processScanResult(result: GmsDocumentScanningResult) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            try {
                val documentId = UUID.randomUUID().toString()
                val pages = mutableListOf<Ml5Page>()
                
                // 1. Process pages
                result.pages?.let { scanPages ->
                    scanPages.forEachIndexed { index, page ->
                        val imageUri = page.imageUri
                        val destFile = copyImageToNoteDir(documentId, index, imageUri)
                        
                        // Extract native dimensions from image to preserve true paper aspect ratio
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(destFile.absolutePath, options)
                        val imgW = options.outWidth.toFloat()
                        val imgH = options.outHeight.toFloat()
                        val (pageW, pageH) = if (imgW > 0f && imgH > 0f) {
                            // Scale dimensions to normalized PDF point baseline (e.g., width 595pt)
                            val scale = 595f / imgW
                            Pair(595f, imgH * scale)
                        } else {
                            Pair(595f, 842f)
                        }

                        pages.add(
                            Ml5Page(
                                id = UUID.randomUUID().toString(),
                                orderIndex = index,
                                backgroundType = "IMAGE",
                                backgroundData = destFile.absolutePath,
                                widthPt = pageW,
                                heightPt = pageH
                            )
                        )
                    }
                }

                // 2. Create UnifiedDocument
                val newDoc = UnifiedDocument(
                    id = documentId,
                    title = "Scanned Doc ${System.currentTimeMillis()}",
                    type = DocumentType.NOTE,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )

                // 3. Save to Repositories
                documentRepository.saveDocument(newDoc)
                noteFileSystemRepository.saveNotePages(documentId, pages)

                _uiState.value = ScannerUiState.Success(documentId)
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private suspend fun copyImageToNoteDir(docId: String, index: Int, uri: Uri): File = withContext(Dispatchers.IO) {
        val noteDir = File(context.filesDir, "notes/$docId")
        if (!noteDir.exists()) noteDir.mkdirs()
        
        val destFile = File(noteDir, "page_$index.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Failed to open scanned image")
        
        destFile
    }

    fun resetState() {
        _uiState.value = ScannerUiState.Idle
    }
}
