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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.math.TextAnnotationTransformSolver
import com.mal5odha.core.ink.models.TextAnnotation
import kotlin.math.roundToInt

/**
 * Curated pastel color palette for Sticky Card stickers.
 */
object StickyCardPalette {
    const val YELLOW = 0xFFFFF9C4L // Default: Pastel Post-it Yellow
    const val GREEN = 0xFFC8E6C9L  // Mint Green
    const val BLUE = 0xFFBBDEFBL   // Sky Blue
    const val PEACH = 0xFFFFE0B2L  // Warm Peach
    const val PURPLE = 0xFFE1BEE7L // Soft Lavender
    const val ROSE = 0xFFF8BBD0L   // Pastel Rose

    val ALL = listOf(YELLOW, GREEN, BLUE, PEACH, PURPLE, ROSE)
}

/**
 * Pre-styled, lightweight "Sticky Card" Sticker Composable.
 *
 * Features:
 * - Distinct Header (Title) and Body text area.
 * - Rounded container with pastel background and subtle drop shadow.
 * - Integrated stationary opposite-anchor horizontal resizing & page translation handles.
 * - Dynamic vertical height auto-expansion downwards.
 * - Pin/Unpin support to lock placement against accidental drags.
 * - 6-Color pastel swatch switcher.
 * - Ultra-lightweight Idle mode rendering for 144Hz scroll fluidity.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StickyNoteWidget(
    annotation: TextAnnotation,
    pageWidthPx: Float,
    pageHeightPx: Float,
    isToolActive: Boolean = false,
    isReadOnly: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onDeselect: () -> Unit = {},
    onPositionChanged: (Rect) -> Unit = {},
    onAnnotationChanged: (TextAnnotation) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }

    var isEditing by remember(isSelected) { mutableStateOf(isSelected) }

    // Synchronize editing state with external selection or tool change
    if (isReadOnly || !isToolActive) {
        if (isEditing) {
            isEditing = false
            onDeselect()
        }
    }

    var isDraggingHandle by remember { mutableStateOf(false) }

    var normX by remember(annotation.id) { mutableFloatStateOf(annotation.xNorm) }
    var normY by remember(annotation.id) { mutableFloatStateOf(annotation.yNorm) }
    var normW by remember(annotation.id) { mutableFloatStateOf(annotation.widthNorm.coerceAtLeast(0.18f)) }
    var normH by remember(annotation.id) { mutableFloatStateOf(annotation.heightNorm) }

    var cardColorHex by remember(annotation.id) {
        mutableLongStateOf(
            annotation.backgroundColor?.toLong()?.and(0xFFFFFFFFL) ?: StickyCardPalette.YELLOW
        )
    }
    var isPinned by remember(annotation.id) { mutableStateOf(annotation.isPinned) }
    var textAlign by remember(annotation.id) { mutableStateOf(annotation.textAlign) }
    var showColorPalette by remember { mutableStateOf(false) }

    // Decoupled Title state keyed strictly by annotation.id
    var titleValue by remember(annotation.id) {
        mutableStateOf(
            TextFieldValue(
                text = annotation.title,
                selection = TextRange(annotation.title.length)
            )
        )
    }

    // Decoupled Body state keyed strictly by annotation.id
    var bodyValue by remember(annotation.id) {
        mutableStateOf(
            TextFieldValue(
                text = annotation.content,
                selection = TextRange(annotation.content.length)
            )
        )
    }

    // Synchronize external updates
    LaunchedEffect(annotation.title) {
        if (annotation.title != titleValue.text) {
            titleValue = titleValue.copy(
                text = annotation.title,
                selection = TextRange(annotation.title.length)
            )
        }
    }

    LaunchedEffect(annotation.content) {
        if (annotation.content != bodyValue.text) {
            bodyValue = bodyValue.copy(
                text = annotation.content,
                selection = TextRange(annotation.content.length)
            )
        }
    }

    LaunchedEffect(annotation.backgroundColor, annotation.isPinned, annotation.textAlign) {
        cardColorHex = annotation.backgroundColor?.toLong()?.and(0xFFFFFFFFL) ?: StickyCardPalette.YELLOW
        isPinned = annotation.isPinned
        textAlign = annotation.textAlign
    }

    // Synchronize layout coordinates when not dragging
    LaunchedEffect(annotation.xNorm, annotation.yNorm, annotation.widthNorm, annotation.heightNorm) {
        if (!isDraggingHandle) {
            normX = annotation.xNorm
            normY = annotation.yNorm
            normW = annotation.widthNorm.coerceAtLeast(0.18f)
            normH = annotation.heightNorm
        }
    }

    val pixelX = normX * pageWidthPx
    val pixelY = normY * pageHeightPx
    val pixelW = normW * pageWidthPx
    val boxWidthDp = with(density) { pixelW.toDp() }

    // Commit helper
    fun commitChanges(
        newX: Float = normX,
        newY: Float = normY,
        newW: Float = normW,
        newH: Float = normH,
        newTitle: String = titleValue.text,
        newBody: String = bodyValue.text,
        newColorHex: Long = cardColorHex,
        newPinned: Boolean = isPinned,
        newTextAlign: TextAlign = textAlign
    ) {
        onAnnotationChanged(
            annotation.copy(
                xNorm = newX,
                yNorm = newY,
                widthNorm = newW,
                heightNorm = newH,
                title = newTitle,
                content = newBody,
                backgroundColor = newColorHex.toInt(),
                isCard = true,
                isPinned = newPinned,
                textAlign = newTextAlign
            )
        )
    }

    val titleStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1B1F),
        lineHeight = 20.sp,
        textAlign = textAlign
    )

    val bodyStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF2C2C2C),
        lineHeight = 19.sp,
        textAlign = textAlign
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(pixelX.roundToInt(), pixelY.roundToInt()) }
            .width(boxWidthDp.coerceAtLeast(160.dp))
            .wrapContentHeight(align = Alignment.Top)
            .onGloballyPositioned { coordinates ->
                if (isEditing) {
                    val pos = coordinates.positionInRoot()
                    val sz = coordinates.size
                    onPositionChanged(
                        Rect(
                            pos,
                            Size(sz.width.toFloat(), sz.height.toFloat())
                        )
                    )
                }
            }
    ) {
        // ─── Main Card Surface ───────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top)
                .shadow(
                    elevation = if (isEditing) 6.dp else 2.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .border(
                    width = if (isEditing) 1.5.dp else 1.dp,
                    color = if (isEditing) PrimaryCyanBlue else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp)
                )
                .onSizeChanged { size ->
                    val measuredH = size.height.toFloat()
                    val computedNormH = TextAnnotationTransformSolver.computeNormalizedHeight(measuredH, pageHeightPx)
                    if (kotlin.math.abs(computedNormH - normH) > 0.002f) {
                        normH = computedNormH
                        if (!isDraggingHandle) {
                            commitChanges(newH = computedNormH)
                        }
                    }
                }
                .then(
                    if (!isReadOnly && isToolActive) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isEditing) {
                                isEditing = true
                                onSelect()
                            }
                        }
                    } else Modifier
                ),
            shape = RoundedCornerShape(14.dp),
            color = Color(cardColorHex)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // ─── Header Bar: Title + Action Controls ─────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title Input / Display
                    Box(modifier = Modifier.weight(1f)) {
                        if (isEditing && !isReadOnly && !isPinned) {
                            BasicTextField(
                                value = titleValue,
                                onValueChange = { newValue ->
                                    titleValue = newValue
                                    commitChanges(newTitle = newValue.text)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(titleFocusRequester),
                                textStyle = titleStyle,
                                singleLine = true,
                                cursorBrush = SolidColor(PrimaryCyanBlue),
                                decorationBox = { innerTextField ->
                                    if (titleValue.text.isEmpty()) {
                                        Text(
                                            text = "Title...",
                                            style = titleStyle.copy(color = Color.Black.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        } else {
                            Text(
                                text = titleValue.text.ifEmpty { if (isToolActive) "Title" else "" },
                                style = if (titleValue.text.isEmpty()) titleStyle.copy(color = Color.Black.copy(alpha = 0.35f)) else titleStyle,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Action Controls in Header (Editing Mode)
                    if (isEditing && !isReadOnly) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pin Toggle
                            IconButton(
                                onClick = {
                                    val newPinned = !isPinned
                                    isPinned = newPinned
                                    commitChanges(newPinned = newPinned)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (isPinned) "Unpin Sticky Card" else "Pin Sticky Card",
                                    tint = if (isPinned) PrimaryCyanBlue else Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Color Palette Trigger
                            Box {
                                IconButton(
                                    onClick = { showColorPalette = !showColorPalette },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Change Pastel Color",
                                        tint = Color.Black.copy(alpha = 0.55f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showColorPalette,
                                    onDismissRequest = { showColorPalette = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                StickyCardPalette.ALL.forEach { hex ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(hex))
                                                            .border(
                                                                width = if (cardColorHex == hex) 2.dp else 1.dp,
                                                                color = if (cardColorHex == hex) PrimaryCyanBlue else Color.Black.copy(alpha = 0.15f),
                                                                shape = CircleShape
                                                            )
                                                            .clickable {
                                                                cardColorHex = hex
                                                                commitChanges(newColorHex = hex)
                                                                showColorPalette = false
                                                            }
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {}
                                    )
                                }
                            }

                            // Text Alignment Cycle Button (Start -> Center -> End)
                            IconButton(
                                onClick = {
                                    val nextAlign = when (textAlign) {
                                        TextAlign.Start, TextAlign.Left -> TextAlign.Center
                                        TextAlign.Center -> TextAlign.End
                                        else -> TextAlign.Start
                                    }
                                    textAlign = nextAlign
                                    commitChanges(newTextAlign = nextAlign)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = when (textAlign) {
                                        TextAlign.Center -> Icons.Default.FormatAlignCenter
                                        TextAlign.End, TextAlign.Right -> Icons.Default.FormatAlignRight
                                        else -> Icons.Default.FormatAlignLeft
                                    },
                                    contentDescription = "Cycle Text Alignment",
                                    tint = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete Button
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Sticky Card",
                                    tint = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Done Button
                            IconButton(
                                onClick = {
                                    isEditing = false
                                    keyboardController?.hide()
                                    onDeselect()
                                    commitChanges()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done Editing",
                                    tint = PrimaryCyanBlue,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    } else if (isPinned) {
                        // Pinned subtle badge when not editing
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ─── Subtle Hairline Divider ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(Color.Black.copy(alpha = 0.08f))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ─── Body Text Area ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    if (isEditing && !isReadOnly && !isPinned) {
                        BasicTextField(
                            value = bodyValue,
                            onValueChange = { newValue ->
                                bodyValue = newValue
                                commitChanges(newBody = newValue.text)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = bodyStyle,
                            cursorBrush = SolidColor(PrimaryCyanBlue),
                            decorationBox = { innerTextField ->
                                if (bodyValue.text.isEmpty()) {
                                    Text(
                                        text = "Take a note...",
                                        style = bodyStyle.copy(color = Color.Black.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = bodyValue.text.ifEmpty { if (isToolActive) "Tap to write..." else "" },
                            style = if (bodyValue.text.isEmpty()) bodyStyle.copy(color = Color.Black.copy(alpha = 0.35f)) else bodyStyle,
                            softWrap = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // ─── Transform Handles (Editing Mode, when not pinned) ───────────────
        if (isEditing && !isReadOnly && !isPinned) {
            // Top Drag Handle (Move Card)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(width = 56.dp, height = 36.dp)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragStart = { isDraggingHandle = true },
                            onDragEnd = {
                                isDraggingHandle = false
                                commitChanges()
                            },
                            onDragCancel = { isDraggingHandle = false }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val deltaYNorm = dragAmount.y / pageHeightPx
                            val (newX, newY) = TextAnnotationTransformSolver.translate(
                                currentX = normX,
                                currentY = normY,
                                width = normW,
                                height = normH,
                                deltaXNorm = deltaXNorm,
                                deltaYNorm = deltaYNorm
                            )
                            normX = newX
                            normY = newY
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Visual Pill Handle (48dp x 18dp)
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(PrimaryCyanBlue),
                    contentAlignment = Alignment.Center
                ) {
                    // Grip indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // Left Pill Handle (Opposite Right Anchor Remains Stationary)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-5).dp)
                    .width(8.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryCyanBlue)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragStart = { isDraggingHandle = true },
                            onDragEnd = {
                                isDraggingHandle = false
                                commitChanges()
                            },
                            onDragCancel = { isDraggingHandle = false }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val (newX, newW) = TextAnnotationTransformSolver.resizeHorizontal(
                                currentX = normX,
                                currentWidth = normW,
                                deltaXNorm = deltaXNorm,
                                isLeftHandle = true,
                                minWidthNorm = 0.18f
                            )
                            normX = newX
                            normW = newW
                        }
                    }
            )

            // Right Pill Handle (Opposite Left Anchor Remains Stationary)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 5.dp)
                    .width(8.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryCyanBlue)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragStart = { isDraggingHandle = true },
                            onDragEnd = {
                                isDraggingHandle = false
                                commitChanges()
                            },
                            onDragCancel = { isDraggingHandle = false }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val (newX, newW) = TextAnnotationTransformSolver.resizeHorizontal(
                                currentX = normX,
                                currentWidth = normW,
                                deltaXNorm = deltaXNorm,
                                isLeftHandle = false,
                                minWidthNorm = 0.18f
                            )
                            normX = newX
                            normW = newW
                        }
                    }
            )
        }
    }

    // Auto-focus title and deploy IME when entering editing mode (if not pinned)
    LaunchedEffect(isEditing) {
        if (isEditing && !isReadOnly && !isPinned) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}
