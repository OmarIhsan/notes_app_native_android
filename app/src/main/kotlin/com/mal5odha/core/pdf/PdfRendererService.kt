package com.mal5odha.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents an extracted PDF page image with its intrinsic points dimensions.
 */
data class ExtractedPdfPage(
    val imagePath: String,
    val widthPt: Float,
    val heightPt: Float
)

/**
 * Service responsible for high-resolution page extraction from PDFs into localized storage.
 */
@Singleton
class PdfRendererService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfDocumentManager: PdfDocumentManager
) {

    private val TAG = "PdfRendererService"

    /**
     * Extracts all pages from a PDF Uri into high-resolution JPG files in the specified target
     * directory. Returns a list of [ExtractedPdfPage] objects containing the file path and native dimensions.
     */
    suspend fun extractPdfPagesToImages(pdfUri: Uri, targetDirectory: File): List<ExtractedPdfPage> =
        withContext(Dispatchers.IO) {
            val extractedPages = mutableListOf<ExtractedPdfPage>()

            val (pfd, pageCount) = pdfDocumentManager.cacheAndOpenPdf(pdfUri)
            val pageRenderer = PdfPageRenderer(pfd)

            try {
                val densityDpi = context.resources.displayMetrics.densityDpi
                // Calculate display scale (e.g. 2.0x to 3.0x for crisp rendering on high-DPI displays)
                val scaleMultiplier = (densityDpi / 160f).coerceIn(1.5f, 3.0f)

                Log.d(TAG, "Starting extraction of $pageCount pages at scale $scaleMultiplier")

                for (i in 0 until pageCount) {
                    val (nativeWidthPt, nativeHeightPt) = pageRenderer.getPageDimensions(i)
                    val bitmap = pageRenderer.renderPage(i, scaleMultiplier = scaleMultiplier)

                    val imageFile = File(targetDirectory, "pdf_bg_p$i.jpg")
                    FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }

                    extractedPages.add(
                        ExtractedPdfPage(
                            imagePath = imageFile.absolutePath,
                            widthPt = nativeWidthPt,
                            heightPt = nativeHeightPt
                        )
                    )
                    bitmap.recycle()
                    Log.d(TAG, "Extracted page $i (${nativeWidthPt}x${nativeHeightPt} pt) -> ${imageFile.absolutePath}")
                }
            } finally {
                pageRenderer.close()
            }

            return@withContext extractedPages
        }
}
