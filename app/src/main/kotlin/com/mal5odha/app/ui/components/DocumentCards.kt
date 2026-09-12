package com.mal5odha.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.data.models.DocumentType

@Composable
fun DraggableDocumentCard(documentName: String) {
    Card(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = "Draggable: $documentName")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentCard(
    title: String,
    date: String,
    type: DocumentType,
    thumbnailPath: String? = null,
    paperColor: Int? = null,
    pageCount: Int = 1,
    isSelected: Boolean = false,
    isStarred: Boolean = false,
    colorHex: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val accentColor = remember(colorHex) {
        if (!colorHex.isNullOrEmpty()) {
            try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { PrimaryCyanBlue }
        } else PrimaryCyanBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Type Icon + Actions (Star & Three-dot menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (type) {
                            DocumentType.PDF -> Icons.Default.Description
                            DocumentType.FOLDER -> Icons.Default.Folder
                            else -> Icons.Default.Edit
                        },
                        contentDescription = null,
                        tint = accentColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Note Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (type == DocumentType.FOLDER) "Folder" else "$pageCount pgs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
