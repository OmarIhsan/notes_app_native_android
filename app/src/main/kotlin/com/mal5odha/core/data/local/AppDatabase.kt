package com.mal5odha.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DocumentEntity::class,
        SearchIndexEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun searchDao(): SearchDao
}
