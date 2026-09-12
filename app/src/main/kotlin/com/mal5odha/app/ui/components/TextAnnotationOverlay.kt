package com.mal5odha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.models.TextAnnotation
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Interactive Rich Text Box with Page-Normalized coordinates,
 * 2D affine manipulation (drag, scale, rotate), and in-place rich formatting.
 */
@Composable
fun TextAnnotationOverlay(
    annotation: TextAnnotation,
    pageWidthPx: Float,
    pageHeightPx: Float,
    isToolActive: Boolean = false,
    isReadOnly: Boolean = false,
    onAnnotationChanged: (TextAnnotation) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }

    if (isReadOnly || !isToolActive) {
        isEditing = false
        isSelected = false
    }

    var normX by remember(annotation.x) { mutableFloatStateOf(annotation.x) }
    var normY by remember(annotation.y) { mutableFloatStateOf(annotation.y) }
    var normW by remember(annotation.width) { mutableFloatStateOf(annotation.width.coerceAtLeast(0.15f)) }
    var normH by remember(annotation.height) { mutableFloatStateOf(annotation.height.coerceAtLeast(0.06f)) }
    var rotation by remember(annotation.rotation) { mutableFloatStateOf(annotation.rotation) }

    var text by remember(annotation.text) { mutableStateOf(annotation.text) }
    var fontSize by remember(annotation.fontSize) { mutableFloatStateOf(annotation.fontSize) }
    var textColor by remember(annotation.color) { mutableIntStateOf(annotation.color) }
    var bgColor by remember(annotation.backgroundColor) { mutableStateOf(annotation.backgroundColor) }
    var alignment by remember(annotation.alignment) { mutableStateOf(annotation.alignment) }

    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    val pixelX = normX * pageWidthPx
    val pixelY = normY * pageHeightPx
    val pixelW = normW * pageWidthPx
    val pixelH = normH * pageHeightPx

    val boxWidthDp = with(density) { pixelW.toDp() }
    val boxHeightDp = with(density) { pixelH.toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset(pixelX.roundToInt(), pixelY.roundToInt()) }
            .rotate(rotation)
            .width(boxWidthDp.coerceAtLeast(100.dp))
            .heightIn(min = boxHeightDp.coerceAtLeast(40.dp))
    ) {
        // ─── Formatting Toolbar (Visible in Editing Mode) ─────────────────────────
        if (isEditing && !isReadOnly) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = (-48).dp),
                shape = RoundedCornerShape(8.dp),
                color = ActionDarkBlue,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Font Size - / +
                    IconButton(
                        onClick = {
                            fontSize = (fontSize - 2f).coerceAtLeast(10f)
                            onAnnotationChanged(annotation.copy(fontSize = fontSize))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("A-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(
                        text = "${fontSize.toInt()}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(
                        onClick = {
                            fontSize = (fontSize + 2f).coerceAtMost(60f)
                            onAnnotationChanged(annotation.copy(fontSize = fontSize))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("A+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Alignment Toggle
                    IconButton(
                        onClick = {
                            alignment = when (alignment) {
                                "START" -> "CENTER"
                                "CENTER" -> "END"
                                else -> "START"
                            }
                            onAnnotationChanged(annotation.copy(alignment = alignment))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = when (alignment) {
                                "CENTER" -> Icons.Default.FormatAlignCenter
                                "END" -> Icons.Default.FormatAlignRight
                                else -> Icons.Default.FormatAlignLeft
                            },
                            contentDescription = "Alignment",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Sticky Note Background Fill Toggle (Sticky yellow / clean white / transparent)
                    val bgColors = listOf(
                        null,
                        android.graphics.Color.parseColor("#FFF9C4"), // Light Yellow Sticky
                        android.graphics.Color.parseColor("#E1F5FE"), // Light Blue Callout
                        android.graphics.Color.WHITE
                    )
                    IconButton(
                        onClick = {
                            val currentIdx = bgColors.indexOf(bgColor)
                            val nextIdx = (currentIdx + 1) % bgColors.size
                            bgColor = bgColors[nextIdx]
                            onAnnotationChanged(annotation.copy(backgroundColor = bgColor))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Card Background",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Done / Close Edit Mode Button
                    IconButton(
                        onClick = {
                            isEditing = false
                            isSelected = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = PrimaryCyanBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ─── Main Text Container ──────────────────────────────────────────────────
        val baseBoxModifier = Modifier
            .fillMaxSize()
            .background(
                color = if (bgColor != null) Color(bgColor!!) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isReadOnly) 0.dp else if (isSelected || isEditing) 1.5.dp else 0.5.dp,
                color = if (isReadOnly) Color.Transparent else if (isSelected || isEditing) PrimaryCyanBlue else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)

        val interactiveModifier = if (!isReadOnly) {
            baseBoxModifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isSelected = true
                    isEditing = true
                }
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
            baseBoxModifier
        }

        Box(
            modifier = interactiveModifier
        ) {
            val alignValue = when (alignment) {
                "CENTER" -> TextAlign.Center
                "END" -> TextAlign.End
                else -> TextAlign.Start
            }

            BasicTextField(
                value = text,
                readOnly = isReadOnly,
                onValueChange = { newText ->
                    if (!isReadOnly) {
                        text = newText
                        onAnnotationChanged(annotation.copy(text = newText))
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = fontSize.sp,
                    color = Color(textColor),
                    textAlign = alignValue,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(PrimaryCyanBlue),
                decorationBox = { innerTextField ->
                    if (text.isEmpty() && !isEditing) {
                        Text(
                            text = if (isReadOnly) "" else "Tap to enter text...",
                            fontSize = fontSize.sp,
                            color = Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )
        }

        // ─── Selection Handles (Transform Mode) ───────────────────────────────────
        if (isSelected && !isEditing && !isReadOnly) {
            // Delete button (Top-Right)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(22.dp)
                    .clickable { onDelete() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                shadowElevation = 3.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(3.dp)
                )
            }

            // Top Rotation Pin Handle
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp)
                    .size(20.dp)
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
                            val currentTouchY = pixelY - 20f + dragAmount.y
                            val angleRad = atan2(currentTouchY - centerY, currentTouchX - centerX)
                            rotation = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                        }
                    },
                shape = CircleShape,
                color = ActionDarkBlue,
                shadowElevation = 3.dp
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

            // Bottom-Right Corner Resize Handle
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(20.dp)
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
                            normW = (normW + dragAmount.x / pageWidthPx).coerceIn(0.1f, 1f - normX)
                            normH = (normH + dragAmount.y / pageHeightPx).coerceIn(0.04f, 1f - normY)
                        }
                    },
                shape = CircleShape,
                color = PrimaryCyanBlue,
                shadowElevation = 3.dp
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Resize",
                    tint = Color.White,
                    modifier = Modifier.padding(3.dp)
                )
            }
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }
}
