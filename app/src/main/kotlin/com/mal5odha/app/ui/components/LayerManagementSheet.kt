package com.mal5odha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.models.DocumentLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerManagementSheet(
    layers: List<DocumentLayer>,
    activeLayerId: String?,
    onSelectActiveLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onOpacityChanged: (String, Float) -> Unit,
    onAddLayer: (String) -> Unit,
    onReorderLayers: (Int, Int) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newLayerName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = PrimaryCyanBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Layers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ActionDarkBlue
                    )
                    Text(
                        text = "(${layers.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            newLayerName = "Layer ${layers.size + 1}"
                            showAddDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Layer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 13.sp)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(12.dp))

            // Layers List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(layers, key = { _, layer -> layer.id }) { index, layer ->
                    val isActive = layer.id == activeLayerId

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isActive) {
                            PrimaryCyanBlue.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) PrimaryCyanBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectActiveLayer(layer.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Active indicator
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) PrimaryCyanBlue else Color.Transparent)
                                    )

                                    Text(
                                        text = layer.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) PrimaryCyanBlue else ActionDarkBlue
                                    )

                                    if (layer.isLocked) {
                                        Text(
                                            text = "Locked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Quick toggles: Reorder Up/Down, Lock, Visibility
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Move Up
                                    IconButton(
                                        onClick = { if (index > 0) onReorderLayers(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (index > 0) ActionDarkBlue else Color.LightGray
                                        )
                                    }

                                    // Move Down
                                    IconButton(
                                        onClick = { if (index < layers.size - 1) onReorderLayers(index, index + 1) },
                                        enabled = index < layers.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (index < layers.size - 1) ActionDarkBlue else Color.LightGray
                                        )
                                    }

                                    // Lock / Unlock
                                    IconButton(
                                        onClick = { onToggleLock(layer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Lock",
                                            tint = if (layer.isLocked) MaterialTheme.colorScheme.error else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Visibility
                                    IconButton(
                                        onClick = { onToggleVisibility(layer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Visibility",
                                            tint = if (layer.isVisible) PrimaryCyanBlue else Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete
                                    if (layers.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteLayer(layer.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Layer",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Layer Opacity Slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Opacity: ${(layer.alpha * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.width(76.dp)
                                )
                                Slider(
                                    value = layer.alpha,
                                    onValueChange = { onOpacityChanged(layer.id, it) },
                                    valueRange = 0.05f..1.0f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Add Layer Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Layer") },
                text = {
                    OutlinedTextField(
                        value = newLayerName,
                        onValueChange = { newLayerName = it },
                        label = { Text("Layer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newLayerName.isNotBlank()) {
                                onAddLayer(newLayerName.trim())
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
