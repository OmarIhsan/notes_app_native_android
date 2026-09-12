package com.mal5odha.core.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mal5odha.core.data.models.Ml5Page
import com.mal5odha.core.data.models.Ml5Stroke
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class NoteFileSystemRepositoryImpl
@Inject
constructor(@ApplicationContext private val context: Context, private val gson: Gson) :
        NoteFileSystemRepository {

    private fun getNoteDir(documentId: String): File {
        val dir = File(context.filesDir, "notes/$documentId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override suspend fun saveNotePages(documentId: String, pages: List<Ml5Page>) {
        withContext(Dispatchers.IO) {
            val noteDir = getNoteDir(documentId)
            val pagesFile = File(noteDir, "pages.json")
            val tempFile = File(noteDir, "pages.json.tmp")
            val json = gson.toJson(pages)
            tempFile.writeText(json)
            tempFile.renameTo(pagesFile)
        }
    }

    override suspend fun loadNotePages(documentId: String): List<Ml5Page> =
            withContext(Dispatchers.IO) {
                val noteDir = getNoteDir(documentId)
                val pagesFile = File(noteDir, "pages.json")
                if (!pagesFile.exists()) return@withContext emptyList()
                val json = pagesFile.readText()
                val type = object : TypeToken<List<Ml5Page>>() {}.type
                val pages: List<Ml5Page> = gson.fromJson(json, type) ?: emptyList()
                pages.sortedBy { it.orderIndex }
            }

    override suspend fun savePageStrokes(
            documentId: String,
            pageId: String,
            strokes: List<Ml5Stroke>
    ) {
        withContext(Dispatchers.IO) {
            val noteDir = getNoteDir(documentId)
            val binFile = File(noteDir, "strokes_$pageId.bin")
            val tempFile = File(noteDir, "strokes_$pageId.bin.tmp")
            tempFile.outputStream().use { out ->
                com.mal5odha.core.data.serializer.BinaryStrokeSerializer.serialize(strokes, out)
            }
            tempFile.renameTo(binFile)
        }
    }

    override suspend fun loadPageStrokes(documentId: String, pageId: String): List<Ml5Stroke> =
            withContext(Dispatchers.IO) {
                val noteDir = getNoteDir(documentId)
                val binFile = File(noteDir, "strokes_$pageId.bin")
                if (binFile.exists()) {
                    try {
                        binFile.inputStream().use { input ->
                            return@withContext com.mal5odha.core.data.serializer.BinaryStrokeSerializer.deserialize(input)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Fallback to JSON format for existing documents
                val jsonFile = File(noteDir, "strokes_$pageId.json")
                if (!jsonFile.exists()) return@withContext emptyList()
                val json = jsonFile.readText()
                val type = object : TypeToken<List<Ml5Stroke>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            }

    override suspend fun savePageTextAnnotations(
            documentId: String,
            pageId: String,
            annotations: List<com.mal5odha.core.data.models.Ml5TextAnnotation>
    ) {
        withContext(Dispatchers.IO) {
            val noteDir = getNoteDir(documentId)
            val textFile = File(noteDir, "text_$pageId.json")
            val tempFile = File(noteDir, "text_$pageId.json.tmp")
            val json = gson.toJson(annotations)
            tempFile.writeText(json)
            tempFile.renameTo(textFile)
        }
    }

    override suspend fun loadPageTextAnnotations(
            documentId: String,
            pageId: String
    ): List<com.mal5odha.core.data.models.Ml5TextAnnotation> =
            withContext(Dispatchers.IO) {
                val noteDir = getNoteDir(documentId)
                val textFile = File(noteDir, "text_$pageId.json")
                if (!textFile.exists()) return@withContext emptyList()
                val json = textFile.readText()
                val type =
                        object :
                                        TypeToken<
                                                List<
                                                        com.mal5odha.core.data.models.Ml5TextAnnotation>>() {}
                                .type
                gson.fromJson(json, type) ?: emptyList()
            }

    override suspend fun savePageMediaAnnotations(
            documentId: String,
            pageId: String,
            annotations: List<com.mal5odha.core.data.models.Ml5MediaAnnotation>
    ) {
        withContext(Dispatchers.IO) {
            val noteDir = getNoteDir(documentId)
            val mediaFile = File(noteDir, "media_$pageId.json")
            val tempFile = File(noteDir, "media_$pageId.json.tmp")
            val json = gson.toJson(annotations)
            tempFile.writeText(json)
            tempFile.renameTo(mediaFile)
        }
    }

    override suspend fun loadPageMediaAnnotations(
            documentId: String,
            pageId: String
    ): List<com.mal5odha.core.data.models.Ml5MediaAnnotation> =
            withContext(Dispatchers.IO) {
                val noteDir = getNoteDir(documentId)
                val mediaFile = File(noteDir, "media_$pageId.json")
                if (!mediaFile.exists()) return@withContext emptyList()
                val json = mediaFile.readText()
                val type =
                        object :
                                        TypeToken<
                                                List<
                                                        com.mal5odha.core.data.models.Ml5MediaAnnotation>>() {}
                                .type
                gson.fromJson(json, type) ?: emptyList()
            }

    override suspend fun deleteNoteFile(documentId: String) =
            withContext(Dispatchers.IO) {
                val noteDir = File(context.filesDir, "notes/$documentId")
                if (noteDir.exists()) {
                    noteDir.deleteRecursively()
                }
            }

    override suspend fun purgeNoteStructure(documentId: String) =
            deleteNoteFile(documentId)

    override suspend fun deletePageData(documentId: String, pageId: String): Unit =
            withContext(Dispatchers.IO) {
                val noteDir = getNoteDir(documentId)
                File(noteDir, "strokes_$pageId.bin").also { if (it.exists()) it.delete() }
                File(noteDir, "strokes_$pageId.json").also { if (it.exists()) it.delete() }
                File(noteDir, "text_$pageId.json").also { if (it.exists()) it.delete() }
                File(noteDir, "media_$pageId.json").also { if (it.exists()) it.delete() }
            }

    override suspend fun copyUriToNoteDir(documentId: String, uri: android.net.Uri): String =
            withContext(Dispatchers.IO) {
                val noteDir = getNoteDir(documentId)
                val fileExtension = context.contentResolver.getType(uri)?.let { mime ->
                    android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                } ?: "jpg"
                val fileName = "img_${UUID.randomUUID()}.$fileExtension"
                val destFile = File(noteDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw java.lang.IllegalStateException("Failed to open input stream for Uri: $uri")
                destFile.absolutePath
            }

    override suspend fun duplicateNoteDirectory(sourceDocId: String, targetDocId: String) =
        withContext(Dispatchers.IO) {
            val srcDir = File(context.filesDir, "notes/$sourceDocId")
            val dstDir = getNoteDir(targetDocId)
            if (srcDir.exists() && srcDir.isDirectory) {
                srcDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val dest = File(dstDir, file.name)
                        file.copyTo(dest, overwrite = true)
                    }
                }
            }
        }

    override suspend fun getNoteDirectorySizeBytes(documentId: String): Long =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "notes/$documentId")
            if (!dir.exists() || !dir.isDirectory) return@withContext 0L
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
}
