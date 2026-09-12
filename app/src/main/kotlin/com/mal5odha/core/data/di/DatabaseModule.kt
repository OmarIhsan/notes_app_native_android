package com.mal5odha.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mal5odha.core.data.local.AppDatabase
import com.mal5odha.core.data.local.DocumentDao
import com.mal5odha.core.data.local.SearchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE VIRTUAL TABLE IF NOT EXISTS `document_search_index` 
                USING FTS4(
                    documentId TEXT NOT NULL,
                    pageIndex INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    sourceType TEXT NOT NULL,
                    boundsLeft REAL NOT NULL,
                    boundsTop REAL NOT NULL,
                    boundsRight REAL NOT NULL,
                    boundsBottom REAL NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `documents` ADD COLUMN `isStarred` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `documents` ADD COLUMN `colorHex` TEXT NOT NULL DEFAULT '#00A6CB'")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mal5odha-db"
        )
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        // High speed I/O logging is essential for saving ink strokes seamlessly
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
    }

    @Provides
    fun provideDocumentDao(database: AppDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    fun provideSearchDao(database: AppDatabase): SearchDao {
        return database.searchDao()
    }

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }
}
