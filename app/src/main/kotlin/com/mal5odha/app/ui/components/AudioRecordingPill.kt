package com.mal5odha.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.audio.AudioState
import com.mal5odha.core.audio.AudioSyncState
import java.util.Locale

/**
 * Top Bar Audio Control Pill providing real-time recording status,
 * live waveform amplitude visualizer, and compact audio playback scrub controls.
 */
@Composable
fun AudioRecordingPill(
    syncState: AudioSyncState,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayPausePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onStopPlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = ActionDarkBlue,
        shadowElevation = 6.dp,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            PrimaryCyanBlue.copy(alpha = 0.25f)
        )
    ) {
        when (syncState.state) {
            AudioState.IDLE -> {
                // Compact Mic Button to Initiate Recording (48dp touch target)
                IconButton(
                    onClick = onStartRecording,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Audio Memo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            AudioState.RECORDING, AudioState.RECORDING_PAUSED -> {
                // Live Recording Bar with Pulsing Dot & Waveform
                val isPaused = syncState.state == AudioState.RECORDING_PAUSED
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing Red Recording Indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPaused) Color.LightGray else Color.Red.copy(alpha = pulseAlpha)
                            )
                    )

                    // Formatted Elapsed Time
                    Text(
                        text = formatTime(syncState.elapsedMs),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Mini Live Waveform Bars
                    Row(
                        modifier = Modifier
                            .height(20.dp)
                            .width(64.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val history = syncState.waveformHistory.takeLast(12)
                        history.forEach { amp ->
                            val barHeight = (18f * amp).coerceIn(3f, 18f).dp
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(PrimaryCyanBlue)
                            )
                        }
                    }

                    // Pause / Resume Button (48dp touch target)
                    IconButton(
                        onClick = {
                            if (isPaused) onResumeRecording() else onPauseRecording()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Stop Recording Button (48dp touch target)
                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Recording",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            AudioState.PLAYING, AudioState.PLAYBACK_PAUSED -> {
                // Audio Playback Scrub Bar (Note Replay)
                val isPlaying = syncState.state == AudioState.PLAYING

                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Skip -15s (48dp touch target)
                    IconButton(
                        onClick = { onSkip(-15000L) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Skip Back 15s",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause Toggle (48dp touch target)
                    IconButton(
                        onClick = onPlayPausePlayback,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Playback" else "Resume Playback",
                            tint = PrimaryCyanBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Skip +15s (48dp touch target)
                    IconButton(
                        onClick = { onSkip(15000L) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Skip Forward 15s",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mini Progress Slider
                    val progress = if (syncState.durationMs > 0L) {
                        (syncState.elapsedMs.toFloat() / syncState.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progress,
                        onValueChange = { frac ->
                            val targetMs = (frac * syncState.durationMs).toLong()
                            onSeekTo(targetMs)
                        },
                        modifier = Modifier
                            .width(110.dp)
                            .height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryCyanBlue,
                            activeTrackColor = PrimaryCyanBlue,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    // Position / Duration text
                    Text(
                        text = "${formatTime(syncState.elapsedMs)} / ${formatTime(syncState.durationMs)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Close Playback (48dp touch target)
                    IconButton(
                        onClick = onStopPlayback,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Audio Playback",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
