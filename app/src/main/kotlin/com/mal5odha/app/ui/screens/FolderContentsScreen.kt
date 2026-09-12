package com.mal5odha.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mal5odha.app.ui.components.DocumentCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@kotlin.OptIn(
        androidx.compose.material3.ExperimentalMaterial3Api::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun FolderContentsScreen(
        folderId: String,
        onNavigateBack: () -> Unit,
        onNavigateToDocument: (String) -> Unit,
        viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val documents = uiState.documents

    // Filter documents by parent folder
    val folderDocs = documents.filter { it.parentFolderId == folderId }
    val folderDoc = documents.find { it.id == folderId }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(folderDoc?.title ?: "Folder") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (folderDocs.isEmpty()) {
                Text(
                        "This folder is empty.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                ) {
                    items(folderDocs.size) { index ->
                        val document = folderDocs[index]
                        DocumentCard(
                                title = document.title,
                                date = dateFormat.format(Date(document.modifiedAt)),
                                type = document.type,
                                thumbnailPath = document.thumbnailPath,
                                paperColor = document.paperColor,
                                isSelected = false,
                                onClick = { onNavigateToDocument(document.id) },
                                onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}
