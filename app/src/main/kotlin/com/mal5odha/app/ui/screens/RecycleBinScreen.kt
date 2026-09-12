package com.mal5odha.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.app.ui.theme.SecondaryGray
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin", color = ActionDarkBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.deletedDocuments.isNotEmpty()) {
                        TextButton(onClick = { 
                            viewModel.purgeItems(uiState.deletedDocuments.map { it.id })
                        }) {
                            Text("Empty Bin", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.deletedDocuments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Recycle bin is empty", color = SecondaryGray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.deletedDocuments.size) { index ->
                        val document = uiState.deletedDocuments[index]
                        BinCard(
                            title = document.title,
                            date = dateFormat.format(java.util.Date(document.modifiedAt)),
                            onRestore = { viewModel.restoreItems(listOf(document.id)) },
                            onPurge = { viewModel.purgeItems(listOf(document.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BinCard(
    title: String,
    date: String,
    onRestore: () -> Unit,
    onPurge: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SecondaryGray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = SecondaryGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ActionDarkBlue,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(date, style = MaterialTheme.typography.labelSmall, color = SecondaryGray)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restore", tint = PrimaryCyanBlue)
                    }
                    IconButton(onClick = onPurge) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
