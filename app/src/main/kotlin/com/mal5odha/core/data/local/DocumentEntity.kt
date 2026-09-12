package com.mal5odha.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val type: String = "NOTE", // "NOTE" or "FOLDER"
    val parentFolderId: String? = null,
    val lastModified: Long,
    val createdAt: Long,
    val coverImage: String? = null,
    val isDeleted: Boolean = false,
    val paperType: String = "BLANK", // "BLANK", "RULED", "GRID", "DOT"
    val paperColor: Int = -1, // white default
    val isStarred: Boolean = false,
    val colorHex: String = "#00A6CB"
)
