package com.mal5odha.core.data.providers

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiConfig @Inject constructor() {
    val baseUrl = "https://api.mal5odha.com/v1/"
}

@Singleton
class SharedContentProvider @Inject constructor() {
    fun getSharedFolders(): List<String> = emptyList()
}

@Singleton
class SelectionProvider @Inject constructor() {
    val selectedItemIds = mutableListOf<String>()
    
    fun clearSelection() {
        selectedItemIds.clear()
    }
}

interface HapticFeedbackService {
    fun performLightHaptic()
    fun performHeavyHaptic()
}

@Singleton
class HapticFeedbackServiceImpl @Inject constructor() : HapticFeedbackService {
    override fun performLightHaptic() {
        // TODO: Implement
    }

    override fun performHeavyHaptic() {
        // TODO: Implement
    }
}

interface ThumbnailGeneratorService {
    fun generateThumbnail(documentId: String)
}

@Singleton
class ThumbnailGeneratorServiceImpl @Inject constructor() : ThumbnailGeneratorService {
    override fun generateThumbnail(documentId: String) {
        // TODO: Implement
    }
}
