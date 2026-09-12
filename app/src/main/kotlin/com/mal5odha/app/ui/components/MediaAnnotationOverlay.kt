package com.mal5odha.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.models.MediaAnnotation
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Interactive Image / Sticker Annotation Overlay anchored in page-normalized coordinates ([0.0, 1.0]).
 * Supports translation within page, aspect-ratio locked resizing, 2D rotation, and atomic deletion.
 */
@Composable
fun MediaAnnotationOverlay(
    annotation: MediaAnnotation,
    pageWidthPx: Float,
    pageHeightPx: Float,
    isReadOnly: Boolean = false,
    onAnnotationChanged: (MediaAnnotation) -> Unit,
    onDelete: () -> Unit
) {
    var isSelected by remember { mutableStateOf(false) }

    if (isReadOnly) {
        isSelected = false
    }

    var normX by remember(annotation.x) { mutableFloatStateOf(annotation.x) }
    var normY by remember(annotation.y) { mutableFloatStateOf(annotation.y) }
    var normW by remember(annotation.width) { mutableFloatStateOf(annotation.width.coerceAtLeast(0.1f)) }
    var normH by remember(annotation.height) { mutableFloatStateOf(annotation.height.coerceAtLeast(0.08f)) }
    var rotation by remember(annotation.rotation) { mutableFloatStateOf(annotation.rotation) }

    val density = LocalDensity.current

    val pixelX = normX * pageWidthPx
    val pixelY = normY * pageHeightPx
    val pixelW = normW * pageWidthPx
    val pixelH = normH * pageHeightPx

    val boxWidthDp = with(density) { pixelW.toDp() }
    val boxHeightDp = with(density) { pixelH.toDp() }

    val imageBitmap = remember(annotation.localPath) {
        try {
            BitmapFactory.decodeFile(annotation.localPath)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(pixelX.roundToInt(), pixelY.roundToInt()) }
            .rotate(rotation)
            .size(boxWidthDp, boxHeightDp)
    ) {
        // Main Image Card
        val baseImageModifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = if (isReadOnly) 0.dp else if (isSelected) 2.dp else 0.5.dp,
                color = if (isReadOnly) Color.Transparent else if (isSelected) PrimaryCyanBlue else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )

        val imageModifier = if (!isReadOnly) {
            baseImageModifier
                .clickable { isSelected = !isSelected }
                .pointerInput(annotation.id) {
                    detectDragGestures(
                        onDragEnd = {
                            onAnnotationChanged(
                                annotation.copy(
                                    x = normX,
                                    y = normY,
                                    width = normW,
                                    height = normH,
                                    rotation = rotation
                                )
                            )
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        isSelected = true
                        normX = (normX + dragAmount.x / pageWidthPx).coerceIn(0f, 1f - normW)
                        normY = (normY + dragAmount.y / pageHeightPx).coerceIn(0f, 1f - normH)
                    }
                }
        } else {
            baseImageModifier
        }

        Box(
            modifier = imageModifier
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Image annotation sticker",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Image",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
            }
        }

        // Handles (Visible when selected)
        if (isSelected && !isReadOnly) {
            // Delete button (Top-Right)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(24.dp)
                    .clickable { onDelete() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                shadowElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete image sticker",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }

            // Rotation Pin Handle (Top-Center)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-22).dp)
                    .size(22.dp)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragEnd = {
                                onAnnotationChanged(annotation.copy(rotation = rotation))
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val centerX = pixelX + pixelW / 2f
                            val centerY = pixelY + pixelH / 2f
                            val currentTouchX = pixelX + pixelW / 2f + dragAmount.x
                            val currentTouchY = pixelY - 22f + dragAmount.y
                            val angleRad = atan2(currentTouchY - centerY, currentTouchX - centerX)
                            rotation = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                        }
                    },
                shape = CircleShape,
                color = ActionDarkBlue,
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PrimaryCyanBlue)
                    )
                }
            }

            // Resize Handle (Bottom-Right) with Aspect-Ratio Maintenance
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(22.dp)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragEnd = {
                                onAnnotationChanged(
                                    annotation.copy(
                                        width = normW,
                                        height = normH
                                    )
                                )
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val aspectRatio = (normW / normH.coerceAtLeast(0.01f)).coerceIn(0.2f, 5f)
                            val deltaNormW = dragAmount.x / pageWidthPx
                            normW = (normW + deltaNormW).coerceIn(0.1f, 1f - normX)
                            normH = (normW / aspectRatio).coerceIn(0.06f, 1f - normY)
                        }
                    },
                shape = CircleShape,
                color = PrimaryCyanBlue,
                shadowElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Resize sticker",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}
