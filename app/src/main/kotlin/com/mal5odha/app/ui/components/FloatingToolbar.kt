package com.mal5odha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.NotePaletteDefaults
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.models.InkTool

/**
 * FloatingToolbar: Multi-tool floating bar with a dynamic 3-slot color well
 * for zero-overhead, 1-tap fast color switching, plus an expandable 12-color
 * palette popover anchored to the active color slot.
 */
@Composable
fun FloatingToolbar(
    currentTool: InkTool,
    onToolSelected: (InkTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    selectedWidth: Float,
    onWidthSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isReadOnlyMode: Boolean = false,
    canUndo: Boolean = true,
    canRedo: Boolean = true
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val recentColors by NotePaletteDefaults.recentColors.collectAsState()
    val palette = NotePaletteDefaults.InkingPalette
    val strokeWidths = listOf(2f, 5f, 10f, 20f, 40f)
    val haptic = LocalHapticFeedback.current

    // Dynamic 3-slot palette derivation
    val defaultFallback = remember {
        listOf(
            NotePaletteDefaults.InkingPalette[0], // Pitch Black (#1E1E1E)
            NotePaletteDefaults.InkingPalette[9], // Royal Blue (#1976D2)
            NotePaletteDefaults.InkingPalette[2]  // Crimson Red (#D32F2F)
        )
    }
    val isBlackMatch = selectedColor == android.graphics.Color.BLACK || selectedColor == NotePaletteDefaults.InkingPalette[0].toArgb()
    val canonicalSelected = if (isBlackMatch) NotePaletteDefaults.InkingPalette[0] else Color(selectedColor)
    val visibleSlots = remember(recentColors, selectedColor) {
        (listOf(canonicalSelected) + recentColors + defaultFallback).distinct().take(3)
    }
    val activeSlotIndex = visibleSlots.indexOfFirst { slotColor ->
        slotColor.toArgb() == selectedColor || (isBlackMatch && slotColor == NotePaletteDefaults.InkingPalette[0])
    }.let { if (it >= 0) it else 0 }

    Surface(
        modifier = modifier.wrapContentSize(),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: Tools & Undo/Redo
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isReadOnlyMode) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                }

                IconButton(onClick = { onToolSelected(InkTool.PEN) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Pen Tool",
                        tint = if (currentTool == InkTool.PEN) PrimaryCyanBlue else ActionDarkBlue
                    )
                }
                IconButton(onClick = { onToolSelected(InkTool.HIGHLIGHTER) }) {
                    Icon(
                        Icons.Default.Brush,
                        contentDescription = "Highlighter",
                        tint = if (currentTool == InkTool.HIGHLIGHTER) PrimaryCyanBlue else ActionDarkBlue
                    )
                }
                IconButton(onClick = { onToolSelected(InkTool.ERASER) }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Eraser",
                        tint = if (currentTool == InkTool.ERASER) PrimaryCyanBlue else ActionDarkBlue
                    )
                }
                IconButton(onClick = { onToolSelected(InkTool.SHAPE) }) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = "Shape Tool",
                        tint = if (currentTool == InkTool.SHAPE) PrimaryCyanBlue else ActionDarkBlue
                    )
                }
                IconButton(onClick = { onToolSelected(InkTool.LASSO) }) {
                    Icon(
                        Icons.Default.Crop,
                        contentDescription = "Lasso Tool",
                        tint = if (currentTool == InkTool.LASSO) PrimaryCyanBlue else ActionDarkBlue
                    )
                }
                IconButton(onClick = { onToolSelected(InkTool.TEXT) }) {
                    Icon(
                        Icons.Default.Title,
                        contentDescription = "Text Tool",
                        tint = if (currentTool == InkTool.TEXT) PrimaryCyanBlue else ActionDarkBlue
                    )
                }
            }

            // Row 2: Dynamic 3-Slot Color Well & Stroke Widths (Edit Mode Only)
            if (!isReadOnlyMode) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dynamic 3-Slot Color Well
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleSlots.forEachIndexed { index, slotColor ->
                            val isSelected = index == activeSlotIndex
                            Box(contentAlignment = Alignment.Center) {
                                // 38dp touch bounding box with centered 24dp visual color disk
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (isSelected) {
                                                // Tapping active slot toggles full palette popover
                                                showColorPicker = !showColorPicker
                                            } else {
                                                // Tapping inactive slot fast-switches color without opening popup
                                                showColorPicker = false
                                                NotePaletteDefaults.recordUsedColor(slotColor)
                                                onColorSelected(slotColor.toArgb())
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Outer selection accent ring (PrimaryCyanBlue, 2.dp) for active slot
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .border(2.dp, PrimaryCyanBlue, CircleShape)
                                        )
                                    }

                                    // Centered 24dp visual color disk with adaptive checkmark for active slot
                                    val checkmarkColor = if (slotColor.luminance() > 0.5f) Color.Black else Color.White
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(slotColor)
                                            .border(
                                                width = 1.dp,
                                                color = if (slotColor == Color.White || slotColor.luminance() > 0.85f)
                                                    Color.Gray.copy(alpha = 0.5f)
                                                else
                                                    Color.White.copy(alpha = 0.35f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active color indicator",
                                                tint = checkmarkColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // Anchored Flyout 12-Color Grid & Recent Colors Popover on Active Slot
                                if (isSelected) {
                                    DropdownMenu(
                                        expanded = showColorPicker,
                                        onDismissRequest = { showColorPicker = false }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Color Palette",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )

                                            // Unified 12-Color Grid (2x6)
                                            // Row 1: First 6 colors
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                palette.take(6).forEach { color ->
                                                    val isColorActive = selectedColor == color.toArgb() ||
                                                        (color == NotePaletteDefaults.InkingPalette[0] && isBlackMatch)
                                                    ColorSwatchItem(
                                                        color = color,
                                                        isSelected = isColorActive,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            NotePaletteDefaults.recordUsedColor(color)
                                                            onColorSelected(color.toArgb())
                                                            showColorPicker = false
                                                        }
                                                    )
                                                }
                                            }

                                            // Row 2: Second 6 colors
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                palette.drop(6).take(6).forEach { color ->
                                                    val isColorActive = selectedColor == color.toArgb() ||
                                                        (color == NotePaletteDefaults.InkingPalette[0] && isBlackMatch)
                                                    ColorSwatchItem(
                                                        color = color,
                                                        isSelected = isColorActive,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            NotePaletteDefaults.recordUsedColor(color)
                                                            onColorSelected(color.toArgb())
                                                            showColorPicker = false
                                                        }
                                                    )
                                                }
                                            }

                                            // Synchronized Recent Colors Row
                                            if (recentColors.isNotEmpty()) {
                                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                                Text(
                                                    text = "Recent Colors",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    recentColors.take(6).forEach { color ->
                                                        val isColorActive = selectedColor == color.toArgb() ||
                                                            (color == NotePaletteDefaults.InkingPalette[0] && isBlackMatch)
                                                        ColorSwatchItem(
                                                            color = color,
                                                            isSelected = isColorActive,
                                                            onClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                NotePaletteDefaults.recordUsedColor(color)
                                                                onColorSelected(color.toArgb())
                                                                showColorPicker = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Vertical Divider between Color Well and Stroke Widths
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(ActionDarkBlue.copy(alpha = 0.2f))
                    )

                    // Row of Stroke Widths
                    strokeWidths.forEach { width ->
                        IconButton(onClick = { onWidthSelected(width) }) {
                            Box(
                                modifier = Modifier
                                    .size((width / 2 + 8).coerceIn(12f, 32f).dp)
                                    .background(ActionDarkBlue, CircleShape)
                                    .alpha(if (selectedWidth == width) 1f else 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Outer selection ring (PrimaryCyanBlue)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .border(2.dp, PrimaryCyanBlue, CircleShape)
            )
        }

        // Inner color circle with adaptive checkmark badge
        val checkmarkColor = if (color.luminance() > 0.5f) Color.Black else Color.White
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 1.dp,
                    color = if (color == Color.White || color.luminance() > 0.85f)
                        Color.Gray.copy(alpha = 0.5f)
                    else
                        Color.White.copy(alpha = 0.25f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active color",
                    tint = checkmarkColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ColorSwatchChip(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) = ColorSwatchItem(color = color, isSelected = isSelected, onClick = onClick)

