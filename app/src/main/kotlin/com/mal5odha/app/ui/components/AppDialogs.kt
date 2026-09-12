package com.mal5odha.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable


@Composable
fun RecoveryDialog(onDismiss: () -> Unit, onRecover: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recover Work?") },
        text = { Text("The app closed unexpectedly. Recover unsaved work?") },
        confirmButton = {
            TextButton(onClick = onRecover) { Text("Recover") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Discard") }
        }
    )
}
