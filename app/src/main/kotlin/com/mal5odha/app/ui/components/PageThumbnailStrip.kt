package com.mal5odha.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.*
import com.mal5odha.core.data.models.DocumentPage

/**
 * Discreet, collapsible thumbnail scrubber and quick-jump pill matching
 * Goodnotes, Notewise, and commercial tablet document viewport standards.
 */
@Composable
fun PageThumbnailStrip(
    pages: List<DocumentPage>,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    onAddPage: (() -> Unit)? = null,
    onOverviewClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Expanded Thumbnail Scrubber Carousel ──────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = PrimaryCyanBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thumbnails",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ActionDarkBlue
                            )
                        }

                        // Collapse Pill Button (Accessible 48dp touch target)
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                tint = SecondaryGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Thumbnail Cards Row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(pages, key = { _, page -> page.id }) { index, _ ->
                            val isSelected = index == currentPageIndex

                            Card(
                                modifier = Modifier
                                    .width(58.dp)
                                    .fillMaxHeight(0.88f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onPageSelected(index) },
                                shape = RoundedCornerShape(6.dp),
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, PrimaryCyanBlue)
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, SecondaryGray.copy(alpha = 0.25f))
                                },
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryCyanBlue.copy(alpha = 0.08f) else PureWhite
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryCyanBlue else SecondaryGray
                                    )
                                }
                            }
                        }

                        if (onAddPage != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .width(58.dp)
                                        .fillMaxHeight(0.88f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onAddPage() },
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        PrimaryCyanBlue.copy(alpha = 0.35f)
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = PrimaryCyanBlue.copy(alpha = 0.05f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Page",
                                            tint = PrimaryCyanBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── Collapsed Quick-Jump Navigation Pill ────────────────────────────────
        AnimatedVisibility(
            visible = !isExpanded,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(
                    0.75.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Page Button (48dp touch target)
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) {
                                onPageSelected(currentPageIndex - 1)
                            }
                        },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Page",
                            tint = if (currentPageIndex > 0) ActionDarkBlue else SecondaryGray.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center Action: Opens Navigation Organizer / Thumbnails
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (onOverviewClicked != null) {
                                    onOverviewClicked()
                                } else {
                                    isExpanded = !isExpanded
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Thumbnails & Organizer",
                            tint = PrimaryCyanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Thumbnails",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ActionDarkBlue
                        )
                    }

                    // Next Page Button (48dp touch target)
                    IconButton(
                        onClick = {
                            if (currentPageIndex < pages.size - 1) {
                                onPageSelected(currentPageIndex + 1)
                            }
                        },
                        enabled = currentPageIndex < pages.size - 1,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Page",
                            tint = if (currentPageIndex < pages.size - 1) ActionDarkBlue else SecondaryGray.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onAddPage != null) {
                        IconButton(
                            onClick = onAddPage,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryCyanBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Quick Add Page",
                                    tint = PrimaryCyanBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
