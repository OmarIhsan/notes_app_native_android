package com.mal5odha.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mal5odha.core.data.services.ErrorHandlingService

@Composable
fun GlobalRecoveryDialog(errorHandlingService: ErrorHandlingService) {
    val currentError by errorHandlingService.currentError.collectAsState()

    if (currentError != null) {
        AlertDialog(
                onDismissRequest = { errorHandlingService.clearError() },
                icon = { Icon(Icons.Filled.Warning, contentDescription = "Error Warning") },
                title = { Text(text = "An Error Occurred") },
                text = {
                    Column {
                        Text(
                                text =
                                        "The application encountered an unexpected issue and needs to recover.",
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = currentError?.message ?: "Unknown Error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { errorHandlingService.recoverSession() }) {
                        Text("Recover Session")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { errorHandlingService.clearError() }) { Text("Dismiss") }
                }
        )
    }
}
