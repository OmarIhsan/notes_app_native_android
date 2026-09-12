package com.mal5odha.app.ui.components.dock

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.NotePaletteDefaults
import com.mal5odha.app.ui.theme.PrimaryCyanBlue

/**
 * Curated 3-slot Academic Inking Palette for Zero-Tap color switching:
 * - InkBlack: Primary lecture notation and formulas (#1E1E1E)
 * - AcademicBlue: Sub-headers, theorem definitions, diagrams (#1976D2)
 * - AlertCrimson: Exam alerts, urgent corrections, deadlines (#D32F2F)
 */
object AcademicPalette {
    val InkBlack = AndroidColor.parseColor("#1E1E1E") // Pitch Black from NotePaletteDefaults
    val AcademicBlue = AndroidColor.parseColor("#1976D2") // Primary Blue from NotePaletteDefaults
    val AlertCrimson = AndroidColor.parseColor("#D32F2F") // Crimson Red from NotePaletteDefaults

    val DefaultSlots = listOf(InkBlack, AcademicBlue, AlertCrimson)
}

/**
 * ZeroTapColorWell provides instantaneous, 1-tap switching between 3 dynamic colors
 * plus an anchored 12-color popover grid with recent colors matching StrokeSelectionToolbar.
 *
 * It dynamically morphs its flex layout (Row <-> Column) based on [isVertical].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZeroTapColorWell(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    slots: List<Int>? = null,
    onColorLongClick: (Int) -> Unit = {}
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val recentColors by NotePaletteDefaults.recentColors.collectAsState()
    val palette = NotePaletteDefaults.InkingPalette
    val haptic = LocalHapticFeedback.current

    val defaultFallback = remember {
        listOf(
            NotePaletteDefaults.InkingPalette[0], // Pitch Black (#1E1E1E)
            NotePaletteDefaults.InkingPalette[9], // Royal Blue (#1976D2)
            NotePaletteDefaults.InkingPalette[2]  // Crimson Red (#D32F2F)
        )
    }

    val isBlackMatch = selectedColor == AndroidColor.BLACK || selectedColor == NotePaletteDefaults.InkingPalette[0].toArgb()
    val canonicalSelected = if (isBlackMatch) NotePaletteDefaults.InkingPalette[0] else Color(selectedColor)

    val visibleSlots: List<Color> = remember(slots, recentColors, selectedColor) {
        if (slots != null && slots != AcademicPalette.DefaultSlots) {
            slots.map { Color(it) }
        } else {
            (listOf(canonicalSelected) + recentColors + defaultFallback).distinct().take(3)
        }
    }

    val activeSlotIndex = visibleSlots.indexOfFirst { slotColor ->
        slotColor.toArgb() == selectedColor || (isBlackMatch && slotColor == NotePaletteDefaults.InkingPalette[0])
    }.let { if (it >= 0) it else 0 }

    val content = @Composable {
        visibleSlots.forEachIndexed { index, slotColor ->
            val isSelected = index == activeSlotIndex
            val colorInt = slotColor.toArgb()

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                animationSpec = spring(dampingRatio = 0.6f),
                label = "colorSlotScale"
            )

            Box(contentAlignment = Alignment.Center) {
                // 38dp touch target with centered visual chip
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isSelected) {
                                    showColorPicker = !showColorPicker
                                } else {
                                    showColorPicker = false
                                    NotePaletteDefaults.recordUsedColor(slotColor)
                                    onColorSelected(colorInt)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showColorPicker = true
                                onColorLongClick(colorInt)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer ring indicator for active selection (M3 Primary accent)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    width = 2.dp,
                                    color = PrimaryCyanBlue,
                                    shape = CircleShape
                                )
                        )
                    }

                    // Inner Color Chip with depth border
                    val checkmarkColor = if (slotColor.luminance() > 0.5f) Color.Black else Color.White
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(slotColor)
                            .border(
                                width = 1.dp,
                                color = if (slotColor == Color.White || slotColor.luminance() > 0.85f) {
                                    Color.Gray.copy(alpha = 0.5f)
                                } else {
                                    Color.White.copy(alpha = 0.35f)
                                },
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

                // Anchored DropdownMenu on the active slot
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

                            // Recent Colors Row
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

    if (isVertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun ColorSwatchItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val checkmarkColor = if (color.luminance() > 0.5f) Color.Black else Color.White
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

        // Inner color circle with checkmark badge
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
