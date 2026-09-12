package com.mal5odha.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mal5odha.core.ink.models.MediaAnnotation
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun MediaBoxWidget(
    annotation: MediaAnnotation,
    onAnnotationChanged: (MediaAnnotation) -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(annotation.x) }
    var offsetY by remember { mutableStateOf(annotation.y) }
    var width by remember { mutableStateOf(annotation.width) }
    var height by remember { mutableStateOf(annotation.height) }

    val imageBitmap = remember(annotation.localPath) {
        try {
            BitmapFactory.decodeFile(annotation.localPath)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width.dp, height.dp)
            .border(1.5.dp, Color(0xFF6200EE).copy(alpha = 0.8f))
            .background(Color.Black.copy(alpha = 0.05f))
            .pointerInput(annotation.id) {
                detectDragGestures(
                    onDragEnd = {
                        onAnnotationChanged(
                            annotation.copy(
                                x = offsetX,
                                y = offsetY,
                                width = width,
                                height = height
                            )
                        )
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Floating image annotation",
                modifier = Modifier.size(width.dp, height.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(width.dp, height.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text("Image Error", color = Color.Red)
            }
        }

        // Delete button (Top-Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Red)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Image",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Resize Handle (Bottom-Right Corner)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF6200EE))
                .border(1.dp, Color.White, CircleShape)
                .pointerInput(annotation.id) {
                    detectDragGestures(
                        onDragEnd = {
                            onAnnotationChanged(
                                annotation.copy(
                                    x = offsetX,
                                    y = offsetY,
                                    width = width,
                                    height = height
                                )
                            )
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val density = this.density
                        val deltaW = dragAmount.x / density
                        val deltaH = dragAmount.y / density
                        width = max(100f, width + deltaW)
                        height = max(100f, height + deltaH)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Resize Image",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
