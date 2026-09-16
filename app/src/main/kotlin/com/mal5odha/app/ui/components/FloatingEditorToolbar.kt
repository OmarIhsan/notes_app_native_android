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
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.models.InkTool

/**
 * FloatingEditorToolbar: Interactive floating editor toolbar for document pages.
 * Exposes core inking tools, shape tools, and the Text tool (T) for freeform text box creation.
 */
@Composable
fun FloatingEditorToolbar(
    currentTool: InkTool,
    onToolSelected: (InkTool) -> Unit,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    canUndo: Boolean = true,
    canRedo: Boolean = true,
    selectedColor: Int = android.graphics.Color.BLACK,
    onColorSelected: (Int) -> Unit = {},
    selectedWidth: Float = 5f,
    onWidthSelected: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    isReadOnlyMode: Boolean = false
) {
    Surface(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: Undo/Redo & Tool Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isReadOnlyMode) {
                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) ActionDarkBlue else ActionDarkBlue.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) ActionDarkBlue else ActionDarkBlue.copy(alpha = 0.38f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                }

                // Pen
                IconButton(onClick = { onToolSelected(InkTool.PEN) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Pen Tool",
                        tint = if (currentTool == InkTool.PEN) PrimaryCyanBlue else ActionDarkBlue
                    )
                }

                // Highlighter
                IconButton(onClick = { onToolSelected(InkTool.HIGHLIGHTER) }) {
                    Icon(
                        Icons.Default.Brush,
                        contentDescription = "Highlighter",
                        tint = if (currentTool == InkTool.HIGHLIGHTER) PrimaryCyanBlue else ActionDarkBlue
                    )
                }

                // Eraser
                IconButton(onClick = { onToolSelected(InkTool.ERASER) }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Eraser",
                        tint = if (currentTool == InkTool.ERASER) PrimaryCyanBlue else ActionDarkBlue
                    )
                }

                // Shape
                IconButton(onClick = { onToolSelected(InkTool.SHAPE) }) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = "Shape Tool",
                        tint = if (currentTool == InkTool.SHAPE || currentTool == InkTool.SHAPE_CIRCLE || currentTool == InkTool.SHAPE_RECTANGLE || currentTool == InkTool.SHAPE_ARROW) PrimaryCyanBlue else ActionDarkBlue
                    )
                }

                // Lasso
                IconButton(onClick = { onToolSelected(InkTool.LASSO) }) {
                    Icon(
                        Icons.Default.Crop,
                        contentDescription = "Lasso Tool",
                        tint = if (currentTool == InkTool.LASSO) PrimaryCyanBlue else ActionDarkBlue
                    )
                }

                // Text Tool (T)
                IconButton(onClick = { onToolSelected(InkTool.TEXT) }) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text Tool (T)",
                        tint = if (currentTool == InkTool.TEXT) PrimaryCyanBlue else ActionDarkBlue
                    )
                }

                // Sticky Note Sticker
                IconButton(onClick = { onToolSelected(InkTool.STICKY_CARD) }) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = "Sticky Note Sticker",
                        tint = if (currentTool.isStickyCard) PrimaryCyanBlue else ActionDarkBlue
                    )
                }

                // Laser
                IconButton(onClick = { onToolSelected(InkTool.LASER) }) {
                    Icon(
                        Icons.Default.Highlight,
                        contentDescription = "Laser Pointer",
                        tint = if (currentTool == InkTool.LASER) PrimaryCyanBlue else ActionDarkBlue
                    )
                }
            }
        }
    }
}
