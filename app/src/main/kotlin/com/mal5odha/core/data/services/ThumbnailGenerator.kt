package com.mal5odha.core.data.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.LruCache
import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.models.Ml5Stroke
import com.mal5odha.core.ink.math.CatmullRomInterpolator
import com.mal5odha.core.ink.models.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and caches compact, high-performance visual notebook previews (Page 1 flattening)
 * combining paper backgrounds, raster PDF tiles, and normalized vector strokes.
 * Target dimensions: 280x396 px (ISO A4 aspect ratio).
 */
@Singleton
class ThumbnailGenerator @Inject constructor() {

    companion object {
        const val THUMB_WIDTH = 280
        const val THUMB_HEIGHT = 396 // ISO A4 aspect ratio ~ 1:1.414

        // Memory-bounded in-memory LRU cache for thumbnail bitmaps (12.5% of heap)
        private val maxCacheMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
        private val thumbnailMemoryCache = object : LruCache<String, Bitmap>(maxCacheMemory.coerceAtLeast(2048)) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }

        fun getCachedBitmap(path: String): Bitmap? {
            return synchronized(thumbnailMemoryCache) {
                thumbnailMemoryCache.get(path)
            }
        }

        fun putCachedBitmap(path: String, bitmap: Bitmap) {
            synchronized(thumbnailMemoryCache) {
                thumbnailMemoryCache.put(path, bitmap)
            }
        }

