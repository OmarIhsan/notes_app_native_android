package com.mal5odha.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mal5odha.app.ui.screens.PageBackgroundPattern
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.data.models.DocumentPage
import com.mal5odha.core.data.models.PageBackground
import com.mal5odha.core.pdf.outline.DocumentOutlineNode

enum class NavigationSheetTab {
    THUMBNAILS,
    OUTLINE,
    BOOKMARKS
}

/**
 * Commercial Tabbed Document Navigation & Page Management Modal.
 * Standard across Goodnotes, Notewise, and Flexcil.
 *
 * Tab 1: Thumbnails Grid (reorder, duplicate, add, delete, change template).
 * Tab 2: Table of Contents / Outline Hierarchy (instant chapter jump).
 * Tab 3: Bookmarks List (user-flagged key study/meeting pages).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationOrganizerSheet(
    pages: List<DocumentPage>,
    currentPageIndex: Int,
    outlineNodes: List<DocumentOutlineNode> = emptyList(),
    onPageSelected: (Int) -> Unit,
    onToggleBookmark: (pageId: String) -> Unit = {},
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onDuplicate: (pageId: String) -> Unit,
    onDelete: (pageId: String) -> Unit,
    onChangeBackground: (pageId: String, newBackground: PageBackground) -> Unit,
    onAddPage: (background: PageBackground) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavigationSheetTab.THUMBNAILS) }
    var pageToDelete by remember { mutableStateOf<DocumentPage?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ─── Header & Tab Row ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = ActionDarkBlue)
                            }
                            Text(
                                text = "Document Navigator",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ActionDarkBlue
                            )
                        }

                        if (currentTab == NavigationSheetTab.THUMBNAILS) {
                            Box {
                                Button(
                                    onClick = { showAddMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Page", style = MaterialTheme.typography.labelMedium)
                                }

                                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                                    PageBackground.values().forEach { bg ->
                                        DropdownMenuItem(
                                            text = { Text(bg.name.replace("_", " ").lowercase().capitalize()) },
                                            onClick = {
                                                onAddPage(bg)
                                                showAddMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = currentTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = PrimaryCyanBlue
                    ) {
                        Tab(
                            selected = currentTab == NavigationSheetTab.THUMBNAILS,
                            onClick = { currentTab = NavigationSheetTab.THUMBNAILS },
                            text = { Text("Thumbnails (${pages.size})") },
                            icon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = currentTab == NavigationSheetTab.OUTLINE,
                            onClick = { currentTab = NavigationSheetTab.OUTLINE },
                            text = { Text("Outline (${outlineNodes.size})") },
                            icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        val bookmarkedCount = pages.count { it.isBookmarked }
                        Tab(
                            selected = currentTab == NavigationSheetTab.BOOKMARKS,
                            onClick = { currentTab = NavigationSheetTab.BOOKMARKS },
                            text = { Text("Bookmarks ($bookmarkedCount)") },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                // ─── Tab Content Views ──────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    when (currentTab) {
                        NavigationSheetTab.THUMBNAILS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                                    val isCurrent = index == currentPageIndex
                                    PageThumbnailCard(
                                        page = page,
                                        pageIndex = index,
                                        isCurrent = isCurrent,
                                        totalCount = pages.size,
                                        onSelect = {
                                            onPageSelected(index)
                                            onDismiss()
                                        },
                                        onToggleBookmark = { onToggleBookmark(page.id) },
                                        onMoveLeft = { if (index > 0) onReorder(index, index - 1) },
                                        onMoveRight = { if (index < pages.size - 1) onReorder(index, index + 1) },
                                        onDuplicate = { onDuplicate(page.id) },
                                        onDelete = { pageToDelete = page },
                                        onChangeBackground = { bg -> onChangeBackground(page.id, bg) }
                                    )
                                }
                            }
                        }
                        NavigationSheetTab.OUTLINE -> {
                            if (outlineNodes.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No Table of Contents in this document", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(outlineNodes) { node ->
                                        OutlineItemRow(
                                            node = node,
                                            depth = 0,
                                            onSelectPage = { pageIdx ->
                                                onPageSelected(pageIdx)
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        NavigationSheetTab.BOOKMARKS -> {
                            val bookmarkedPages = pages.filter { it.isBookmarked }
                            if (bookmarkedPages.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No Bookmarks Yet", style = MaterialTheme.typography.titleMedium, color = ActionDarkBlue, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Bookmark any page via thumbnail or toolbar to quickly access it here.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(bookmarkedPages) { page ->
                                        val pIndex = pages.indexOf(page)
                                        Surface(
                                            onClick = {
                                                onPageSelected(pIndex)
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 2.dp,
                                            border = BorderStroke(1.dp, PrimaryCyanBlue.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryCyanBlue)
                                                    Column {
                                                        Text("Page ${pIndex + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ActionDarkBlue)
                                                        Text("Template: ${page.background.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    }
                                                }
                                                IconButton(onClick = { onToggleBookmark(page.id) }) {
                                                    Icon(Icons.Default.BookmarkRemove, contentDescription = "Remove Bookmark", tint = Color.Red.copy(alpha = 0.7f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Modal
        pageToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { pageToDelete = null },
                title = { Text("Delete Page") },
                text = { Text("Are you sure you want to delete this page? All inking strokes and annotations on it will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete(target.id)
                            pageToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pageToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun OutlineItemRow(
    node: DocumentOutlineNode,
    depth: Int,
    onSelectPage: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { onSelectPage(node.targetPageIndex) },
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 18).dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (node.children.isNotEmpty()) {
                        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = ActionDarkBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Normal,
                        color = ActionDarkBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryCyanBlue.copy(alpha = 0.1f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "p. ${node.targetPageIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyanBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (expanded && node.children.isNotEmpty()) {
            node.children.forEach { child ->
                OutlineItemRow(node = child, depth = depth + 1, onSelectPage = onSelectPage)
            }
        }
    }
}

@Composable
fun PageThumbnailCard(
    page: DocumentPage,
    pageIndex: Int,
    isCurrent: Boolean,
    totalCount: Int,
    onSelect: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onChangeBackground: (PageBackground) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val pageAspectRatio = page.aspectRatio

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(160.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(pageAspectRatio)
                .clickable { onSelect() },
            shape = RoundedCornerShape(8.dp),
            border = if (isCurrent) BorderStroke(2.5.dp, PrimaryCyanBlue) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 6.dp else 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PageBackgroundPattern(type = page.background)

                // Bookmark Badge (48dp touch target)
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.align(Alignment.TopEnd).size(48.dp)
                ) {
                    Icon(
                        imageVector = if (page.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (page.isBookmarked) PrimaryCyanBlue else Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Active Page Badge
                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        color = PrimaryCyanBlue,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${pageIndex + 1}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) PrimaryCyanBlue else ActionDarkBlue,
                modifier = Modifier.padding(start = 4.dp)
            )

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Page Options", modifier = Modifier.size(20.dp))
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (pageIndex > 0) {
                        DropdownMenuItem(
                            text = { Text("Move Backward") },
                            leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                            onClick = {
                                onMoveLeft()
                                showMenu = false
                            }
                        )
                    }
                    if (pageIndex < totalCount - 1) {
                        DropdownMenuItem(
                            text = { Text("Move Forward") },
                            leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
                            onClick = {
                                onMoveRight()
                                showMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Duplicate Page") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            onDuplicate()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (page.isBookmarked) "Remove Bookmark" else "Bookmark Page") },
                        leadingIcon = { Icon(if (page.isBookmarked) Icons.Default.BookmarkRemove else Icons.Default.Bookmark, contentDescription = null) },
                        onClick = {
                            onToggleBookmark()
                            showMenu = false
                        }
                    )
                    if (totalCount > 1) {
                        DropdownMenuItem(
                            text = { Text("Delete Page", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
