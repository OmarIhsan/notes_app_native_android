package com.mal5odha.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * DocumentsScreen delegating directly to the unified DashboardScreen shelf.
 */
@Composable
fun DocumentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToFolder: (String) -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    DashboardScreen(
        onNavigateToEditor = onNavigateToEditor,
        onNavigateToScanner = { onNavigateTo("scanner") },
        onNavigateToFolder = onNavigateToFolder,
        onNavigateTo = onNavigateTo,
        viewModel = viewModel
    )
}
