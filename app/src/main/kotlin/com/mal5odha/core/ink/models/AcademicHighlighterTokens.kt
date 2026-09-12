package com.mal5odha.core.ink.models

import android.graphics.Color

/**
 * 4 Academic Highlighter Tokens calibrated with fixed alpha ranges (38% opacity)
 * and PorterDuff.Mode.MULTIPLY / BlendMode.MULTIPLY.
 *
 * This ensures strokes tint documents and lecture slides without washing out
 * or obscuring the underlying black vector text of clinical PDFs.
 */
object AcademicHighlighterTokens {
    // 38% Opacity ratio: 255 * 0.38f = 97
    const val ALPHA_RATIO: Float = 0.38f
    const val ALPHA_INT: Int = (255 * ALPHA_RATIO).toInt() // 97

    // Semantic tokens
    val ExamAlert = Color.parseColor("#FFE082")
    val KeyTerminology = Color.parseColor("#80E5FF")
    val ClinicalFormulaic = Color.parseColor("#A7F3D0")
    val WarningCaution = Color.parseColor("#FBCFE8")

    val AllTokens = listOf(
        ExamAlert,
        KeyTerminology,
        ClinicalFormulaic,
        WarningCaution
    )

    /**
     * Applies the contrast-safe 38% alpha to any RGB color for highlighter blending.
     */
    fun applyContrastSafeAlpha(color: Int, layerAlpha: Float = 1.0f): Int {
        val base = if (Color.alpha(color) == 0) (color and 0x00FFFFFF) or (0xFF shl 24) else color
        val targetAlpha = (ALPHA_INT * layerAlpha).toInt().coerceIn(20, 130)
        return androidx.core.graphics.ColorUtils.setAlphaComponent(base, targetAlpha)
    }
}
