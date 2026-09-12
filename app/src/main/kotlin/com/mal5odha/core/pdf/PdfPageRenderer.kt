package com.mal5odha.core.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * Thread-safe, leak-free PDF page rasterizer.
 * Guarantees solid white background pre-fill to eliminate transparent/black rendering artifacts,
 * accurately measures source page dimensions, and enforces synchronized locks over native [PdfRenderer].
 */
class PdfPageRenderer(private val pfd: ParcelFileDescriptor) : AutoCloseable {

    private val TAG = "PdfPageRenderer"
    private val renderer: PdfRenderer = PdfRenderer(pfd)
    private val renderLock = Any()

    val pageCount: Int = renderer.pageCount

    /**
     * Inspects and returns the native point dimensions (widthPt, heightPt) of the given page.
     */
    fun getPageDimensions(pageIndex: Int): Pair<Float, Float> {
        require(pageIndex in 0 until pageCount) {
            "Index out of bounds: Requested $pageIndex, total pages: $pageCount"
        }
        synchronized(renderLock) {
            val page = renderer.openPage(pageIndex)
            try {
                return Pair(page.width.toFloat(), page.height.toFloat())
            } finally {
                page.close()
            }
        }
    }

    /**
     * Renders a PDF page to a destination bitmap scaled by [scaleMultiplier] from the page's intrinsic point dimensions.
     * Pre-fills the bitmap with solid white to eliminate black/transparent rendering artifacts.
     */
    fun renderPage(pageIndex: Int, scaleMultiplier: Float = 2.0f): Bitmap {
        require(pageIndex in 0 until pageCount) {
            "Index out of bounds: Requested $pageIndex, total pages: $pageCount"
        }

        synchronized(renderLock) {
            val page = renderer.openPage(pageIndex)
            try {
                val intrinsicWidth = page.width
                val intrinsicHeight = page.height

                val targetWidth = (intrinsicWidth * scaleMultiplier).toInt().coerceAtLeast(1)
                val targetHeight = (intrinsicHeight * scaleMultiplier).toInt().coerceAtLeast(1)

                Log.d(TAG, "Rendering page $pageIndex: intrinsic=${intrinsicWidth}x${intrinsicHeight}, target=${targetWidth}x${targetHeight}")

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

                // Eliminate black background: explicitly fill destination bitmap with pure white
                bitmap.eraseColor(Color.WHITE)

                page.render(
                    bitmap,
                    null, // Render full page
                    null, // Fit to destination bounds
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                return bitmap
            } finally {
                page.close()
            }
        }
    }

    /**
     * Renders a PDF page to explicit target dimensions with white background prefill.
     */
    fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        require(pageIndex in 0 until pageCount) {
            "Index out of bounds: Requested $pageIndex, total pages: $pageCount"
        }

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)

        synchronized(renderLock) {
            val page = renderer.openPage(pageIndex)
            try {
                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
            } finally {
                page.close()
            }
        }

        return bitmap
    }

    override fun close() {
        synchronized(renderLock) {
            try {
                renderer.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing PdfRenderer: ${e.message}")
            }
            try {
                pfd.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing ParcelFileDescriptor: ${e.message}")
            }
        }
    }
}
