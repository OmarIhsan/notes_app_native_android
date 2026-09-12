package com.mal5odha.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.mal5odha.core.pdf.cache.BitmapLruCache
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * High-DPI Multi-Scale PDF Rendering & Tile Caching Engine.
 * Supports zoom-dependent multi-scale rasterization (1.0x to 3.0x),
 * memory bounded LRU caching to eliminate OOMs during continuous textbook scrolling.
 */
class PdfRendererManager(
    private val context: Context,
    private val cache: BitmapLruCache = BitmapLruCache()
) {
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentFileHash: String = ""
    private val renderMutex = Mutex()

    suspend fun openPdf(file: File) = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            closeInternal()
            currentFileHash = "${file.name}_${file.length()}"
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
        }
    }

    /**
     * Renders a PDF page dynamically scaled to the current document zoom level.
     * Buckets zoom scales to 1.0x, 1.5x, 2.0x, and 3.0x to maximize LRU cache reuse.
     */
    suspend fun renderPageScaled(
        pageIndex: Int,
        baseWidth: Int,
        baseHeight: Int,
        zoomScale: Float = 1.0f
    ): Bitmap? = withContext(Dispatchers.IO) {
        val scaleBucket = when {
            zoomScale > 2.25f -> 3.0f
            zoomScale > 1.50f -> 2.0f
            zoomScale > 1.15f -> 1.5f
            else -> 1.0f
        }

        val cacheKey = "pdf_${currentFileHash}_p${pageIndex}_scale_${scaleBucket}"
        val cached = cache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        renderMutex.withLock {
            val doubleChecked = cache.get(cacheKey)
            if (doubleChecked != null && !doubleChecked.isRecycled) {
                return@withLock doubleChecked
            }

            val renderer = pdfRenderer ?: return@withLock null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null

            val targetWidth = (baseWidth * scaleBucket).toInt().coerceIn(100, 3840)
            val targetHeight = (baseHeight * scaleBucket).toInt().coerceIn(100, 3840)

            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            cache.put(cacheKey, bitmap)
            bitmap
        }
    }

    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? =
        renderPageScaled(pageIndex, width, height, 1.0f)

    fun getPageCount(): Int {
        return pdfRenderer?.pageCount ?: 0
    }

    private fun closeInternal() {
        pdfRenderer?.close()
        fileDescriptor?.close()
        pdfRenderer = null
        fileDescriptor = null
    }

    fun close() {
        closeInternal()
    }
}
