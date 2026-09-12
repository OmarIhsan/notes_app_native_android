package com.mal5odha.app.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun ScannerScreen(
    onScanSuccess: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val activity = LocalContext.current as Activity
    var scannedPagesCount by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }


    val options = remember {
        GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(30)
                .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                .setScannerMode(SCANNER_MODE_FULL)
                .build()
    }

    val scanner = remember { GmsDocumentScanning.getClient(options) }

    val scannerLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                    scanResult?.let {
                        scannedPagesCount = it.pages?.size ?: 0
                        viewModel.processScanResult(it)
                    }
                }
            }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ScannerUiState.Success -> onScanSuccess((uiState as ScannerUiState.Success).documentId)
            is ScannerUiState.Error -> errorMsg = (uiState as ScannerUiState.Error).message
            else -> {}
        }
    }


    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        if (uiState is ScannerUiState.Processing) {
            CircularProgressIndicator()
            Text("Optimizing scan and creating note...", modifier = Modifier.padding(top = 16.dp))
        } else if (errorMsg != null) {

            Text(
                    "Capture, crop, and enhance documents using Google ML Kit Scanner.",
                    style = MaterialTheme.typography.bodyMedium
            )
        }

        errorMsg?.let {
            Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
            )
        }

        Button(
                modifier = Modifier.padding(top = 24.dp),
                onClick = {
                    errorMsg = null
                    scanner.getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener { e -> errorMsg = e.localizedMessage }
                }
        ) { Text("Start Document Scan") }
    }
}