        fun evict(path: String) {
            synchronized(thumbnailMemoryCache) {
                thumbnailMemoryCache.remove(path)
            }
        }
    }

    /**
     * Efficiently loads a thumbnail bitmap, checking the in-memory LRU cache first.
     */
    fun loadThumbnailBitmap(filePath: String): Bitmap? {
        val cached = getCachedBitmap(filePath)
        if (cached != null && !cached.isRecycled) {
            return cached
        }
        val file = File(filePath)
        if (!file.exists() || file.length() <= 0L) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                putCachedBitmap(filePath, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Rasterizes Page 1 of a document into a compact WebP thumbnail (280x396 px) and persists it to disk.
     */
    suspend fun generateThumbnail(
        context: Context,
        documentId: String,
        page: Ml5Page?,
        strokes: List<Ml5Stroke>?,
        paperColor: Int = -1,
        texts: List<com.mal5odha.core.data.models.Ml5TextAnnotation>? = null,
        media: List<com.mal5odha.core.data.models.Ml5MediaAnnotation>? = null
    ): String? = withContext(Dispatchers.Default) {
        try {
            val thumbDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
            val thumbFile = File(thumbDir, "${documentId}_thumb.webp")

            val bitmap = Bitmap.createBitmap(THUMB_WIDTH, THUMB_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Draw Page Background Layer
            val bgColor = if (paperColor != -1) paperColor else Color.WHITE
            canvas.drawColor(bgColor)

            if (page != null) {
                if (!page.backgroundData.isNullOrEmpty()) {
                    val bgFile = File(page.backgroundData)
                    if (bgFile.exists() && bgFile.length() > 0L) {
                        val bgBitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
                        if (bgBitmap != null) {
                            canvas.drawBitmap(
                                bgBitmap,
                                null,
                                android.graphics.Rect(0, 0, THUMB_WIDTH, THUMB_HEIGHT),
                                Paint().apply { isFilterBitmap = true }
                            )
                            bgBitmap.recycle()
                        }
                    }
                } else {
                    drawPattern(canvas, page.backgroundType)
                }
            }

            // 2. Draw Media Stickers (Under vector ink)
            if (!media.isNullOrEmpty()) {
                val mediaPaint = Paint().apply { isFilterBitmap = true }
                for (item in media) {
                    val file = File(item.localPath)
                    if (file.exists() && file.length() > 0L) {
                        val stickerBmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (stickerBmp != null) {
                            val left = item.x * THUMB_WIDTH
                            val top = item.y * THUMB_HEIGHT
                            val right = (item.x + item.width) * THUMB_WIDTH
                            val bottom = (item.y + item.height) * THUMB_HEIGHT
                            val dstRect = android.graphics.RectF(left, top, right, bottom)
                            canvas.save()
                            canvas.rotate(item.rotation, dstRect.centerX(), dstRect.centerY())
                            canvas.drawBitmap(stickerBmp, null, dstRect, mediaPaint)
                            canvas.restore()
                            stickerBmp.recycle()
                        }
                    }
                }
            }

            // 3. Draw Vector Ink Strokes (Page-Local Normalized -> Thumbnail Space)
            if (!strokes.isNullOrEmpty()) {
                val strokePath = Path()
                val inkPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                }

                for (stroke in strokes) {
                    inkPaint.color = stroke.color
                    // Scale stroke width proportionally to thumbnail resolution
                    inkPaint.strokeWidth = (stroke.width * (THUMB_WIDTH / 1080f)).coerceIn(1.2f, 8f)

                    if (stroke.tool == "HIGHLIGHTER") {
                        inkPaint.alpha = 120
                    } else {
                        inkPaint.alpha = 255
                    }

                    if (stroke.points.isNotEmpty()) {
                        val domainPoints = stroke.points.map { p ->
                            val px = if (p.x <= 1.0f) p.x * THUMB_WIDTH else (p.x / 1080f) * THUMB_WIDTH
                            val py = if (p.y <= 1.0f) p.y * THUMB_HEIGHT else (p.y / 1920f) * THUMB_HEIGHT
                            Point(px, py, p.pressure, p.timestamp)
                        }
                        CatmullRomInterpolator.createSmoothPath(domainPoints, strokePath)
                        canvas.drawPath(strokePath, inkPaint)
                    }
                }
            }

            // 4. Draw Rich Text Annotations
            if (!texts.isNullOrEmpty()) {
                val textPaint = Paint().apply { isAntiAlias = true }
                for (annotation in texts) {
                    val left = annotation.x * THUMB_WIDTH
                    val top = annotation.y * THUMB_HEIGHT
                    val right = (annotation.x + annotation.width) * THUMB_WIDTH
                    val bottom = (annotation.y + annotation.height) * THUMB_HEIGHT
                    val rect = android.graphics.RectF(left, top, right, bottom)

                    canvas.save()
                    canvas.rotate(annotation.rotation, rect.centerX(), rect.centerY())

                    if (annotation.backgroundColor != null) {
                        val bgPaint = Paint().apply {
                            color = annotation.backgroundColor
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(rect, 4f, 4f, bgPaint)
                    }

                    textPaint.color = annotation.color
                    textPaint.textSize = (annotation.fontSize * (THUMB_WIDTH / 600f)).coerceAtLeast(6f)
                    val lines = annotation.text.split("\n")
                    var lineY = top + textPaint.textSize + 2f
                    lines.forEach { line ->
                        canvas.drawText(line, left + 4f, lineY, textPaint)
                        lineY += textPaint.textSize * 1.2f
                    }
                    canvas.restore()
                }
            }

            // 3. Persist compressed thumbnail to internal storage
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(format, 85, out)
            }

            // Cache in memory for immediate dashboard display
            putCachedBitmap(thumbFile.absolutePath, bitmap)

            thumbFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawPattern(canvas: Canvas, backgroundType: String) {
        val patternPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            alpha = 75
        }

        when (backgroundType.uppercase()) {
            "RULED" -> {
                val step = 18f
                var y = step
                while (y < THUMB_HEIGHT) {
                    canvas.drawLine(0f, y, THUMB_WIDTH.toFloat(), y, patternPaint)
                    y += step
                }
            }
            "GRID" -> {
                val step = 18f
                var x = step
                while (x < THUMB_WIDTH) {
                    canvas.drawLine(x, 0f, x, THUMB_HEIGHT.toFloat(), patternPaint)
                    x += step
                }
                var y = step
                while (y < THUMB_HEIGHT) {
                    canvas.drawLine(0f, y, THUMB_WIDTH.toFloat(), y, patternPaint)
                    y += step
                }
            }
            "DOTTED" -> {
                val step = 18f
                var x = step
                while (x < THUMB_WIDTH) {
                    var y = step
                    while (y < THUMB_HEIGHT) {
                        canvas.drawCircle(x, y, 1.2f, patternPaint)
                        y += step
                    }
                    x += step
                }
            }
        }
    }
}
