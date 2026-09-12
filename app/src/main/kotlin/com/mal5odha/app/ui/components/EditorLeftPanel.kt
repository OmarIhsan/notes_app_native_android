package com.mal5odha.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.data.models.DocumentPage
import com.mal5odha.core.pdf.outline.DocumentOutlineNode

enum class EditorSidebarTab {
    PAGES,
    OUTLINE,
    BOOKMARKS
}

/**
 * In-Editor Collapsible Left Panel (240 dp) hosting:
 * - Page Thumbnails
 * - PDF Document Outlines
 * - Bookmarked Pages
 */
@Composable
fun EditorLeftPanel(
    pages: List<DocumentPage>,
    activePageId: String?,
    outlineTree: List<DocumentOutlineNode>,
    onSelectPage: (Int) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onClosePanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(EditorSidebarTab.PAGES) }

    Surface(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ActionDarkBlue
                )
                IconButton(onClick = onClosePanel, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Collapse Sidebar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Navigation Strip
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryCyanBlue,
                divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
            ) {
                Tab(
                    selected = selectedTab == EditorSidebarTab.PAGES,
                    onClick = { selectedTab = EditorSidebarTab.PAGES },
                    icon = { Icon(Icons.Default.Layers, contentDescription = "Pages", modifier = Modifier.size(18.dp)) },
                    text = { Text("Pages", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )

                Tab(
                    selected = selectedTab == EditorSidebarTab.OUTLINE,
                    onClick = { selectedTab = EditorSidebarTab.OUTLINE },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Outline", modifier = Modifier.size(18.dp)) },
                    text = { Text("Outline", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )

                Tab(
                    selected = selectedTab == EditorSidebarTab.BOOKMARKS,
                    onClick = { selectedTab = EditorSidebarTab.BOOKMARKS },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks", modifier = Modifier.size(18.dp)) },
                    text = { Text("Bookmarks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            // Tab Content
            when (selectedTab) {
                EditorSidebarTab.PAGES -> {
                    PagesThumbnailsList(
                        pages = pages,
                        activePageId = activePageId,
                        onSelectPage = onSelectPage,
                        onToggleBookmark = onToggleBookmark
                    )
                }
                EditorSidebarTab.OUTLINE -> {
                    OutlineTreeList(
                        outlineTree = outlineTree,
                        onSelectPage = onSelectPage
                    )
                }
                EditorSidebarTab.BOOKMARKS -> {
                    BookmarksList(
                        pages = pages,
                        activePageId = activePageId,
                        onSelectPage = onSelectPage,
                        onToggleBookmark = onToggleBookmark
                    )
                }
            }
        }
    }
}

@Composable
private fun PagesThumbnailsList(
    pages: List<DocumentPage>,
    activePageId: String?,
    onSelectPage: (Int) -> Unit,
    onToggleBookmark: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
            val isActive = page.id == activePageId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectPage(index) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) PrimaryCyanBlue.copy(alpha = 0.08f) else Color.White
                ),
                border = BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) PrimaryCyanBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Page Mock Thumbnail / Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFAFAFA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${page.background.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) PrimaryCyanBlue else ActionDarkBlue
                        )

                        IconButton(
                            onClick = { onToggleBookmark(page.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (page.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (page.isBookmarked) Color(0xFFFFB300) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineTreeList(
    outlineTree: List<DocumentOutlineNode>,
    onSelectPage: (Int) -> Unit
) {
    if (outlineTree.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No PDF Outline Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            outlineTree.forEach { node ->
                renderOutlineNode(node, 0, onSelectPage)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderOutlineNode(
    node: DocumentOutlineNode,
    depth: Int,
    onSelectPage: (Int) -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelectPage(node.targetPageIndex) }
                .padding(start = (depth * 14 + 8).dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${node.targetPageIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }

    node.children.forEach { child ->
        renderOutlineNode(child, depth + 1, onSelectPage)
    }
}

@Composable
private fun BookmarksList(
    pages: List<DocumentPage>,
    activePageId: String?,
    onSelectPage: (Int) -> Unit,
    onToggleBookmark: (String) -> Unit
) {
    val bookmarkedPages = remember(pages) {
        pages.mapIndexedNotNull { index, page -> if (page.isBookmarked) Pair(index, page) else null }
    }

    if (bookmarkedPages.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Bookmarks Yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap the bookmark icon on any page card to bookmark it here.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 11.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(bookmarkedPages, key = { it.second.id }) { (index, page) ->
                val isActive = page.id == activePageId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectPage(index) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) PrimaryCyanBlue.copy(alpha = 0.08f) else Color.White
                    ),
                    border = BorderStroke(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) PrimaryCyanBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Page ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { onToggleBookmark(page.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove Bookmark",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
