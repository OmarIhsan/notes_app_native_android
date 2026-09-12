package com.mal5odha.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class AudioState {
    IDLE,
    RECORDING,
    RECORDING_PAUSED,
    PLAYING,
    PLAYBACK_PAUSED
}

data class AudioSyncState(
    val state: AudioState = AudioState.IDLE,
    val currentSessionId: String? = null,
    val currentFilePath: String? = null,
    val elapsedMs: Long = 0L,
    val durationMs: Long = 0L,
    val amplitudeLevel: Float = 0f,
    val waveformHistory: List<Float> = emptyList()
)

/**
 * High-performance, lifecycle-aware Audio Recording & Playback Service
 * with millisecond synchronization for live inking and interactive note replay.
 */
@Singleton
class AudioSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "AudioSyncManager"

    private val _syncState = MutableStateFlow(AudioSyncState())
    val syncState: StateFlow<AudioSyncState> = _syncState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null

    private var recordingStartTime: Long = 0L
    private var accumulatedPauseTime: Long = 0L
    private var pauseStartTime: Long = 0L

    /**
     * Starts voice recording for the given document using AAC/M4A.
     */
    fun startRecording(documentId: String): String? {
        if (_syncState.value.state == AudioState.RECORDING) return _syncState.value.currentSessionId

        stopPlayback()

        val audioDir = File(context.filesDir, "documents/$documentId/audio").apply { if (!exists()) mkdirs() }
        val sessionId = UUID.randomUUID().toString()
        val outputFile = File(audioDir, "$sessionId.m4a")

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            accumulatedPauseTime = 0L

            _syncState.value = AudioSyncState(
                state = AudioState.RECORDING,
                currentSessionId = sessionId,
                currentFilePath = outputFile.absolutePath,
                elapsedMs = 0L,
                durationMs = 0L,
                waveformHistory = emptyList()
            )

            startRecordingTicker()
            Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
            return sessionId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording: ${e.message}", e)
            stopRecording()
            return null
        }
    }

    /**
     * Pauses the active audio recording session.
     */
    fun pauseRecording() {
        if (_syncState.value.state != AudioState.RECORDING) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                pauseStartTime = System.currentTimeMillis()
                _syncState.update { it.copy(state = AudioState.RECORDING_PAUSED) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pause recording error: ${e.message}")
        }
    }

    /**
     * Resumes a paused recording session.
     */
    fun resumeRecording() {
        if (_syncState.value.state != AudioState.RECORDING_PAUSED) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                accumulatedPauseTime += (System.currentTimeMillis() - pauseStartTime)
                _syncState.update { it.copy(state = AudioState.RECORDING) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Resume recording error: ${e.message}")
        }
    }

    /**
     * Stops the active recording session and finalizes metadata.
     */
    fun stopRecording(): String? {
        val currentPath = _syncState.value.currentFilePath
        tickerJob?.cancel()
        tickerJob = null

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop recorder error: ${e.message}")
        } finally {
            mediaRecorder = null
        }

        val finalDuration = _syncState.value.elapsedMs
        _syncState.update {
            it.copy(
                state = AudioState.IDLE,
                durationMs = finalDuration,
                amplitudeLevel = 0f
            )
        }
        return currentPath
    }

    /**
     * Starts note replay playback from an existing recording file.
     */
    fun startPlayback(filePath: String, startPositionMs: Long = 0L) {
        stopRecording()
        stopPlayback()

        val file = File(filePath)
        if (!file.exists() || file.length() <= 0L) return

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                if (startPositionMs > 0L) {
                    seekTo(startPositionMs.toInt())
                }
                start()
            }

            mediaPlayer = player
            val totalDuration = player.duration.toLong().coerceAtLeast(0L)

            _syncState.value = AudioSyncState(
                state = AudioState.PLAYING,
                currentFilePath = filePath,
                elapsedMs = startPositionMs,
                durationMs = totalDuration
            )

            player.setOnCompletionListener {
                _syncState.update { it.copy(state = AudioState.IDLE, elapsedMs = 0L) }
                stopPlayback()
            }

            startPlaybackTicker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback: ${e.message}", e)
            stopPlayback()
        }
    }

    fun pausePlayback() {
        if (_syncState.value.state != AudioState.PLAYING) return
        mediaPlayer?.pause()
        _syncState.update { it.copy(state = AudioState.PLAYBACK_PAUSED) }
    }

    fun resumePlayback() {
        if (_syncState.value.state != AudioState.PLAYBACK_PAUSED) return
        mediaPlayer?.start()
        _syncState.update { it.copy(state = AudioState.PLAYING) }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _syncState.update { it.copy(elapsedMs = positionMs) }
    }

    fun skipBy(deltaMs: Long) {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.toLong())
        seekTo(newPos)
    }

    fun stopPlayback() {
        tickerJob?.cancel()
        tickerJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop playback error: ${e.message}")
        } finally {
            mediaPlayer = null
        }
        _syncState.update { it.copy(state = AudioState.IDLE) }
    }

    fun getCurrentAudioElapsedMs(): Long {
        return when (_syncState.value.state) {
            AudioState.RECORDING -> {
                (System.currentTimeMillis() - recordingStartTime - accumulatedPauseTime).coerceAtLeast(0L)
            }
            AudioState.PLAYING, AudioState.PLAYBACK_PAUSED -> {
                mediaPlayer?.currentPosition?.toLong() ?: _syncState.value.elapsedMs
            }
            else -> 0L
        }
    }

    private fun startRecordingTicker() {
        tickerJob?.cancel()
        tickerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && _syncState.value.state == AudioState.RECORDING) {
                delay(80L)
                val elapsed = (System.currentTimeMillis() - recordingStartTime - accumulatedPauseTime).coerceAtLeast(0L)
                val rawAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                val normalizedAmp = (rawAmp / 32767f).coerceIn(0.05f, 1.0f)

                _syncState.update { state ->
                    val history = (state.waveformHistory + normalizedAmp).takeLast(28)
                    state.copy(
                        elapsedMs = elapsed,
                        amplitudeLevel = normalizedAmp,
                        waveformHistory = history
                    )
                }
            }
        }
    }

    private fun startPlaybackTicker() {
        tickerJob?.cancel()
        tickerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(100L)
                val player = mediaPlayer ?: break
                if (player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    _syncState.update { it.copy(elapsedMs = pos) }
                }
            }
        }
    }

    fun destroy() {
        stopRecording()
        stopPlayback()
    }
}
