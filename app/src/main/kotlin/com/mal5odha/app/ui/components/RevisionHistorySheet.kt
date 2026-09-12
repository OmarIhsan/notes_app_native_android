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
import com.mal5odha.core.data.models.DocumentMutation
import com.mal5odha.core.data.models.MutationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionHistorySheet(
    mutations: List<DocumentMutation>,
    onRollbackToMutation: (DocumentMutation) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }
    var selectedMutationIndex by remember(mutations) {
        mutableStateOf(if (mutations.isNotEmpty()) mutations.size - 1 else 0)
    }

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
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = PrimaryCyanBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Revision History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ActionDarkBlue
                    )
                    Text(
                        text = "(${mutations.size} entries)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (mutations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded revisions for this session yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                // Timeline Scrubber Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time-Travel Scrubber",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ActionDarkBlue
                        )
                        val activeMutation = mutations.getOrNull(selectedMutationIndex)
                        if (activeMutation != null) {
                            Text(
                                text = dateFormat.format(Date(activeMutation.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyanBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Slider(
                        value = selectedMutationIndex.toFloat(),
                        onValueChange = { selectedMutationIndex = it.toInt().coerceIn(0, mutations.size - 1) },
                        valueRange = 0f..(mutations.size - 1).toFloat(),
                        steps = if (mutations.size > 2) mutations.size - 2 else 0,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val selectedMutation = mutations.getOrNull(selectedMutationIndex)
                    if (selectedMutation != null && selectedMutationIndex < mutations.size - 1) {
                        Button(
                            onClick = {
                                onRollbackToMutation(selectedMutation)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rollback to this point", fontSize = 13.sp)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(8.dp))

                // Timeline List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(mutations.reversed()) { revIdx, mutation ->
                        val originalIndex = mutations.size - 1 - revIdx
                        val isSelected = originalIndex == selectedMutationIndex

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryCyanBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PrimaryCyanBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMutationIndex = originalIndex }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val icon = when (mutation.type) {
                                        MutationType.ADD_STROKE -> Icons.Default.Edit
                                        MutationType.REMOVE_STROKE -> Icons.Default.DeleteOutline
                                        MutationType.ADD_TEXT -> Icons.Default.TextFields
                                        MutationType.REMOVE_TEXT -> Icons.Default.Delete
                                        MutationType.CLEAR_PAGE -> Icons.Default.DeleteSweep
                                        MutationType.ADD_PAGE -> Icons.Default.NoteAdd
                                        MutationType.DELETE_PAGE -> Icons.Default.DeleteForever
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PrimaryCyanBlue else Color.LightGray.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else ActionDarkBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = when (mutation.type) {
                                                MutationType.ADD_STROKE -> "Inked Stroke"
                                                MutationType.REMOVE_STROKE -> "Erased Stroke"
                                                MutationType.ADD_TEXT -> "Added Text Note"
                                                MutationType.REMOVE_TEXT -> "Deleted Text Note"
                                                MutationType.CLEAR_PAGE -> "Cleared Page"
                                                MutationType.ADD_PAGE -> "Added Page"
                                                MutationType.DELETE_PAGE -> "Deleted Page"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = ActionDarkBlue
                                        )
                                        Text(
                                            text = dateFormat.format(Date(mutation.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (originalIndex < mutations.size - 1) {
                                    TextButton(
                                        onClick = {
                                            onRollbackToMutation(mutation)
                                            onDismiss()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Restore", fontSize = 12.sp, color = PrimaryCyanBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
