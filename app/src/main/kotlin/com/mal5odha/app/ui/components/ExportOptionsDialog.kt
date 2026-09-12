package com.mal5odha.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.pdf.PdfExportMode

/**
 * Material 3 Export Dialog offering choice between Vector PDF and 300 DPI Flattened PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsDialog(
    isExporting: Boolean,
    progress: Float = 0f,
    exportError: String?,
    onDismiss: () -> Unit,
    onExportPdf: (PdfExportMode) -> Unit
) {
    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Export Document",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ActionDarkBlue
                )

                if (isExporting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = PrimaryCyanBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "Exporting PDF... ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ActionDarkBlue
                        )
                    }
                } else {
                    Text(
                        text = "Choose your desired PDF output format:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Vector PDF
                    OutlinedCard(
                        onClick = { onExportPdf(PdfExportMode.VECTOR) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryCyanBlue.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = PrimaryCyanBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Vector PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ActionDarkBlue
                                )
                                Text(
                                    text = "Crisp Bézier curves with infinite zoom sharpness & minimal file size.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: Flattened PDF
                    OutlinedCard(
                        onClick = { onExportPdf(PdfExportMode.FLATTENED) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = ActionDarkBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Flattened PDF (300 DPI)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ActionDarkBlue
                                )
                                Text(
                                    text = "High-resolution rasterization ideal for universal compatibility & print.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (exportError != null) {
                        Text(
                            text = "Error: $exportError",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}
