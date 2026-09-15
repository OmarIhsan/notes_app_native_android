package com.mal5odha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.models.TextAnnotation

/**
 * Convenience overload accepting [TextAnnotation] directly.
 */
@Composable
fun FloatingTextFormattingBar(
    annotation: TextAnnotation,
    onUpdate: (TextAnnotation) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FloatingTextFormattingBar(
        fontSizeSp = annotation.fontSizeSp,
        colorHex = annotation.colorHex,
        isBold = annotation.isBold,
        isItalic = annotation.isItalic,
        isUnderline = annotation.isUnderline,
        textAlign = annotation.textAlign,
        onFontSizeChange = { onUpdate(annotation.copy(fontSizeSp = it)) },
        onColorChange = { onUpdate(annotation.copy(colorHex = it)) },
        onBoldToggle = { onUpdate(annotation.copy(isBold = !annotation.isBold)) },
        onItalicToggle = { onUpdate(annotation.copy(isItalic = !annotation.isItalic)) },
        onUnderlineToggle = { onUpdate(annotation.copy(isUnderline = !annotation.isUnderline)) },
        onAlignmentChange = { onUpdate(annotation.copy(textAlign = it)) },
        onDelete = onDelete,
        onDone = onDone,
        modifier = modifier
    )
}

/**
 * 5 Essential Palette Colors for text annotations:
 * Black, Slate Gray, Electric Red, Navy Blue, Forest Green.
 */
val EssentialTextColors = listOf(
    0xFF1A1A1AL, // Black / Charcoal
    0xFF64748BL, // Slate Gray
    0xFFE53935L, // Electric Red
    0xFF1E3A8AL, // Navy Blue
    0xFF2E7D32L  // Forest Green
)

/**
 * Compact, detached floating formatting bar pill adhering to Goodnotes/Notewise ergonomics.
 *
 * Provides:
 * - Font Size Stepper (12sp - 36sp)
 * - 5 Essential Palette Color Swatches
 * - Rich Style Toggles (Bold, Italic, Underline)
 * - Text Alignment Toggles (Left, Center, Right)
 * - Immediate Delete Action with haptic feedback
 * - Done action to finish editing
 */
@Composable
fun FloatingTextFormattingBar(
    fontSizeSp: Float,
    colorHex: Long,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    textAlign: TextAlign,
    onFontSizeChange: (Float) -> Unit,
    onColorChange: (Long) -> Unit,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    onAlignmentChange: (TextAlign) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        color = ActionDarkBlue,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Row 1: Typography Controls (Size Stepper | B/I/U | Alignment) ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Font Size Stepper (12 - 36sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newSize = (fontSizeSp - 2f).coerceAtLeast(12f)
                            onFontSizeChange(newSize)
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text(
                            text = "–",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "${fontSizeSp.toInt()}pt",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    IconButton(
                        onClick = {
                            val newSize = (fontSizeSp + 2f).coerceAtMost(36f)
                            onFontSizeChange(newSize)
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text(
                            text = "+",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                BarDivider()

                // Style Toggles: Bold, Italic, Underline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FormattingToggleChip(
                        label = "B",
                        isActive = isBold,
                        fontWeight = FontWeight.ExtraBold,
                        onClick = onBoldToggle
                    )

                    FormattingToggleChip(
                        label = "I",
                        isActive = isItalic,
                        fontStyle = FontStyle.Italic,
                        onClick = onItalicToggle
                    )

                    FormattingToggleChip(
                        label = "U",
                        isActive = isUnderline,
                        textDecoration = TextDecoration.Underline,
                        onClick = onUnderlineToggle
                    )
                }

                BarDivider()

                // Alignment Toggle
                IconButton(
                    onClick = {
                        val nextAlign = when (textAlign) {
                            TextAlign.Start, TextAlign.Left -> TextAlign.Center
                            TextAlign.Center -> TextAlign.End
                            else -> TextAlign.Start
                        }
                        onAlignmentChange(nextAlign)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = when (textAlign) {
                            TextAlign.Center -> Icons.Default.FormatAlignCenter
                            TextAlign.End, TextAlign.Right -> Icons.Default.FormatAlignRight
                            else -> Icons.Default.FormatAlignLeft
                        },
                        contentDescription = "Text Alignment",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Subtle Horizontal Separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // ─── Row 2: Color Palette & Action Buttons ───────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 5 Color Swatches
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EssentialTextColors.forEach { colorValue ->
                        val isSelected = (colorHex and 0xFFFFFFL) == (colorValue and 0xFFFFFFL)
                        val swatchColor = Color(colorValue)

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryCyanBlue else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onColorChange(colorValue)
                                }
                        )
                    }
                }

                BarDivider()

                // Delete Action (with Haptic Feedback)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Text Box",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Done / Commit Button
                IconButton(
                    onClick = onDone,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done Editing",
                        tint = PrimaryCyanBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattingToggleChip(
    label: String,
    isActive: Boolean,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    textDecoration: TextDecoration = TextDecoration.None,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) PrimaryCyanBlue else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun BarDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(1.dp)
            .height(20.dp)
            .background(Color.White.copy(alpha = 0.2f))
    )
}
