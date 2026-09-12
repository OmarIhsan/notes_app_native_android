package com.mal5odha.core.data.di

import com.mal5odha.core.data.services.AppPreferencesService
import com.mal5odha.core.data.services.AppPreferencesServiceImpl
import com.mal5odha.core.data.services.AuthService
import com.mal5odha.core.data.services.AuthServiceImpl
import com.mal5odha.core.data.services.BatchOperationsService
import com.mal5odha.core.data.services.BatchOperationsServiceImpl
import com.mal5odha.core.data.services.DataMigrationService
import com.mal5odha.core.data.services.DataMigrationServiceImpl
import com.mal5odha.core.data.services.DocumentApiService
import com.mal5odha.core.data.services.DocumentApiServiceImpl
import com.mal5odha.core.data.services.DocumentProcessor
import com.mal5odha.core.data.services.DocumentProcessorImpl
import com.mal5odha.core.data.services.DocumentScannerService
import com.mal5odha.core.data.services.DocumentScannerServiceImpl
import com.mal5odha.core.data.services.ErrorHandlingService
import com.mal5odha.core.data.services.ErrorHandlingServiceImpl
import com.mal5odha.core.data.services.PageManagerService
import com.mal5odha.core.data.services.PageManagerServiceImpl
import com.mal5odha.core.data.services.PdfBackgroundService
import com.mal5odha.core.data.services.PdfBackgroundServiceImpl
import com.mal5odha.core.data.services.PdfExportService
import com.mal5odha.core.data.services.PdfExportServiceImpl
import com.mal5odha.core.data.services.SecureStorageService
import com.mal5odha.core.data.services.SecureStorageServiceImpl
import com.mal5odha.core.data.services.SyncService
import com.mal5odha.core.data.services.SyncServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds abstract fun bindPageManagerService(impl: PageManagerServiceImpl): PageManagerService

    @Binds
    abstract fun bindBatchOperationsService(
            impl: BatchOperationsServiceImpl
    ): BatchOperationsService

    @Binds abstract fun bindSyncService(impl: SyncServiceImpl): SyncService

    @Binds abstract fun bindAuthService(impl: AuthServiceImpl): AuthService

    @Binds abstract fun bindDocumentApiService(impl: DocumentApiServiceImpl): DocumentApiService

    @Binds abstract fun bindDocumentProcessor(impl: DocumentProcessorImpl): DocumentProcessor

    @Binds
    abstract fun bindDocumentScannerService(
            impl: DocumentScannerServiceImpl
    ): DocumentScannerService

    @Binds
    abstract fun bindPdfBackgroundService(impl: PdfBackgroundServiceImpl): PdfBackgroundService

    @Binds abstract fun bindPdfExportService(impl: PdfExportServiceImpl): PdfExportService

    @Binds
    abstract fun bindAppPreferencesService(impl: AppPreferencesServiceImpl): AppPreferencesService

    @Binds
    abstract fun bindErrorHandlingService(impl: ErrorHandlingServiceImpl): ErrorHandlingService

    @Binds
    abstract fun bindDataMigrationService(impl: DataMigrationServiceImpl): DataMigrationService

    @Binds
    abstract fun bindSecureStorageService(impl: SecureStorageServiceImpl): SecureStorageService

    @Binds
    abstract fun bindHapticFeedbackService(
            impl: com.mal5odha.core.data.services.HapticFeedbackServiceImpl
    ): com.mal5odha.core.data.services.HapticFeedbackService

    @Binds
    abstract fun bindThumbnailGeneratorService(
            impl: com.mal5odha.core.data.providers.ThumbnailGeneratorServiceImpl
    ): com.mal5odha.core.data.providers.ThumbnailGeneratorService

    @Binds
    abstract fun bindNoteFileSystemRepository(
            impl: com.mal5odha.core.data.repository.NoteFileSystemRepositoryImpl
    ): com.mal5odha.core.data.repository.NoteFileSystemRepository

    @Binds
    abstract fun bindUserPreferencesRepository(
            impl: com.mal5odha.core.data.preferences.UserPreferencesRepositoryImpl
    ): com.mal5odha.core.data.preferences.UserPreferencesRepository
}

