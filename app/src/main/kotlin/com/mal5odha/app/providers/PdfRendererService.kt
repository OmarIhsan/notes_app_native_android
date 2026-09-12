package com.mal5odha.app.providers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfRendererService(private val context: Context) {

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    /**
     * Initializes the built-in Android PdfRenderer strictly using out-of-the-box system APIs
     * with no third party dependencies required.
     */
    @Throws(IOException::class)
    fun initRendererFromUri(uri: Uri) {
        val file = copyUriToInternalFile(uri)
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        fileDescriptor?.let {
            pdfRenderer = PdfRenderer(it)
        }
    }

    /**
     * Renders a specific page into a Bitmap strictly using PdfRenderer.
     */
    fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? {
        val renderer = pdfRenderer ?: return null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

        val page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Render onto the bitmap using the native Android utility
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        
        return bitmap
    }

    fun getPageCount(): Int {
        return pdfRenderer?.pageCount ?: 0
    }

    fun close() {
        pdfRenderer?.close()
        fileDescriptor?.close()
        pdfRenderer = null
        fileDescriptor = null
    }

    private fun copyUriToInternalFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open URI: $uri")
        val tempFile = File(context.cacheDir, "imported_pdf_${System.currentTimeMillis()}.pdf")
        
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        return tempFile
    }
}
