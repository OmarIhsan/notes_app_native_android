package com.mal5odha.core.data.factory

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified document naming, URI extraction, and filesystem sanitization factory.
 * Enforces decoupling of user-facing display names from internal disk storage UUIDs.
 */
object DocumentNameFactory {

    /**
     * Generates a dynamic default notebook title using the local device timezone:
     * "Note YYYY-MM-DD HH:mm".
     */
    fun defaultNotebookTitle(context: Context? = null): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return "Note ${formatter.format(Date())}"
    }

    /**
     * Derives a clean display title from an imported document's raw display name or URI segment:
     * - Strips file extension
     * - Converts separators ('_', '-') to spaces
     * - Trims leading/trailing whitespace
     * - Falls back to "Imported Document" if blank
     */
    fun fromImportedUri(rawDisplayName: String): String {
        return rawDisplayName
            .substringBeforeLast(".")
            .replace(Regex("[_\\-]+"), " ")
            .trim()
            .ifBlank { "Imported Document" }
    }

    /**
     * Generates a collision-free duplicate document title within the same folder/collection.
     * Pattern: "<Original Title> (Copy)", "<Original Title> (Copy 2)", "<Original Title> (Copy 3)", etc.
     */
    fun forDuplicate(originalTitle: String, existingTitles: Collection<String>): String {
        val baseTitle = originalTitle.replace(Regex(" \\(Copy( ?\\d+)?\\)$"), "").trim()
        var candidate = "$baseTitle (Copy)"
        var counter = 2
        while (existingTitles.contains(candidate)) {
            candidate = "$baseTitle (Copy $counter)"
            counter++
        }
        return candidate
    }

    /**
     * Sanitizes user-facing document titles for external filesystem safety:
     * - Strips illegal characters: \ / : * ? " < > |
     * - Clamps length to 128 characters
     * - Provides an "Untitled_Note" fallback if empty
     */
    fun sanitizeForExport(title: String): String {
        val clean = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return clean.ifBlank { "Untitled_Note" }.take(128)
    }
}
