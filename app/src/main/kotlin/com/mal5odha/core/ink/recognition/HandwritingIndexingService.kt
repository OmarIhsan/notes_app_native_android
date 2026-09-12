package com.mal5odha.core.ink.recognition

import android.content.Context
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.mal5odha.core.data.local.SearchDao
import com.mal5odha.core.data.local.SearchIndexEntity
import com.mal5odha.core.ink.models.Stroke
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * High-performance background service managing ML Kit Digital Ink Recognition,
 * offline-tolerant model caching, and Room FTS indexing for vector ink handwriting.
 */
@Singleton
class HandwritingIndexingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchDao: SearchDao
) {
    companion object {
        private const val TAG = "HandwritingIndexing"
        private const val DEFAULT_LANGUAGE = "en-US"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val remoteModelManager = RemoteModelManager.getInstance()

    private var model: DigitalInkRecognitionModel? = null
    private var recognizer: DigitalInkRecognizer? = null
    private var isModelDownloaded = false
    private var isDownloading = false

    // Offline pending queue to avoid dropped recognition jobs
    private data class PendingJob(
        val documentId: String,
        val pageIndex: Int,
        val strokes: List<Stroke>
    )
    private val pendingQueue = ConcurrentLinkedQueue<PendingJob>()

    init {
        initializeModel(DEFAULT_LANGUAGE)
    }

    /**
     * Initializes the recognition model for the target language.
     */
    fun initializeModel(languageTag: String = DEFAULT_LANGUAGE) {
        serviceScope.launch {
            try {
                val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
                if (modelIdentifier == null) {
                    Log.w(TAG, "No model identifier available for language: $languageTag")
                    return@launch
                }

                val recognitionModel = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                model = recognitionModel
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(recognitionModel).build()
                )

                checkAndDownloadModel(recognitionModel)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Digital Ink Model: ${e.message}", e)
            }
        }
    }

    private suspend fun checkAndDownloadModel(recognitionModel: DigitalInkRecognitionModel): Unit =
        withContext(Dispatchers.IO) {
            try {
                val downloaded = remoteModelManager.isModelDownloaded(recognitionModel).await()
                isModelDownloaded = downloaded

                if (!downloaded && !isDownloading) {
                    isDownloading = true
                    Log.d(TAG, "Starting download of Digital Ink recognition pack for $DEFAULT_LANGUAGE")
                    val conditions = DownloadConditions.Builder().build()
                    remoteModelManager.download(recognitionModel, conditions).await()
                    isModelDownloaded = true
                    isDownloading = false
                    Log.d(TAG, "Digital Ink recognition pack downloaded successfully")
                    drainPendingQueue()
                } else if (downloaded) {
                    drainPendingQueue()
                }
            } catch (e: Exception) {
                isDownloading = false
                Log.w(TAG, "Digital Ink model download deferred or failed: ${e.message}")
            }
            Unit
        }

    /**
     * Enqueues a page's vector strokes for asynchronous handwriting indexing.
     * Guaranteed never to block the main inking thread.
     */
    fun indexStrokes(documentId: String, pageIndex: Int, strokes: List<Stroke>) {
        if (strokes.isEmpty()) return

        serviceScope.launch {
            if (!isModelDownloaded) {
                Log.d(TAG, "Model not downloaded yet. Queuing ${strokes.size} strokes for offline sync.")
                pendingQueue.add(PendingJob(documentId, pageIndex, strokes))
                model?.let { checkAndDownloadModel(it) }
                return@launch
            }

            processRecognition(documentId, pageIndex, strokes)
        }
    }

    private suspend fun drainPendingQueue() {
        while (pendingQueue.isNotEmpty()) {
            val job = pendingQueue.poll() ?: break
            processRecognition(job.documentId, job.pageIndex, job.strokes)
        }
    }

    private suspend fun processRecognition(
        documentId: String,
        pageIndex: Int,
        strokes: List<Stroke>
    ) = withContext(Dispatchers.Default) {
        val client = recognizer ?: return@withContext
        if (strokes.isEmpty()) return@withContext

        try {
            // Build ML Kit Ink representation
            val inkBuilder = Ink.builder()
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (stroke in strokes) {
                if (stroke.points.isEmpty()) continue
                val strokeBuilder = Ink.Stroke.builder()
                for (point in stroke.points) {
                    strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, point.timestamp))
                    if (point.x < minX) minX = point.x
                    if (point.y < minY) minY = point.y
                    if (point.x > maxX) maxX = point.x
                    if (point.y > maxY) maxY = point.y
                }
                inkBuilder.addStroke(strokeBuilder.build())
            }

            val ink = inkBuilder.build()
            if (ink.strokes.isEmpty()) return@withContext

            val result = client.recognize(ink).await()
            val candidate = result.candidates.firstOrNull() ?: return@withContext
            val recognizedText = candidate.text.trim()

            if (recognizedText.isNotBlank()) {
                Log.d(TAG, "Recognized handwriting on page $pageIndex: '$recognizedText'")

                // Clear previous stroke OCR records for this page before inserting updated recognized text
                searchDao.clearPageIndices(documentId, pageIndex, "STROKE_OCR")

                val bounds = RectF(
                    if (minX == Float.MAX_VALUE) 0f else minX,
                    if (minY == Float.MAX_VALUE) 0f else minY,
                    if (maxX == Float.MIN_VALUE) 1f else maxX,
                    if (maxY == Float.MIN_VALUE) 1f else maxY
                )

                val entity = SearchIndexEntity(
                    documentId = documentId,
                    pageIndex = pageIndex,
                    text = recognizedText,
                    sourceType = "STROKE_OCR",
                    boundsLeft = bounds.left,
                    boundsTop = bounds.top,
                    boundsRight = bounds.right,
                    boundsBottom = bounds.bottom
                )

                searchDao.insertIndex(entity)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognition error on document $documentId page $pageIndex: ${e.message}")
        }
    }

    /**
     * Cleans up recognizer resources when not in use.
     */
    fun close() {
        recognizer?.close()
        recognizer = null
    }
}
