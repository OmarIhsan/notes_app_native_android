package com.mal5odha.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.mal5odha.core.data.models.DocumentPage
import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.models.Ml5Stroke
import com.mal5odha.core.data.models.Ml5TextAnnotation
import com.mal5odha.core.data.models.PageBackground
import com.mal5odha.core.ink.math.CatmullRomInterpolator
import com.mal5odha.core.ink.models.MediaAnnotation
import com.mal5odha.core.ink.models.Point
import com.mal5odha.core.ink.models.Stroke
import com.mal5odha.core.ink.models.TextAnnotation
import com.mal5odha.core.ink.models.AcademicHighlighterTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PdfExportMode {
    VECTOR,
    FLATTENED
}

/**
 * Production PDF Document Export Pipeline:
 * Generates print-ready PDFs supporting both:
 * 1. Vector PDF: Native vector Bézier paths with infinite zoom sharpness.
 * 2. Flattened PDF: 300 DPI high-resolution rasterized pages.
 */
@Singleton
class PdfExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Exports a document's pages, vector strokes, and text annotations to a shareable PDF file via FileProvider.
     */
    suspend fun exportDocument(
        documentTitle: String,
        pages: List<DocumentPage>,
        pageStrokesMap: Map<String, List<Stroke>>,
        pageTextsMap: Map<String, List<TextAnnotation>> = emptyMap(),
        pageMediaMap: Map<String, List<MediaAnnotation>> = emptyMap(),
        mode: PdfExportMode = PdfExportMode.VECTOR,
        onProgress: (Float) -> Unit = {}
    ): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exported").apply { mkdirs() }
        val sanitizedTitle = com.mal5odha.core.data.factory.DocumentNameFactory.sanitizeForExport(documentTitle)
        val outputFile = File(exportDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        val pdfDocument = PdfDocument()

        try {
            val total = pages.size.coerceAtLeast(1)

            pages.forEachIndexed { index, page ->
                val pageWidth = if (page.widthPt > 0f) page.widthPt.toInt().coerceAtLeast(1) else 595
                val pageHeight = if (page.heightPt > 0f) page.heightPt.toInt().coerceAtLeast(1) else 842

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                val strokes = pageStrokesMap[page.id] ?: emptyList()
                val texts = pageTextsMap[page.id] ?: emptyList()
                val media = pageMediaMap[page.id] ?: emptyList()

                when (mode) {
                    PdfExportMode.VECTOR -> {
                        renderVectorPage(
                            canvas = canvas,
                            page = page,
                            width = pageWidth,
                            height = pageHeight,
                            strokes = strokes,
                            texts = texts,
                            media = media
                        )
                    }
                    PdfExportMode.FLATTENED -> {
                        renderFlattened300DpiPage(
                            canvas = canvas,
                            page = page,
                            pdfWidth = pageWidth,
                            pdfHeight = pageHeight,
                            strokes = strokes,
                            texts = texts,
                            media = media
                        )
                    }
                }

                pdfDocument.finishPage(pdfPage)
                onProgress((index + 1).toFloat() / total)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Backward-compatible exportToPdf implementation using OutputStream.
     */
    suspend fun exportToPdf(
        outputStream: OutputStream,
        pages: List<Ml5Page>,
        pageStrokesMap: Map<String, List<Ml5Stroke>>,
        pageTextsMap: Map<String, List<Ml5TextAnnotation>>
    ): Unit = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            pages.sortedBy { it.orderIndex }.forEachIndexed { index, page ->
                val pageWidth = if (page.widthPt > 0f) page.widthPt.toInt().coerceAtLeast(1) else 595
                val pageHeight = if (page.heightPt > 0f) page.heightPt.toInt().coerceAtLeast(1) else 842

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Background
                if (page.backgroundType == "IMAGE" && page.backgroundData != null) {
                    val bgFile = File(page.backgroundData)
                    if (bgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
                        canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, pageWidth, pageHeight), null)
                        bitmap.recycle()
                    } else {
                        canvas.drawColor(Color.WHITE)
                    }
                } else {
                    canvas.drawColor(Color.WHITE)
                    when (page.backgroundType) {
                        "RULED" -> drawRuledLines(canvas, pageWidth, pageHeight)
                        "GRID" -> drawGridLines(canvas, pageWidth, pageHeight)
                    }
                }

                // Strokes
                val strokes = pageStrokesMap[page.id] ?: emptyList()
                val inkPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }

                strokes.forEach { stroke ->
                    inkPaint.color = stroke.color
                    inkPaint.strokeWidth = stroke.width
                    if (stroke.points.isNotEmpty()) {
                        val path = Path()
                        val firstP = stroke.points[0]
                        val startX = if (firstP.x <= 1.0f) firstP.x * pageWidth else firstP.x
                        val startY = if (firstP.y <= 1.0f) firstP.y * pageHeight else firstP.y
                        path.moveTo(startX, startY)

                        for (i in 1 until stroke.points.size) {
                            val p = stroke.points[i]
                            val px = if (p.x <= 1.0f) p.x * pageWidth else p.x
                            val py = if (p.y <= 1.0f) p.y * pageHeight else p.y
                            path.lineTo(px, py)
                        }
                        canvas.drawPath(path, inkPaint)
                    }
                }

                // Texts
                val texts = pageTextsMap[page.id] ?: emptyList()
                val textPaint = Paint().apply { isAntiAlias = true }
                texts.forEach { annotation ->
                    textPaint.color = annotation.color
                    textPaint.textSize = annotation.fontSize
                    val textX = if (annotation.x <= 1.0f) annotation.x * pageWidth else annotation.x
                    val textY = if (annotation.y <= 1.0f) annotation.y * pageHeight else annotation.y
                    val lines = annotation.text.split("\n")
                    var currentY = textY + annotation.fontSize
                    lines.forEach { line ->
                        canvas.drawText(line, textX, currentY, textPaint)
                        currentY += annotation.fontSize * 1.2f
                    }
                }

                pdfDocument.finishPage(pdfPage)
            }

            outputStream.use { out -> pdfDocument.writeTo(out) }
        } finally {
            pdfDocument.close()
        }
    }

    private fun renderVectorPage(
        canvas: Canvas,
        page: DocumentPage,
        width: Int,
        height: Int,
        strokes: List<Stroke>,
        texts: List<TextAnnotation>,
        media: List<MediaAnnotation>
    ) {
        // 1. Background
        renderBackground(canvas, page, width, height)

        // 2. Media Stickers (under ink)
        renderMediaStickers(canvas, media, width, height)

        // 3. Vector Strokes using Catmull-Rom smoothed Bézier paths
        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        val strokePath = Path()

        strokes.forEach { stroke ->
            if (stroke.points.size >= 2) {
                strokePaint.color = stroke.color
                strokePaint.strokeWidth = stroke.width * (width.toFloat() / 1080f).coerceAtLeast(0.5f)

                if (stroke.tool == com.mal5odha.core.ink.models.InkTool.HIGHLIGHTER) {
                    strokePaint.color = AcademicHighlighterTokens.applyContrastSafeAlpha(stroke.color)
                    strokePaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                } else {
                    strokePaint.color = stroke.color
                    strokePaint.alpha = 255
                    strokePaint.xfermode = null
                }

                val pagePoints = stroke.points.map { p ->
                    val px = if (p.x <= 1.0f) p.x * width else p.x
                    val py = if (p.y <= 1.0f) p.y * height else p.y
                    Point(px, py, p.pressure, p.timestamp)
                }

                strokePath.reset()
                CatmullRomInterpolator.createSmoothPath(pagePoints, strokePath)
                canvas.drawPath(strokePath, strokePaint)
            }
        }

        // 4. Text Annotations
        renderTexts(canvas, texts, width, height, 1.0f)
    }

    private fun renderFlattened300DpiPage(
        canvas: Canvas,
        page: DocumentPage,
        pdfWidth: Int,
        pdfHeight: Int,
        strokes: List<Stroke>,
        texts: List<TextAnnotation>,
        media: List<MediaAnnotation>
    ) {
        // 300 DPI resolution scaling: 300 dpi / 72 pt = ~4.167x
        val dpiScale = (300f / 72f)
        val rasterWidth = (pdfWidth * dpiScale).toInt().coerceIn(800, 4096)
        val rasterHeight = (pdfHeight * dpiScale).toInt().coerceIn(800, 4096)

        val bitmap = Bitmap.createBitmap(rasterWidth, rasterHeight, Bitmap.Config.ARGB_8888)
        val bmpCanvas = Canvas(bitmap)

        // 1. Background
        renderBackground(bmpCanvas, page, rasterWidth, rasterHeight)

        // 2. Media Stickers
        renderMediaStickers(bmpCanvas, media, rasterWidth, rasterHeight)

        // 2. High-DPI Ink Strokes
        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val strokePath = Path()
        val scale = rasterWidth.toFloat() / 1080f

        strokes.forEach { stroke ->
            if (stroke.points.size >= 2) {
                strokePaint.color = stroke.color
                strokePaint.strokeWidth = stroke.width * scale

                if (stroke.tool == com.mal5odha.core.ink.models.InkTool.HIGHLIGHTER) {
                    strokePaint.color = AcademicHighlighterTokens.applyContrastSafeAlpha(stroke.color)
                    strokePaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                } else {
                    strokePaint.color = stroke.color
                    strokePaint.alpha = 255
                    strokePaint.xfermode = null
                }

                val pagePoints = stroke.points.map { p ->
                    val px = if (p.x <= 1.0f) p.x * rasterWidth else p.x
                    val py = if (p.y <= 1.0f) p.y * rasterHeight else p.y
                    Point(px, py, p.pressure, p.timestamp)
                }

                strokePath.reset()
                CatmullRomInterpolator.createSmoothPath(pagePoints, strokePath)
                bmpCanvas.drawPath(strokePath, strokePaint)
            }
        }

        // 3. High-DPI Text Annotations
        renderTexts(bmpCanvas, texts, rasterWidth, rasterHeight, rasterWidth.toFloat() / pdfWidth)

        // Draw rasterized high-DPI bitmap onto PDF canvas with filtering
        val filterPaint = Paint().apply { isFilterBitmap = true }
        canvas.drawBitmap(
            bitmap,
            null,
            android.graphics.Rect(0, 0, pdfWidth, pdfHeight),
            filterPaint
        )
        bitmap.recycle()
    }

    private fun renderBackground(canvas: Canvas, page: DocumentPage, width: Int, height: Int) {
        canvas.drawColor(Color.WHITE)

        if (!page.backgroundData.isNullOrBlank()) {
            val bgFile = File(page.backgroundData)
            if (bgFile.exists() && bgFile.length() > 0L) {
                try {
                    val bitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
                    if (bitmap != null) {
                        canvas.drawBitmap(
                            bitmap,
                            null,
                            android.graphics.Rect(0, 0, width, height),
                            Paint().apply { isFilterBitmap = true }
                        )
                        bitmap.recycle()
                        return
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        when (page.background) {
            PageBackground.RULED -> drawRuledLines(canvas, width, height)
            PageBackground.GRID -> drawGridLines(canvas, width, height)
            PageBackground.DOTTED -> drawDottedGrid(canvas, width, height)
            else -> Unit
        }
    }

    private fun renderMediaStickers(
        canvas: Canvas,
        mediaList: List<MediaAnnotation>,
        width: Int,
        height: Int
    ) {
        val filterPaint = Paint().apply { isFilterBitmap = true }
        mediaList.forEach { media ->
            val file = File(media.localPath)
            if (file.exists() && file.length() > 0L) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val left = media.x * width
                        val top = media.y * height
                        val right = (media.x + media.width) * width
                        val bottom = (media.y + media.height) * height
                        val dstRect = android.graphics.RectF(left, top, right, bottom)

                        canvas.save()
                        canvas.rotate(media.rotation, dstRect.centerX(), dstRect.centerY())
                        canvas.drawBitmap(bitmap, null, dstRect, filterPaint)
                        canvas.restore()
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun renderTexts(
        canvas: Canvas,
        texts: List<TextAnnotation>,
        width: Int,
        height: Int,
        fontScale: Float
    ) {
        val textPaint = Paint().apply { isAntiAlias = true }
        texts.forEach { annotation ->
            val left = annotation.x * width
            val top = annotation.y * height
            val boxW = annotation.width * width
            val boxH = annotation.height * height
            val rect = android.graphics.RectF(left, top, left + boxW, top + boxH)

            canvas.save()
            canvas.rotate(annotation.rotation, rect.centerX(), rect.centerY())

            if (annotation.backgroundColor != null) {
                val bgPaint = Paint().apply {
                    color = annotation.backgroundColor!!
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
            }

            textPaint.color = annotation.color
            textPaint.textSize = (annotation.fontSize * fontScale).coerceAtLeast(10f)
            val lines = annotation.text.split("\n")
            var currentY = top + textPaint.textSize + 6f
            lines.forEach { line ->
                canvas.drawText(line, left + 8f, currentY, textPaint)
                currentY += textPaint.textSize * 1.25f
            }
            canvas.restore()
        }
    }

    private fun drawRuledLines(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val lineSpacing = height / 32f
        var y = lineSpacing * 2f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += lineSpacing
        }
    }

    private fun drawGridLines(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#EDEDED")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val spacing = width / 24f
        var x = spacing
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            x += spacing
        }
        var y = spacing
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += spacing
        }
    }

    private fun drawDottedGrid(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            style = Paint.Style.FILL
        }
        val spacing = width / 24f
        val radius = 1.2f
        var x = spacing
        while (x < width) {
            var y = spacing
            while (y < height) {
                canvas.drawCircle(x, y, radius, paint)
                y += spacing
            }
            x += spacing
        }
    }
}
