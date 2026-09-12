package com.mal5odha.core.data.services

import javax.inject.Inject
import javax.inject.Singleton

interface DocumentProcessor {
    fun processDocument(uri: String)
}

@Singleton
class DocumentProcessorImpl @Inject constructor() : DocumentProcessor {
    override fun processDocument(uri: String) {
        // TODO: Implement OCR/Vision
    }
}

interface DocumentScannerService {
    fun scanDocument()
}

@Singleton
class DocumentScannerServiceImpl @Inject constructor() : DocumentScannerService {
    override fun scanDocument() {
        // TODO: Implement Camera Scan
    }
}

interface PdfBackgroundService {
    fun extractPdfBackground(pdfUri: String)
}

@Singleton
class PdfBackgroundServiceImpl @Inject constructor() : PdfBackgroundService {
    override fun extractPdfBackground(pdfUri: String) {
        // TODO: Implement PDF Extraction
    }
}

interface PdfExportService {
    fun exportToPdf(documentId: String)
}

@Singleton
class PdfExportServiceImpl @Inject constructor(private val realService: com.mal5odha.core.pdf.PdfExportService) : PdfExportService {
    override fun exportToPdf(documentId: String) {
        // This is a legacy bridge. The UI now calls the Coroutine-powered PdfExportService directly.
    }
}
