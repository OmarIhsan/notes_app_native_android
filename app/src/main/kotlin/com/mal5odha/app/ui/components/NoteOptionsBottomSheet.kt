package com.mal5odha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.data.models.DocumentType
import com.mal5odha.core.data.models.UnifiedDocument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Comprehensive Note Options Bottom Sheet and contextual menu actions:
 * - Rename Note
 * - Move to Folder
 * - Duplicate
 * - Star / Favorite
 * - Export (Vector PDF, Flattened PDF)
 * - Note Details (File size, page count, timestamps)
 * - Delete / Trash
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteOptionsBottomSheet(
    document: UnifiedDocument,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleStar: () -> Unit,
    onExport: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Icon + Title + Type Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryCyanBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (document.type) {
                            DocumentType.PDF -> Icons.Default.PictureAsPdf
                            DocumentType.FOLDER -> Icons.Default.Folder
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = PrimaryCyanBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (document.type) {
                            DocumentType.PDF -> "PDF Document"
                            DocumentType.FOLDER -> "Folder"
                            else -> "Notebook (${document.paperType.lowercase().replaceFirstChar { it.uppercase() }})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Rename Note
            OptionItemRow(
                icon = Icons.Default.Edit,
                title = "Rename",
                onClick = {
                    onDismiss()
                    onRename()
                }
            )

            // 2. Move to Folder
            OptionItemRow(
                icon = Icons.Default.DriveFileMove,
                title = "Move to Folder",
                onClick = {
                    onDismiss()
                    onMove()
                }
            )

            // 3. Duplicate Note
            OptionItemRow(
                icon = Icons.Default.ContentCopy,
                title = "Duplicate",
                onClick = {
                    onDismiss()
                    onDuplicate()
                }
            )

            // 4. Star / Favorite
            OptionItemRow(
                icon = if (document.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                iconTint = if (document.isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface,
                title = if (document.isStarred) "Remove from Starred" else "Add to Starred",
                onClick = {
                    onDismiss()
                    onToggleStar()
                }
            )

            // 5. Export
            if (document.type != DocumentType.FOLDER) {
                OptionItemRow(
                    icon = Icons.Default.Share,
                    title = "Export...",
                    onClick = {
                        onDismiss()
                        onExport()
                    }
                )
            }

            // 6. Note Details
            OptionItemRow(
                icon = Icons.Default.Info,
                title = "Note Details",
                onClick = {
                    onDismiss()
                    onDetails()
                }
            )

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // 7. Delete / Trash
            OptionItemRow(
                icon = Icons.Default.Delete,
                iconTint = MaterialTheme.colorScheme.error,
                title = "Move to Trash",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun OptionItemRow(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = titleColor
        )
    }
}

/**
 * In-place Rename Note Dialog
 */
@Composable
fun RenameNoteDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Note", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Move to Folder Selector Dialog
 */
@Composable
fun MoveToFolderDialog(
    folders: List<UnifiedDocument>,
    currentFolderId: String?,
    onDismiss: () -> Unit,
    onFolderSelected: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Folder", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Root Option (No folder / All Notes)
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onFolderSelected(null) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PrimaryCyanBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Root (All Notes)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentFolderId == null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                items(folders) { folder ->
                    val isCurrent = folder.id == currentFolderId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onFolderSelected(folder.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        val folderColor = try {
                            Color(android.graphics.Color.parseColor(folder.colorHex))
                        } catch (e: Exception) {
                            PrimaryCyanBlue
                        }
                        Icon(Icons.Default.Folder, contentDescription = null, tint = folderColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = folder.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrent) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Note Details Dialog showing file size, page count, and edit timestamps
 */
@Composable
fun NoteDetailsDialog(
    document: UnifiedDocument,
    fileSizeBytes: Long,
    pageCount: Int,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }
    val formattedCreated = remember(document.createdAt) { dateFormat.format(Date(document.createdAt)) }
    val formattedModified = remember(document.modifiedAt) { dateFormat.format(Date(document.modifiedAt)) }

    val formattedSize = remember(fileSizeBytes) {
        when {
            fileSizeBytes < 1024 -> "$fileSizeBytes B"
            fileSizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", fileSizeBytes / 1024.0)
            else -> String.format(Locale.US, "%.2f MB", fileSizeBytes / (1024.0 * 1024.0))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryCyanBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Note Details", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailItemRow(label = "Title", value = document.title)
                DetailItemRow(label = "Type", value = when (document.type) {
                    DocumentType.PDF -> "PDF Document"
                    DocumentType.FOLDER -> "Folder"
                    else -> "Notebook (${document.paperType})"
                })
                DetailItemRow(label = "Pages", value = "$pageCount")
                DetailItemRow(label = "Disk Usage", value = formattedSize)
                DetailItemRow(label = "Created", value = formattedCreated)
                DetailItemRow(label = "Last Modified", value = formattedModified)
                DetailItemRow(label = "Favorite / Starred", value = if (document.isStarred) "Yes" else "No")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
