package com.mal5odha.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PdfImportScreen(
        onNavigateToEditor: (String) -> Unit,
        viewModel: PdfImportViewModel = hiltViewModel()
) {
    val isImporting by viewModel.isImporting.collectAsState()
    val importError by viewModel.importError.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf<String>("") }

    val pdfPickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri: Uri? ->
                if (uri != null) {
                    selectedPdfUri = uri
                    val rawName = try {
                        context.contentResolver.query(
                            uri,
                            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (idx != -1) cursor.getString(idx) else null
                            } else null
                        }
                    } catch (e: Exception) {
                        null
                    } ?: (uri.lastPathSegment ?: "Imported Document")
                    selectedPdfName = com.mal5odha.core.data.factory.DocumentNameFactory.fromImportedUri(rawName)
                }
            }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                    "Import PDF",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isImporting) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Extracting PDF pages, please wait...")
            } else {
                if (selectedPdfUri == null) {
                    Text("Select a PDF document from your device to import.")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Select PDF File")
                    }
                } else {
                    Text("Selected: $selectedPdfName", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                            onClick = {
                                viewModel.handlePdfImport(selectedPdfUri!!, selectedPdfName) {
                                        documentId ->
                                    onNavigateToEditor(documentId)
                                }
                            }
                    ) { Text("Create Note from PDF") }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                            onClick = { selectedPdfUri = null },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors()
                    ) { Text("Cancel") }
                }
            }

            importError?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
