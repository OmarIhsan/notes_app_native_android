package com.mal5odha.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import java.util.Locale
import kotlin.math.sin

/**
 * Low-Profile Waveform Scrubber Pill for Academic Audio-Ink Synchronization:
 *
 * - Real-time lecture timeline slider (0..durationMs)
 * - Dynamic waveform visualizer displaying simulated audio amplitude peaks
 * - Play/Pause and Skip +/- 10s controls
 * - Contrast-safe time indicator formatted as "42:15 / 1:14:22"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSyncScrubber(
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPauseToggled: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkip: ((Long) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalDuration = durationMs.coerceAtLeast(1000L)
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(currentPositionMs.toFloat()) }

    // Synchronize slider state when not dragging
    LaunchedEffect(currentPositionMs) {
        if (!isDraggingSlider) {
            dragPositionMs = currentPositionMs.toFloat().coerceIn(0f, totalDuration.toFloat())
        }
    }

    val displayPositionMs = if (isDraggingSlider) dragPositionMs.toLong() else currentPositionMs

    Surface(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 680.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        color = ActionDarkBlue,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, PrimaryCyanBlue.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // ─── Top Row: Play/Pause, Skip, Time Readout, Close ─────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Play/Pause Button
                    FilledIconButton(
                        onClick = onPlayPauseToggled,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PrimaryCyanBlue,
                            contentColor = ActionDarkBlue
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Playback" else "Resume Playback",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Skip -10s
                    IconButton(
                        onClick = { onSkip?.invoke(-10_000L) ?: onSeekTo((displayPositionMs - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 Seconds",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Skip +10s
                    IconButton(
                        onClick = { onSkip?.invoke(10_000L) ?: onSeekTo((displayPositionMs + 10_000L).coerceAtMost(totalDuration)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 Seconds",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Visual Indicator: "42:15 / 1:14:22"
                Text(
                    text = "${formatLectureTime(displayPositionMs)} / ${formatLectureTime(totalDuration)}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Audio Scrubber",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─── Bottom Row: Waveform Visualizer & Interactive Slider ───────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Waveform Peaks
                val progressRatio = (displayPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                val activeWaveColor = PrimaryCyanBlue
                val inactiveWaveColor = Color.White.copy(alpha = 0.22f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val barCount = 54
                    val totalW = size.width
                    val maxH = size.height
                    val barW = (totalW / barCount) * 0.55f
                    val step = totalW / barCount

                    for (i in 0 until barCount) {
                        // Simulated speech amplitude peaks across lecture time
                        val xRatio = i.toFloat() / barCount
                        val baseWave = sin(xRatio * 22f) * 0.35f + sin(xRatio * 7f) * 0.25f + 0.55f
                        val peakHeight = (maxH * baseWave.coerceIn(0.18f, 0.95f))
                        val x = i * step + (step - barW) / 2f
                        val y = (maxH - peakHeight) / 2f

                        val isPast = xRatio <= progressRatio
                        val barColor = if (isPast) activeWaveColor.copy(alpha = 0.7f) else inactiveWaveColor

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barW, peakHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }

                // Timeline Scrubber Slider Overlay
                Slider(
                    value = dragPositionMs,
                    onValueChange = { newValue ->
                        isDraggingSlider = true
                        dragPositionMs = newValue
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeekTo(dragPositionMs.toLong())
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Formats milliseconds into clean lecture time notation ("42:15" or "1:14:22").
 */
fun formatLectureTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
