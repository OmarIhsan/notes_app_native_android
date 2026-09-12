package com.mal5odha.core.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles safe stream extraction from SAF (Storage Access Framework) Uris into atomic internal
 * cache files, validated page counting, and ParcelFileDescriptor lifecycle management.
 */
@Singleton
class PdfDocumentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = "PdfDocumentManager"

    /**
     * Atomically copies a remote or content-provider PDF Uri into the app's internal cache,
     * validates that the file is non-empty (> 0 bytes), and returns a fresh seekable
     * [ParcelFileDescriptor] along with the verified total page count.
     */
    suspend fun cacheAndOpenPdf(uri: Uri, documentId: String = java.util.UUID.randomUUID().toString()): Pair<ParcelFileDescriptor, Int> =
        withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "pdf_cache").apply { if (!exists()) mkdirs() }
            val cacheFile = File(cacheDir, "${documentId}_doc.pdf")

            // Atomic stream copy from SAF to internal cache
            val bytesCopied = context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed to open input stream for URI: $uri")

            val fileLength = cacheFile.length()
            Log.d(TAG, "SAF Copy complete: path=${cacheFile.absolutePath}, bytesCopied=$bytesCopied, fileLength=$fileLength")

            if (!cacheFile.exists() || fileLength <= 0L) {
                throw IOException("Extracted PDF file is empty or missing: length=$fileLength bytes at ${cacheFile.absolutePath}")
            }

            // Measure accurate page count with temporary renderer
            val tempPfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                ?: throw IOException("Failed to open ParcelFileDescriptor for $cacheFile")

            val totalPages: Int
            try {
                val tempRenderer = PdfRenderer(tempPfd)
                totalPages = tempRenderer.pageCount
                tempRenderer.close()
            } finally {
                tempPfd.close()
            }

            Log.d(TAG, "PDF verified: totalPages=$totalPages for file ${cacheFile.name}")

            if (totalPages <= 0) {
                throw IOException("PDF has 0 pages or is corrupted: $cacheFile")
            }

            // Return active seekable PFD for the dedicated page renderer
            val activePfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                ?: throw IOException("Failed to open active ParcelFileDescriptor for $cacheFile")

            Pair(activePfd, totalPages)
        }
}
