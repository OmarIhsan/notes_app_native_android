package com.mal5odha.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import kotlin.math.roundToInt

/**
 * Contextual Floating Action Pill for Academic Lasso Selections:
 * - [Copy Text]: On-device OCR / LaTeX recognition extraction
 * - [Make Flashcard]: Convert selected diagram/formula into active recall flashcard
 * - [Re-Color]: Quick semantic restyling using academic palette
 * - [Delete]: Vector deletion of lassoed elements
 *
 * Accepts [x] and [y] offset coordinates to anchor directly above the selection bounding box.
 */
@Composable
fun LassoActionPill(
    onCopyText: () -> Unit,
    onMakeFlashcard: () -> Unit,
    onReColor: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    x: Float = 0f,
    y: Float = 0f,
    selectedCount: Int = 1
) {
    var showColorMenu by remember { mutableStateOf(false) }

    val palette = com.mal5odha.app.ui.theme.NotePaletteDefaults.InkingPalette

    Surface(
        modifier = modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .wrapContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Count indicator badge if multiple strokes are selected
            if (selectedCount > 1) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryCyanBlue.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "$selectedCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ActionDarkBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 1. [Copy Text / OCR]
            IconButton(
                onClick = onCopyText,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Text",
                    tint = ActionDarkBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. [Make Flashcard]
            IconButton(
                onClick = onMakeFlashcard,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Make Flashcard",
                    tint = PrimaryCyanBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3. [Re-Color]
            Box {
                IconButton(
                    onClick = { showColorMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Re-Color",
                        tint = ActionDarkBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showColorMenu,
                    onDismissRequest = { showColorMenu = false }
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            palette.take(6).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                                        .clickable {
                                            com.mal5odha.app.ui.theme.NotePaletteDefaults.recordUsedColor(color)
                                            onReColor(color.toArgb())
                                            showColorMenu = false
                                        }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            palette.drop(6).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                                        .clickable {
                                            com.mal5odha.app.ui.theme.NotePaletteDefaults.recordUsedColor(color)
                                            onReColor(color.toArgb())
                                            showColorMenu = false
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Subtle divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 4. [Delete]
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Selection",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
