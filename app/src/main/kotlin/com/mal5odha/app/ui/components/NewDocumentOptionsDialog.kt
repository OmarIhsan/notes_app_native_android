package com.mal5odha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDocumentOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, paperType: String, paperColor: Int) -> Unit
) {
    var title by remember { mutableStateOf("Untitled Note") }
    var selectedType by remember { mutableStateOf("Blank") }
    val paperTypes = listOf("Blank", "Ruled", "Grid", "Dotted")
    
    val colorOptions = listOf(
        Color.White to android.graphics.Color.WHITE,
        Color(0xFFFFF9C4) to android.graphics.Color.parseColor("#FFF9C4"), // Yellowish
        Color(0xFF1E1E1E) to android.graphics.Color.parseColor("#1E1E1E")  // Dark
    )
    var selectedColorIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "New Document",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Paper Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paperTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Text("Paper Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colorOptions.forEachIndexed { index, pair ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(pair.first, CircleShape)
                                .border(
                                    width = if (selectedColorIndex == index) 3.dp else 1.dp,
                                    color = if (selectedColorIndex == index) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, selectedType, colorOptions[selectedColorIndex].second)
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
