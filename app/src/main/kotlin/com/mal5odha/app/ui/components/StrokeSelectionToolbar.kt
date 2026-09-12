package com.mal5odha.app.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.NotePaletteDefaults
import com.mal5odha.app.ui.theme.PrimaryCyanBlue

/**
 * Contextual action bar for vector lasso selections:
 * Supports Cut, Copy, Paste, Real-time Color Change, Duplicate, and Delete.
 *
 * Synchronized with [NotePaletteDefaults.InkingPalette] single source of truth.
 */
@Composable
fun StrokeSelectionToolbar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onCut: () -> Unit = {},
    onCopy: () -> Unit = {},
    onPaste: () -> Unit = {},
    hasClipboard: Boolean = false,
    activeColor: Int? = null,
    onColorSelected: (Color) -> Unit = {},
    onChangeColor: (Int) -> Unit = {},
    onDuplicate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val recentColors by NotePaletteDefaults.recentColors.collectAsState()
    val palette = NotePaletteDefaults.InkingPalette
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .padding(16.dp)
            .wrapContentWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.labelMedium,
                color = ActionDarkBlue,
                modifier = Modifier.padding(end = 4.dp, start = 4.dp)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Cut
            IconButton(onClick = onCut, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Cut",
                    tint = ActionDarkBlue,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Copy
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = ActionDarkBlue,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Paste
            IconButton(
                onClick = onPaste,
                enabled = hasClipboard,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    tint = if (hasClipboard) PrimaryCyanBlue else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Change Color (with active color chip indicator)
            Box {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showColorPicker = true
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    if (activeColor != null) {
                        val activeComposeColor = Color(activeColor)
                        val checkmarkColor = if (activeComposeColor.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(activeComposeColor)
                                .border(1.5.dp, PrimaryCyanBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active color",
                                tint = checkmarkColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change Color",
                            tint = PrimaryCyanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showColorPicker,
                    onDismissRequest = { showColorPicker = false }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Color Palette",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        // Row 1: First 6 colors
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            palette.take(6).forEach { color ->
                                val isSelected = activeColor != null && (activeColor == color.toArgb() || activeColor == color.toArgb().toInt())
                                ColorSwatchItem(
                                    color = color,
                                    isSelected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        NotePaletteDefaults.recordUsedColor(color)
                                        onColorSelected(color)
                                        onChangeColor(color.toArgb())
                                        showColorPicker = false
                                    }
                                )
                            }
                        }

                        // Row 2: Second 6 colors
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            palette.drop(6).forEach { color ->
                                val isSelected = activeColor != null && (activeColor == color.toArgb() || activeColor == color.toArgb().toInt())
                                ColorSwatchItem(
                                    color = color,
                                    isSelected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        NotePaletteDefaults.recordUsedColor(color)
                                        onColorSelected(color)
                                        onChangeColor(color.toArgb())
                                        showColorPicker = false
                                    }
                                )
                            }
                        }

                        // Recent colors row (if any)
                        if (recentColors.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Recent Colors",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                recentColors.forEach { color ->
                                    val isSelected = activeColor != null && (activeColor == color.toArgb() || activeColor == color.toArgb().toInt())
                                    ColorSwatchItem(
                                        color = color,
                                        isSelected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            NotePaletteDefaults.recordUsedColor(color)
                                            onColorSelected(color)
                                            onChangeColor(color.toArgb())
                                            showColorPicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Duplicate
            IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.CopyAll,
                    contentDescription = "Duplicate Selection",
                    tint = PrimaryCyanBlue,
                    modifier = Modifier.size(17.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Selection",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(17.dp)
                )
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
        // Outer selection ring (matching top toolbar active chip)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .border(2.dp, PrimaryCyanBlue, CircleShape)
            )
        }

        // Inner color circle with checkmark badge
        val checkmarkColor = if (color.luminance() > 0.5f) Color.Black else Color.White
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 1.dp,
                    color = if (color == Color.White || color.luminance() > 0.85f) Color.Gray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f),
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
