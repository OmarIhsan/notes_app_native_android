package com.mal5odha.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import com.mal5odha.core.data.preferences.ThemeMode
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mal5odha.app.ui.components.*
import com.mal5odha.app.ui.theme.*
import com.mal5odha.core.data.models.DocumentType
import com.mal5odha.core.data.models.UnifiedDocument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Universal Dashboard with Pinned Tablet Sidebar (260 dp) and Comprehensive Document Shelf.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToEditor: (String) -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToFolder: (String) -> Unit = {},
    onNavigateTo: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentThemeMode by settingsViewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (currentThemeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val sidebarBg = if (isDark) Color(0xFF1E1F22) else Color(0xFFF0F2F5)

    var selectedDocForOptions by remember { mutableStateOf<UnifiedDocument?>(null) }
    var docToRename by remember { mutableStateOf<UnifiedDocument?>(null) }
    var docToMove by remember { mutableStateOf<UnifiedDocument?>(null) }
    var docDetailsInfo by remember { mutableStateOf<Pair<UnifiedDocument, Pair<Long, Int>>?>(null) }
    var docToExport by remember { mutableStateOf<UnifiedDocument?>(null) }

    var showNewNoteDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ─── 1. Pinned Tablet Navigation Sidebar (260 dp) ────────────────────────
        TabletNavigationSidebar(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(sidebarBg),
            uiState = uiState,
            themeMode = currentThemeMode,
            isDark = isDark,
            onSelectThemeMode = { settingsViewModel.setThemeMode(it) },
            onSelectFilter = { viewModel.setFilter(it) },
            onSelectFolder = { viewModel.selectFolder(it) },
            onNewFolderClick = { showNewFolderDialog = true },
            onScannerClick = onNavigateToScanner,
            onSettingsClick = { onNavigateTo("settings") }
        )

        // ─── 2. Main Document Shelf ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // Shelf Top Bar: Title, Search, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when {
                            uiState.selectedFolderId != null -> uiState.selectedFolderName ?: "Folder"
                            uiState.selectedFilter == LibraryFilter.RECENTS -> "Recents"
                            uiState.selectedFilter == LibraryFilter.STARRED -> "Starred Notes"
                            uiState.selectedFilter == LibraryFilter.TRASH -> "Trash"
                            else -> "All Notes"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.displayedDocuments.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search notes...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(240.dp)
                            .height(48.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Grid / List toggle
                    IconButton(
                        onClick = { viewModel.toggleViewMode() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.selectedFilter == LibraryFilter.TRASH) {
                        Button(
                            onClick = { viewModel.emptyTrash() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Empty Trash")
                        }
                    } else {
                        // [+] New Note Button
                        Button(
                            onClick = { showNewNoteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Note", fontWeight = FontWeight.Bold)
                        }

                        // [^] Import PDF Button
                        OutlinedButton(
                            onClick = { onNavigateTo("pdf_import") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import PDF")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content Area: Grid or List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryCyanBlue)
                }
            } else if (uiState.displayedDocuments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No notes match '${uiState.searchQuery}'"
                            else if (uiState.selectedFilter == LibraryFilter.TRASH) "Trash is empty"
                            else "No notes yet in this section",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.displayedDocuments, key = { it.id }) { doc ->
                        val formattedDate = remember(doc.modifiedAt) { dateFormat.format(Date(doc.modifiedAt)) }
                        val pageCount = uiState.documentPageCounts[doc.id] ?: 1

                        DocumentCard(
                            title = doc.title,
                            date = formattedDate,
                            type = doc.type,
                            thumbnailPath = doc.thumbnailPath,
                            paperColor = doc.paperColor,
                            pageCount = pageCount,
                            isStarred = doc.isStarred,
                            colorHex = doc.colorHex,
                            onClick = {
                                if (doc.type == DocumentType.FOLDER) {
                                    viewModel.selectFolder(doc.id)
                                } else {
                                    onNavigateToEditor(doc.id)
                                }
                            },
                            onOptionsClick = {
                                selectedDocForOptions = doc
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.displayedDocuments, key = { it.id }) { doc ->
                        val formattedDate = remember(doc.modifiedAt) { dateFormat.format(Date(doc.modifiedAt)) }
                        val pageCount = uiState.documentPageCounts[doc.id] ?: 1

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (doc.type == DocumentType.FOLDER) {
                                        viewModel.selectFolder(doc.id)
                                    } else {
                                        onNavigateToEditor(doc.id)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = when (doc.type) {
                                            DocumentType.PDF -> Icons.Default.PictureAsPdf
                                            DocumentType.FOLDER -> Icons.Default.Folder
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = PrimaryCyanBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "$formattedDate • ${if (doc.type == DocumentType.FOLDER) "Folder" else "$pageCount pages"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (doc.isStarred) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Starred",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    IconButton(onClick = { selectedDocForOptions = doc }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── 3. Contextual Note Options Bottom Sheet ─────────────────────────────
    selectedDocForOptions?.let { doc ->
        NoteOptionsBottomSheet(
            document = doc,
            onDismiss = { selectedDocForOptions = null },
            onRename = { docToRename = doc },
            onMove = { docToMove = doc },
            onDuplicate = { viewModel.duplicateDocument(doc.id) },
            onToggleStar = { viewModel.toggleStarred(doc.id) },
            onExport = { docToExport = doc },
            onDetails = {
                viewModel.getNoteDetails(doc.id) { size, pages ->
                    docDetailsInfo = Pair(doc, Pair(size, pages))
                }
            },
            onDelete = { viewModel.deleteDocument(doc.id) }
        )
    }

    // Rename Dialog
    docToRename?.let { doc ->
        RenameNoteDialog(
            initialTitle = doc.title,
            onDismiss = { docToRename = null },
            onConfirm = { newTitle ->
                viewModel.renameDocument(doc.id, newTitle)
                docToRename = null
            }
        )
    }

    // Move to Folder Dialog
    docToMove?.let { doc ->
        MoveToFolderDialog(
            folders = uiState.folders,
            currentFolderId = doc.parentFolderId,
            onDismiss = { docToMove = null },
            onFolderSelected = { targetFolderId ->
                viewModel.moveDocumentToFolder(doc.id, targetFolderId)
                docToMove = null
            }
        )
    }

    // Note Details Dialog
    docDetailsInfo?.let { (doc, info) ->
        val (sizeBytes, pages) = info
        NoteDetailsDialog(
            document = doc,
            fileSizeBytes = sizeBytes,
            pageCount = pages,
            onDismiss = { docDetailsInfo = null }
        )
    }

    // Export Options Dialog
    docToExport?.let { doc ->
        ExportOptionsDialog(
            isExporting = false,
            exportError = null,
            onDismiss = { docToExport = null },
            onExportPdf = { mode ->
                docToExport = null
                onNavigateToEditor(doc.id)
            }
        )
    }

    // Create New Note Dialog
    if (showNewNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var selectedPaper by remember { mutableStateOf("BLANK") }

        AlertDialog(
            onDismissRequest = { showNewNoteDialog = false },
            title = { Text("New Notebook", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val defaultTitle = remember { com.mal5odha.core.data.factory.DocumentNameFactory.defaultNotebookTitle() }
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Notebook Title") },
                        placeholder = { Text(defaultTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Paper Template", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("BLANK", "RULED", "GRID", "DOTTED").forEach { type ->
                            FilterChip(
                                selected = selectedPaper == type,
                                onClick = { selectedPaper = type },
                                label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (noteTitle.isNotBlank()) noteTitle.trim() else com.mal5odha.core.data.factory.DocumentNameFactory.defaultNotebookTitle()
                        viewModel.createNewNote(
                            title = title,
                            paperType = selectedPaper,
                            paperColor = -1,
                            onCreated = { newId ->
                                showNewNoteDialog = false
                                onNavigateToEditor(newId)
                            }
                        )
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create New Folder Dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf("#00A6CB") }

        val palette = listOf("#00A6CB", "#2563EB", "#059669", "#D97706", "#E11D48", "#7C3AED")

        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") },
                        placeholder = { Text("e.g., Projects") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Color Tag", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        palette.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName.trim(), selectedColor)
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Pinned Tablet Navigation Sidebar Component (260 dp)
 */
@Composable
private fun TabletNavigationSidebar(
    modifier: Modifier = Modifier,
    uiState: DashboardUiState,
    themeMode: ThemeMode,
    isDark: Boolean,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
    onSelectFolder: (String) -> Unit,
    onNewFolderClick: () -> Unit,
    onScannerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val headerTextColor = if (isDark) Color.White else ActionDarkBlue
    val sectionHeaderColor = if (isDark) Color.Gray else Color(0xFF718096)
    val dividerColor = if (isDark) Color(0xFF2E3035) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // App Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryCyanBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Malhodha",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = headerTextColor
                )
            }

            // Library Section
            Text(
                text = "LIBRARY",
                style = MaterialTheme.typography.labelSmall,
                color = sectionHeaderColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            SidebarNavigationItem(
                icon = Icons.Default.Description,
                title = "All Notes",
                count = uiState.allNotesCount,
                isSelected = uiState.selectedFolderId == null && uiState.selectedFilter == LibraryFilter.ALL,
                isDark = isDark,
                onClick = { onSelectFilter(LibraryFilter.ALL) }
            )

            SidebarNavigationItem(
                icon = Icons.Default.Schedule,
                title = "Recents",
                isSelected = uiState.selectedFolderId == null && uiState.selectedFilter == LibraryFilter.RECENTS,
                isDark = isDark,
                onClick = { onSelectFilter(LibraryFilter.RECENTS) }
            )

            SidebarNavigationItem(
                icon = Icons.Default.Star,
                title = "Starred",
                count = uiState.starredCount,
                iconTint = if (uiState.starredCount > 0) Color(0xFFFFB300) else null,
                isSelected = uiState.selectedFolderId == null && uiState.selectedFilter == LibraryFilter.STARRED,
                isDark = isDark,
                onClick = { onSelectFilter(LibraryFilter.STARRED) }
            )

            SidebarNavigationItem(
                icon = Icons.Default.Delete,
                title = "Trash",
                count = uiState.trashCount,
                isSelected = uiState.selectedFolderId == null && uiState.selectedFilter == LibraryFilter.TRASH,
                isDark = isDark,
                onClick = { onSelectFilter(LibraryFilter.TRASH) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = dividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            // User Folders Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOLDERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = sectionHeaderColor,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onNewFolderClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Folder",
                        tint = if (isDark) Color.LightGray else ActionDarkBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.folders, key = { it.id }) { folder ->
                    val folderColor = remember(folder.colorHex) {
                        try { Color(android.graphics.Color.parseColor(folder.colorHex)) } catch (e: Exception) { PrimaryCyanBlue }
                    }
                    val isSelected = uiState.selectedFolderId == folder.id

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) (if (isDark) Color(0xFF2E3035) else Color(0xFFE2E8F0)) else Color.Transparent)
                            .clickable { onSelectFolder(folder.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = folder.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) (if (isDark) Color.White else PrimaryCyanBlue) else (if (isDark) Color(0xFFB0B3B8) else Color(0xFF4A5568)),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Bottom Tools & Theme Mode Segmented Pill
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SidebarNavigationItem(
                icon = Icons.Default.DocumentScanner,
                title = "Document Scanner",
                isSelected = false,
                isDark = isDark,
                onClick = onScannerClick
            )

            SidebarNavigationItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                isSelected = false,
                isDark = isDark,
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3-way Theme Mode Segmented Control
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF282A2E) else Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeSegmentItem(
                        title = "Auto",
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = themeMode == ThemeMode.SYSTEM,
                        isDark = isDark,
                        onClick = { onSelectThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeSegmentItem(
                        title = "Light",
                        icon = Icons.Default.LightMode,
                        isSelected = themeMode == ThemeMode.LIGHT,
                        isDark = isDark,
                        onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeSegmentItem(
                        title = "Dark",
                        icon = Icons.Default.DarkMode,
                        isSelected = themeMode == ThemeMode.DARK,
                        isDark = isDark,
                        onClick = { onSelectThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSegmentItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) {
            if (isDark) Color(0xFF383A40) else Color.White
        } else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) PrimaryCyanBlue else (if (isDark) Color(0xFF9E9E9E) else Color(0xFF718096)),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    if (isDark) Color.White else ActionDarkBlue
                } else (if (isDark) Color(0xFF9E9E9E) else Color(0xFF718096))
            )
        }
    }
}

@Composable
private fun SidebarNavigationItem(
    icon: ImageVector,
    title: String,
    count: Int? = null,
    iconTint: Color? = null,
    isSelected: Boolean,
    isDark: Boolean = true,
    onClick: () -> Unit
) {
    val activeTint = if (isSelected) PrimaryCyanBlue else (iconTint ?: if (isDark) Color(0xFFB0B3B8) else Color(0xFF5A6270))
    val textColor = if (isSelected) (if (isDark) Color.White else PrimaryCyanBlue) else (if (isDark) Color(0xFFB0B3B8) else ActionDarkBlue)
    val bgColor = if (isSelected) PrimaryCyanBlue.copy(alpha = if (isDark) 0.2f else 0.12f) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = activeTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }

        if (count != null && count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF2E3035) else Color(0xFFE2E8F0))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color.LightGray else ActionDarkBlue,
                    fontSize = 11.sp
                )
            }
        }
    }
}
