package com.mal5odha.app.providers

import androidx.core.content.FileProvider
import com.mal5odha.app.R

/**
 * A custom FileProvider to share our internal application files (like exported PDFs)
 * securely with other applications.
 *
 * Configured via AndroidManifest.xml and res/xml/file_paths.xml.
 */
class SharedContentProvider :
        FileProvider(
                R.xml.file_paths // Requires res/xml/file_paths.xml to define the exposed
                // directories
                )
