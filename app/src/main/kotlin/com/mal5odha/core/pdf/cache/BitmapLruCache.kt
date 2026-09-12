package com.mal5odha.core.pdf.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory-bounded LRU cache for rendered PDF pages and background raster images.
 * Allocates up to 25% of available JVM heap to prevent OutOfMemoryErrors on high-DPI displays.
 */
@Singleton
class BitmapLruCache @Inject constructor() {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 4 // Use 25% of max available memory

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Retrieves a cached Bitmap by key or null if not present.
     */
    fun get(key: String): Bitmap? {
        return synchronized(memoryCache) {
            memoryCache.get(key)
        }
    }

    /**
     * Caches a Bitmap under the specified key.
     */
    fun put(key: String, bitmap: Bitmap) {
        synchronized(memoryCache) {
            if (memoryCache.get(key) == null) {
                memoryCache.put(key, bitmap)
            }
        }
    }

    /**
     * Loads a Bitmap from file with LRU caching.
     */
    fun loadFromFile(filePath: String): Bitmap? {
        val cached = get(filePath)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        val file = java.io.File(filePath)
        if (!file.exists() || file.length() <= 0L) return null

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(filePath, options) ?: return null
        put(filePath, decoded)
        return decoded
    }

    /**
     * Clears all cached bitmaps.
     */
    fun clear() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }
}
