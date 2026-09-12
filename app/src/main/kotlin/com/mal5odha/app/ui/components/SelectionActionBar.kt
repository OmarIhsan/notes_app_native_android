package com.mal5odha.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionActionBar(
        selectedCount: Int,
        onClearSelection: () -> Unit,
        onDelete: () -> Unit,
        onMove: () -> Unit = {},
        onCopy: () -> Unit = {}
) {
    Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
            }
            Text(
                    text = "$selectedCount Selected",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
            )
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.MailOutline, contentDescription = "Copy") // Placeholder for Copy
            }
            IconButton(onClick = onMove) {
                Icon(Icons.Default.Share, contentDescription = "Move") // Placeholder for Move
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
