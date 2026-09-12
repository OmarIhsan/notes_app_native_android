package com.mal5odha.core.data.repository

import android.content.Context
import com.google.gson.Gson
import com.mal5odha.core.data.models.DocumentMutation
import com.mal5odha.core.data.models.MutationType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enterprise-grade local-first append-only mutation journal and revision engine.
 * Prevents inode and file-descriptor bloat by storing mutations in an append-only flat log (mutations.log),
 * with automated checkpoints squashing state every 500 operations or on document close.
 */
@Singleton
class DocumentSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val fileSystemRepository: NoteFileSystemRepository
) {
    private val mutationCounters = ConcurrentHashMap<String, AtomicInteger>()
    private val CHECKPOINT_THRESHOLD = 500

    private fun getNoteDir(documentId: String): File {
        val dir = File(context.filesDir, "notes/$documentId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getJournalFile(documentId: String): File {
        return File(getNoteDir(documentId), "mutations.log")
    }

    /**
     * Appends an atomic mutation to the journal asynchronously.
     */
    suspend fun recordMutation(documentId: String, mutation: DocumentMutation) = withContext(Dispatchers.IO) {
        try {
            val journalFile = getJournalFile(documentId)
            val jsonLine = gson.toJson(mutation)
            BufferedWriter(FileWriter(journalFile, true)).use { writer ->
                writer.write(jsonLine)
                writer.newLine()
            }

            val counter = mutationCounters.getOrPut(documentId) { AtomicInteger(0) }
            if (counter.incrementAndGet() >= CHECKPOINT_THRESHOLD) {
                checkpointSnapshot(documentId)
                counter.set(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the chronological sequence of mutations from the journal.
     */
    suspend fun loadHistory(documentId: String): List<DocumentMutation> = withContext(Dispatchers.IO) {
        val journalFile = getJournalFile(documentId)
        if (!journalFile.exists()) return@withContext emptyList()

        val list = mutableListOf<DocumentMutation>()
        try {
            BufferedReader(FileReader(journalFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim()
                    if (!trimmed.isNullOrEmpty()) {
                        try {
                            val mutation = gson.fromJson(trimmed, DocumentMutation::class.java)
                            if (mutation != null) {
                                list.add(mutation)
                            }
                        } catch (e: Exception) {
                            // Skip corrupted lines
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Performs a checkpoint snapshot: squashes mutations and cleans up the journal log.
     */
    suspend fun checkpointSnapshot(documentId: String) = withContext(Dispatchers.IO) {
        val journalFile = getJournalFile(documentId)
        if (!journalFile.exists()) return@withContext

        // Rename current journal to checkpoint archive
        val archiveFile = File(getNoteDir(documentId), "mutations_archive_${System.currentTimeMillis()}.log")
        if (journalFile.renameTo(archiveFile)) {
            // Prune old archives, retaining at most 2
            val noteDir = getNoteDir(documentId)
            val archives = noteDir.listFiles { _, name -> name.startsWith("mutations_archive_") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            if (archives.size > 2) {
                archives.drop(2).forEach { it.delete() }
            }
        }
        mutationCounters[documentId]?.set(0)
    }

    /**
     * Reconstructs page strokes up to a target timestamp.
     */
    suspend fun getStrokesAtTimestamp(
        documentId: String,
        pageId: String,
        targetTimestamp: Long
    ): List<com.mal5odha.core.data.models.Ml5Stroke> = withContext(Dispatchers.IO) {
        val currentStrokes = fileSystemRepository.loadPageStrokes(documentId, pageId)
        val history = loadHistory(documentId).filter { it.pageId == pageId && it.timestamp > targetTimestamp }

        // Start with current strokes and rewind backward from future to target timestamp
        val workingList = currentStrokes.toMutableList()
        val sortedDescending = history.sortedByDescending { it.timestamp }

        for (mutation in sortedDescending) {
            when (mutation.type) {
                MutationType.ADD_STROKE -> {
                    // Stroke was added after target timestamp, remove it
                    if (mutation.stroke != null) {
                        workingList.removeAll { it.id == mutation.stroke.id }
                    } else if (mutation.strokeId != null) {
                        workingList.removeAll { it.id == mutation.strokeId }
                    }
                }
                MutationType.REMOVE_STROKE -> {
                    // Stroke was removed after target timestamp, restore it
                    if (mutation.stroke != null && workingList.none { it.id == mutation.stroke.id }) {
                        workingList.add(mutation.stroke)
                    }
                }
                else -> Unit
            }
        }
        workingList
    }
}
