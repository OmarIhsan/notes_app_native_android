package com.mal5odha.core.data.services

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper object to interact with device file systems,
 * particularly dealing with scoped storage limits.
 */
object FileSystemServices {
    suspend fun saveToDownloads(
        context: Context,
        displayName: String,
        mimeType: String,
        block: suspend (OutputStream) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { resolver.openOutputStream(it) }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, displayName)
                FileOutputStream(file)
            }

            outputStream?.use { stream ->
                block(stream)
            } ?: throw IllegalStateException("Could not create output stream for $displayName")
        }
    }
}
