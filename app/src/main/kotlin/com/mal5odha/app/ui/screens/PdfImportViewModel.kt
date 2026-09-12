package com.mal5odha.app.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mal5odha.core.data.models.DocumentType
import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.models.UnifiedDocument
import com.mal5odha.core.data.repository.DocumentRepository
import com.mal5odha.core.data.repository.NoteFileSystemRepository
import com.mal5odha.core.pdf.PdfRendererService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PdfImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfRendererService: PdfRendererService,
    private val documentRepository: DocumentRepository,
    private val fileSystemRepository: NoteFileSystemRepository
) : ViewModel() {

    private val TAG = "PdfImportViewModel"

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    fun handlePdfImport(uri: Uri, documentTitle: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null

            try {
                val rawDisplayName = try {
                    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) cursor.getString(nameIndex) else null
                        } else null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to resolve DISPLAY_NAME: ${e.message}")
                    null
                } ?: documentTitle.ifBlank { uri.lastPathSegment } ?: "Imported Document"

                val cleanTitle = com.mal5odha.core.data.factory.DocumentNameFactory.fromImportedUri(rawDisplayName)
                val newDocId = UUID.randomUUID().toString()

                Log.d(TAG, "Initiating import for '$cleanTitle' (raw='$rawDisplayName', id=$newDocId) from uri=$uri")

                // 1. Create a new document metadata entry
                val newDocument = UnifiedDocument(
                    id = newDocId,
                    title = cleanTitle,
                    type = DocumentType.PDF,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )

                // 2. Setup document storage directory
                val documentDirectory = File(context.filesDir, "notes/$newDocId").apply {
                    if (!exists()) mkdirs()
                }

                // 3. Extract all PDF pages into images inside the document directory
                val extractedPages = pdfRendererService.extractPdfPagesToImages(uri, documentDirectory)

                if (extractedPages.isEmpty()) {
                    throw IllegalStateException("No pages could be extracted from PDF")
                }

                // Also copy original source PDF to notes/<docId>/source.pdf for outline/TOC extraction
                try {
                    val sourcePdfFile = File(documentDirectory, "source.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        sourcePdfFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not copy source PDF for outline extraction: ${e.message}")
                }

                Log.d(TAG, "Extracted ${extractedPages.size} pages successfully")

                // 4. Save metadata via Room repository
                documentRepository.saveDocument(newDocument)

                // 5. Build pages structure in exact 0-indexed order with native point dimensions
                val initialPages = extractedPages.mapIndexed { index, extracted ->
                    Ml5Page(
                        id = "page_$index",
                        orderIndex = index,
                        backgroundType = "IMAGE",
                        backgroundData = extracted.imagePath,
                        widthPt = extracted.widthPt,
                        heightPt = extracted.heightPt
                    )
                }

                // Persist note pages structure
                fileSystemRepository.saveNotePages(newDocument.id, initialPages)

                Log.d(TAG, "Note pages saved with true dimensions. Navigating to editor with id=$newDocId")

                // 6. Navigate to the editor on Main dispatcher
                withContext(Dispatchers.Main) {
                    onSuccess(newDocument.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing PDF: ${e.message}", e)
                _importError.value = "Failed to import PDF: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }
}
