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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.math.TextAnnotationTransformSolver
import com.mal5odha.core.ink.models.TextAnnotation
import kotlin.math.roundToInt

/**
 * Goodnotes/Notewise-style Interactive Floating Text Box.
 *
 * Supports:
 * - Bounded word wrapping (softWrap = true) governed strictly by width.
 * - Dynamic vertical height auto-expansion downwards (top edge stationary Y_top = const).
 * - Stationary opposite-anchor horizontal resizing via left/right pill handles.
 * - Page translation via top drag handle.
 * - Detached floating formatting bar with 5 color swatches, font stepper, B/I/U toggles, alignment, and haptic delete.
 * - Optimized Idle / Pinned mode with lightweight Text rendering for 144Hz scroll fluidity.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextAnnotationOverlay(
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
    val focusRequester = remember { FocusRequester() }

    var isEditing by remember(isSelected) { mutableStateOf(isSelected) }

    // Synchronize editing state with external selection or tool change
    if (isReadOnly || !isToolActive) {
        if (isEditing) {
            isEditing = false
            onDeselect()
        }
    }

    var normX by remember(annotation.xNorm) { mutableFloatStateOf(annotation.xNorm) }
    var normY by remember(annotation.yNorm) { mutableFloatStateOf(annotation.yNorm) }
    var normW by remember(annotation.widthNorm) { mutableFloatStateOf(annotation.widthNorm.coerceAtLeast(0.08f)) }
    var normH by remember(annotation.heightNorm) { mutableFloatStateOf(annotation.heightNorm) }
    var toolbarWidthPx by remember { mutableFloatStateOf(0f) }
    var toolbarHeightPx by remember { mutableFloatStateOf(0f) }

    var content by remember(annotation.content) { mutableStateOf(annotation.content) }
    var fontSizeSp by remember(annotation.fontSizeSp) { mutableFloatStateOf(annotation.fontSizeSp) }
    var colorHex by remember(annotation.colorHex) { mutableLongStateOf(annotation.colorHex) }
    var isBold by remember(annotation.isBold) { mutableStateOf(annotation.isBold) }
    var isItalic by remember(annotation.isItalic) { mutableStateOf(annotation.isItalic) }
    var isUnderline by remember(annotation.isUnderline) { mutableStateOf(annotation.isUnderline) }
    var textAlign by remember(annotation.textAlign) { mutableStateOf(annotation.textAlign) }

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
        newContent: String = content,
        newFontSize: Float = fontSizeSp,
        newColor: Long = colorHex,
        newBold: Boolean = isBold,
        newItalic: Boolean = isItalic,
        newUnderline: Boolean = isUnderline,
        newAlign: TextAlign = textAlign
    ) {
        onAnnotationChanged(
            annotation.copy(
                xNorm = newX,
                yNorm = newY,
                widthNorm = newW,
                heightNorm = newH,
                content = newContent,
                fontSizeSp = newFontSize,
                colorHex = newColor,
                isBold = newBold,
                isItalic = newItalic,
                isUnderline = newUnderline,
                textAlign = newAlign
            )
        )
    }

    val textStyle = TextStyle(
        fontSize = fontSizeSp.sp,
        color = Color(colorHex),
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
        textAlign = textAlign,
        lineHeight = (fontSizeSp * 1.25f).sp
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(pixelX.roundToInt(), pixelY.roundToInt()) }
            .width(boxWidthDp.coerceAtLeast(80.dp))
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
        // ─── Detached Floating Formatting Palette (Above / Below via unconstrained Popup) ──
        if (isEditing && !isReadOnly) {
            val effectiveToolbarHeight = if (toolbarHeightPx > 0f) toolbarHeightPx else with(density) { 76.dp.toPx() }
            val isNearTop = pixelY < effectiveToolbarHeight + with(density) { 26.dp.toPx() }
            val computedYOffsetPx = if (isNearTop) {
                (normH * pageHeightPx + with(density) { 12.dp.toPx() }).roundToInt()
            } else {
                (-effectiveToolbarHeight - with(density) { 22.dp.toPx() }).roundToInt()
            }

            // Horizontal Centering: align midpoint of toolbar with midpoint of text box
            val marginPaddingPx = with(density) { 12.dp.toPx() }
            val effectiveToolbarWidth = if (toolbarWidthPx > 0f) toolbarWidthPx else with(density) { 220.dp.toPx() }
            val computedXOffsetPx = TextAnnotationTransformSolver.calculateCenteredToolbarOffset(
                boxLeftPx = pixelX,
                boxWidthPx = pixelW,
                toolbarWidthPx = effectiveToolbarWidth,
                pageWidthPx = pageWidthPx,
                marginPaddingPx = marginPaddingPx
            ).roundToInt()

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(x = computedXOffsetPx, y = computedYOffsetPx),
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                FloatingTextFormattingBar(
                    annotation = annotation.copy(
                        xNorm = normX,
                        yNorm = normY,
                        widthNorm = normW,
                        heightNorm = normH,
                        content = content,
                        fontSizeSp = fontSizeSp,
                        colorHex = colorHex,
                        isBold = isBold,
                        isItalic = isItalic,
                        isUnderline = isUnderline,
                        textAlign = textAlign
                    ),
                    onUpdate = { updated ->
                        fontSizeSp = updated.fontSizeSp
                        colorHex = updated.colorHex
                        isBold = updated.isBold
                        isItalic = updated.isItalic
                        isUnderline = updated.isUnderline
                        textAlign = updated.textAlign
                        commitChanges(
                            newFontSize = updated.fontSizeSp,
                            newColor = updated.colorHex,
                            newBold = updated.isBold,
                            newItalic = updated.isItalic,
                            newUnderline = updated.isUnderline,
                            newAlign = updated.textAlign
                        )
                    },
                    onDelete = onDelete,
                    onDone = {
                        isEditing = false
                        keyboardController?.hide()
                        onDeselect()
                        commitChanges()
                    },
                    modifier = Modifier.onSizeChanged { size ->
                        toolbarWidthPx = size.width.toFloat()
                        toolbarHeightPx = size.height.toFloat()
                    }
                )
            }
        }

        // ─── Main Text Container ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top)
                .onSizeChanged { size ->
                    val measuredH = size.height.toFloat()
                    val computedNormH = TextAnnotationTransformSolver.computeNormalizedHeight(measuredH, pageHeightPx)
                    if (kotlin.math.abs(computedNormH - normH) > 0.002f) {
                        normH = computedNormH
                        commitChanges(newH = computedNormH)
                    }
                }
                .background(
                    color = if (isEditing) Color.White.copy(alpha = 0.95f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = if (isEditing) 1.5.dp else 0.dp,
                    color = if (isEditing) PrimaryCyanBlue else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isReadOnly && isToolActive && !isEditing) {
                        isEditing = true
                        onSelect()
                    }
                }
        ) {
            if (isEditing && !isReadOnly) {
                // ─── Editing Mode: BasicTextField with active IME input ──────
                BasicTextField(
                    value = content,
                    onValueChange = { newText ->
                        content = newText
                        commitChanges(newContent = newText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(PrimaryCyanBlue),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = "Type here...",
                                style = textStyle.copy(color = Color.Gray.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                )
            } else {
                // ─── Idle / Pinned Mode: Ultra-lightweight Text composable ────
                Text(
                    text = content.ifEmpty { if (isToolActive) "Tap to type..." else "" },
                    style = if (content.isEmpty()) textStyle.copy(color = Color.Gray.copy(alpha = 0.4f)) else textStyle,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ─── Stationary Opposite-Anchor Transform Handles (Editing Mode) ─────
        if (isEditing && !isReadOnly) {
            // Top Drag Handle (Move Card with enlarged touch target and 48dp x 18dp pill)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(width = 56.dp, height = 36.dp)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragEnd = { commitChanges() }
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
                            onDragEnd = { commitChanges() }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val (newX, newW) = TextAnnotationTransformSolver.resizeHorizontal(
                                currentX = normX,
                                currentWidth = normW,
                                deltaXNorm = deltaXNorm,
                                isLeftHandle = true
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
                            onDragEnd = { commitChanges() }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val (newX, newW) = TextAnnotationTransformSolver.resizeHorizontal(
                                currentX = normX,
                                currentWidth = normW,
                                deltaXNorm = deltaXNorm,
                                isLeftHandle = false
                            )
                            normX = newX
                            normW = newW
                        }
                    }
            )
        }
    }

    // Auto-focus and deploy IME when entering editing mode
    LaunchedEffect(isEditing) {
        if (isEditing && !isReadOnly) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}
