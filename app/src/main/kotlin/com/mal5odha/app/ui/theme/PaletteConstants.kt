package com.mal5odha.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth palette architecture for all Malhodha inking,
 * annotation, and vector selection tools.
 */
object NotePaletteDefaults {
    val InkingPalette: List<Color> = listOf(
        Color(0xFF1E1E1E), // Pitch Black
        Color(0xFF424242), // Graphite / Dark Gray
        Color(0xFFD32F2F), // Crimson Red
        Color(0xFFE91E63), // Rose Pink
        Color(0xFFF57C00), // Tangerine Orange
        Color(0xFFFBC02D), // Sunflower Yellow
        Color(0xFF388E3C), // Forest Green
        Color(0xFF00897B), // Deep Teal
        Color(0xFF0288D1), // Sky Blue
        Color(0xFF1976D2), // Royal / Primary Blue
        Color(0xFF7B1FA2), // Violet Purple
        Color(0xFF8D6E63)  // Earth Brown
    )

    val InkingPaletteArgb: List<Int> by lazy {
        InkingPalette.map { it.toArgb() }
    }

    // Dynamic recent / custom colors shared across toolbars without divergence
    private val _recentColors = MutableStateFlow<List<Color>>(emptyList())
    val recentColors: StateFlow<List<Color>> = _recentColors.asStateFlow()

    fun recordUsedColor(color: Color) {
        val current = _recentColors.value.toMutableList()
        current.remove(color)
        current.add(0, color)
        if (current.size > 6) {
            _recentColors.value = current.take(6)
        } else {
            _recentColors.value = current
        }
    }

    fun recordUsedColorArgb(colorArgb: Int) {
        recordUsedColor(Color(colorArgb))
    }
}
