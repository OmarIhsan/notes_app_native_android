package com.mal5odha.core.ink.ui

import android.graphics.RectF
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mal5odha.core.pdf.viewport.ViewportState

@Composable
fun InkCanvas(
    modifier: Modifier = Modifier,
    viewportState: ViewportState? = null,
    activePageBounds: RectF? = null,
    currentTool: com.mal5odha.core.ink.models.InkTool = com.mal5odha.core.ink.models.InkTool.PEN,
    initialStrokes: List<com.mal5odha.core.ink.models.Stroke> = emptyList(),
    surfaceRef: (DrawingSurface) -> Unit = {},
    onStrokeDrawn: (com.mal5odha.core.ink.models.Stroke) -> Unit = {},
    onStrokeRemoved: (com.mal5odha.core.ink.models.Stroke) -> Unit = {},
    onStrokeRestored: (com.mal5odha.core.ink.models.Stroke) -> Unit = {},
    onSelectionChanged: (List<com.mal5odha.core.ink.models.Stroke>) -> Unit = {},
    onViewportTransformed: ((scale: Float, offsetX: Float, offsetY: Float) -> Unit)? = null,
    currentColor: Int = android.graphics.Color.BLACK,
    currentWidth: Float = 5f,
    eraserMode: com.mal5odha.core.ink.models.EraserMode = com.mal5odha.core.ink.models.EraserMode.STROKE,
    eraserTarget: com.mal5odha.core.ink.models.EraserTarget = com.mal5odha.core.ink.models.EraserTarget.ALL,
    eraserThickness: Float = 40f,
    layers: List<com.mal5odha.core.ink.models.DocumentLayer> = com.mal5odha.core.ink.models.DocumentLayer.defaultLayers(),
    activeLayerId: String? = null,
    stylusOnlyMode: Boolean = true,
    smartGesturesEnabled: Boolean = true,
    isAudioPlaybackActive: Boolean = false,
    audioPlaybackPositionMs: Long = -1L,
    isReadOnlyMode: Boolean = false,
    onStrokeTapped: ((com.mal5odha.core.ink.models.Stroke) -> Unit)? = null,
    onPinchZoom: ((scaleFactor: Float, focusX: Float, focusY: Float) -> Unit)? = null
) {
    AndroidView(
        factory = { context ->
            DrawingSurface(context).apply {
                if (viewportState != null) {
                    this.viewportState = viewportState
                }
                if (activePageBounds != null) {
                    this.activePageBounds = activePageBounds
                }
                this.isReadOnlyMode = isReadOnlyMode
                this.stylusOnlyMode = stylusOnlyMode
                this.smartGesturesEnabled = smartGesturesEnabled
                this.onStrokeTapped = onStrokeTapped
                this.setAudioPlaybackProgress(isAudioPlaybackActive, audioPlaybackPositionMs)
                this.onPinchZoom = onPinchZoom
                this.onViewportTransformed = onViewportTransformed
                this.onStrokeDrawn = onStrokeDrawn
                this.onStrokeRemoved = onStrokeRemoved
                this.onStrokeRestored = onStrokeRestored
                this.onSelectionChanged = onSelectionChanged
                this.currentStrokeColor = currentColor
                this.currentStrokeWidth = currentWidth
                this.eraserMode = eraserMode
                this.eraserTarget = eraserTarget
                this.eraserThickness = eraserThickness
                this.layers = layers
                this.activeLayerId = activeLayerId
                this.setStrokes(initialStrokes)
                surfaceRef(this)
            }
        },
        update = { surface ->
            surface.reinitializeRenderer()
            if (viewportState != null && surface.viewportState !== viewportState) {
                surface.viewportState = viewportState
            }
            if (activePageBounds != null) {
                surface.activePageBounds = activePageBounds
            }
            surface.isReadOnlyMode = isReadOnlyMode
            surface.stylusOnlyMode = stylusOnlyMode
            surface.smartGesturesEnabled = smartGesturesEnabled
            surface.onStrokeTapped = onStrokeTapped
            surface.setAudioPlaybackProgress(isAudioPlaybackActive, audioPlaybackPositionMs)
            surface.onPinchZoom = onPinchZoom
            surface.onViewportTransformed = onViewportTransformed
            surface.currentTool = currentTool
            surface.currentStrokeColor = currentColor
            surface.currentStrokeWidth = currentWidth
            surface.eraserMode = eraserMode
            surface.eraserTarget = eraserTarget
            surface.eraserThickness = eraserThickness
            surface.layers = layers
            surface.activeLayerId = activeLayerId
            surface.setStrokes(initialStrokes)
        },
        onRelease = { surface ->
            surface.destroy()
        },
        onReset = { surface ->
            surface.cancelActiveGestures()
        },
        modifier = modifier.fillMaxSize()
    )
}
