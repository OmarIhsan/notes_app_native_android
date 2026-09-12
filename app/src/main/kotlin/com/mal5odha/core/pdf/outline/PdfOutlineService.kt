package com.mal5odha.core.pdf.outline

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for extracting the Table of Contents / Outline bookmark hierarchy
 * directly from PDF documents.
 */
@Singleton
class PdfOutlineService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PdfOutlineService"
    }

    /**
     * Extracts outline tree nodes from a PDF file.
     * Returns an empty list if the document has no outline or is a blank notebook.
     */
    suspend fun extractOutline(pdfFile: File): List<DocumentOutlineNode> = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() < 100) return@withContext emptyList()

        try {
            RandomAccessFile(pdfFile, "r").use { raf ->
                val fileLength = raf.length()
                // Read trailer / cross-reference to find Catalog
                val tailSize = minOf(fileLength, 32768L).toInt()
                val tailBytes = ByteArray(tailSize)
                raf.seek(fileLength - tailSize)
                raf.readFully(tailBytes)
                val tailString = String(tailBytes, Charsets.ISO_8859_1)

                // Scan for outlines or bookmark items
                parseOutlineFromPdfStream(raf, tailString)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse PDF outline for ${pdfFile.name}: ${e.message}")
            emptyList()
        }
    }

    private fun parseOutlineFromPdfStream(
        raf: RandomAccessFile,
        tailString: String
    ): List<DocumentOutlineNode> {
        val rootNodes = mutableListOf<DocumentOutlineNode>()

        // Look for Title tokens: /Title (String) or /Title <Hex>
        // and destination: /Dest [pageNum ...] or page reference
        val titlePattern = Regex("""/Title\s*\(([^)]+)\)""")
        val destPattern = Regex("""/Dest\s*\[\s*(\d+)\s+""")

        val fileLength = raf.length()
        val readLength = minOf(fileLength, 1024L * 1024L).toInt() // Scan first 1MB where outlines usually reside
        val buffer = ByteArray(readLength)
        raf.seek(0)
        raf.readFully(buffer)
        val content = String(buffer, Charsets.ISO_8859_1)

        val titleMatches = titlePattern.findAll(content).toList()
        var fallbackPageIndex = 0

        for (match in titleMatches) {
            val rawTitle = match.groupValues[1].trim()
            if (rawTitle.isNotBlank()) {
                // Look for nearby destination
                val matchEnd = match.range.last
                val searchWindow = content.substring(matchEnd, minOf(content.length, matchEnd + 300))
                val destMatch = destPattern.find(searchWindow)
                val targetPage = destMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(0)
                    ?: fallbackPageIndex

                rootNodes.add(
                    DocumentOutlineNode(
                        title = sanitizePdfString(rawTitle),
                        targetPageIndex = targetPage
                    )
                )
                fallbackPageIndex++
            }
        }

        return rootNodes
    }

    private fun sanitizePdfString(raw: String): String {
        return raw.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", " ")
            .replace("\\r", " ")
            .trim()
    }
}
