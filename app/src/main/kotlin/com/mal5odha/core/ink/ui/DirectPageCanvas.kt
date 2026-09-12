package com.mal5odha.core.ink.ui

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.mal5odha.core.ink.math.CatmullRomInterpolator
import com.mal5odha.core.ink.models.AcademicHighlighterTokens
import com.mal5odha.core.ink.models.DocumentLayer
import com.mal5odha.core.ink.models.EraserTarget
import com.mal5odha.core.ink.models.InkTool
import com.mal5odha.core.ink.models.Point
import com.mal5odha.core.ink.models.Stroke
import com.mal5odha.core.ink.services.ShapeDetectionService

/**
 * Direct Document Canvas (Unified Native Drawing Engine):
 *
 * Eliminates the legacy SurfaceView / TextureView overlay paradigm completely.
 * All vector strokes, contrast-safe highlighters (BlendMode.MULTIPLY), and active in-flight inking
 * are rendered directly on the document canvas in a single unified hardware-accelerated pass.
 *
 * When [isReadOnly] is true, pointer consumption is bypassed, streaming all touch gestures directly
 * to viewport scrolling.
 */
@Composable
fun DirectPageCanvas(
    pageId: String,
    pageStrokes: List<Stroke>,
    currentTool: InkTool,
    currentColor: Int,
    currentWidth: Float,
    eraserTarget: EraserTarget = EraserTarget.ALL,
    eraserThickness: Float = 40f,
    isReadOnly: Boolean = false,
    audioPlaybackPositionMs: Long = 0L,
    currentAudioElapsedMs: Long = 0L,
    currentAudioSessionId: String? = null,
    layers: List<DocumentLayer>,
    activeLayerId: String?,
    smartGesturesEnabled: Boolean = true,
    onStrokeDrawn: (Stroke) -> Unit,
    onStrokeRemoved: (String) -> Unit,
    onStrokeTapped: ((Stroke) -> Unit)? = null,
    onSelectionChanged: ((List<Stroke>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inFlightPoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var inFlightTool by remember { mutableStateOf(currentTool) }
    var inFlightColor by remember { mutableIntStateOf(currentColor) }
    var inFlightWidth by remember { mutableFloatStateOf(currentWidth) }

    val shapeDetectionService = remember { ShapeDetectionService() }

    // Pre-allocated Android Paints for native Canvas hardware acceleration
    val penPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    val highlighterPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                blendMode = android.graphics.BlendMode.MULTIPLY
            }
        }
    }

    val pulsePaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = android.graphics.Color.parseColor("#38BDF8")
        }
    }

    val inFlightPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pageId, isReadOnly, currentTool, currentColor, currentWidth, eraserThickness, eraserTarget, audioPlaybackPositionMs) {
                if (isReadOnly) return@pointerInput
                if (currentTool == InkTool.TEXT) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downPos = down.position
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val firstPointerId = down.id

                    if (currentTool == InkTool.ERASER) {
                        down.consume()
                        fun eraseAt(normX: Float, normY: Float) {
                            val radiusNormX = (eraserThickness / 2f) / w
                            val rSq = radiusNormX * radiusNormX

                            for (stroke in pageStrokes) {
                                if (eraserTarget == EraserTarget.HIGHLIGHTER_ONLY && stroke.tool != InkTool.HIGHLIGHTER) continue
                                if (eraserTarget == EraserTarget.ACTIVE_LAYER_ONLY && stroke.layerId != activeLayerId) continue
                                val isLocked = layers.find { it.id == stroke.layerId }?.isLocked == true
                                if (isLocked) continue

                                val hit = stroke.points.any { pt ->
                                    val dx = pt.x - normX
                                    val dy = (pt.y - normY) * (h / w)
                                    (dx * dx + dy * dy) <= rSq
                                }
                                if (hit) {
                                    onStrokeRemoved(stroke.id)
                                }
                            }
                        }

                        eraseAt(down.position.x / w, down.position.y / h)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == firstPointerId } ?: break
                            if (!change.pressed) break
                            change.consume()
                            eraseAt(change.position.x / w, change.position.y / h)
                        }
                    } else if (currentTool == InkTool.LASSO) {
                        // Lasso selection polygon
                        down.consume()
                        val lassoPts = mutableListOf(Point(down.position.x / w, down.position.y / h))
                        inFlightPoints = lassoPts.toList()
                        inFlightTool = InkTool.LASSO

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == firstPointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            lassoPts.add(Point(change.position.x / w, change.position.y / h))
                            inFlightPoints = lassoPts.toList()
                        }

                        // Compute selection
                        if (lassoPts.size >= 3) {
                            val selected = pageStrokes.filter { stroke ->
                                stroke.points.any { pt ->
                                    var inside = false
                                    var j = lassoPts.size - 1
                                    for (i in lassoPts.indices) {
                                        val pi = lassoPts[i]
                                        val pj = lassoPts[j]
                                        if ((pi.y > pt.y) != (pj.y > pt.y) &&
                                            (pt.x < (pj.x - pi.x) * (pt.y - pi.y) / (pj.y - pi.y) + pi.x)
                                        ) {
                                            inside = !inside
                                        }
                                        j = i
                                    }
                                    inside
                                }
                            }
                            onSelectionChanged?.invoke(selected)
                        }
                        inFlightPoints = emptyList()
                    } else {
                        // Inking: PEN, HIGHLIGHTER, SHAPE
                        down.consume()
                        val pts = mutableListOf(Point(down.position.x / w, down.position.y / h, down.pressure))
                        inFlightPoints = pts.toList()
                        inFlightTool = currentTool
                        inFlightColor = currentColor
                        inFlightWidth = currentWidth

                        var isMultiTouch = false
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size > 1) {
                                isMultiTouch = true
                                inFlightPoints = emptyList()
                                break
                            }
                            val change = event.changes.find { it.id == firstPointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            pts.add(Point(change.position.x / w, change.position.y / h, change.pressure))
                            inFlightPoints = pts.toList()
                        }

                        if (!isMultiTouch && pts.isNotEmpty()) {
                            val totalTime = System.currentTimeMillis() - downTime
                            val dx = downPos.x - (pts.last().x * w)
                            val dy = downPos.y - (pts.last().y * h)
                            val distSq = dx * dx + dy * dy
                            val isTap = (pts.size <= 3 && distSq < (28f * 28f) && totalTime < 450L)

                            if (isTap && onStrokeTapped != null) {
                                val normX = downPos.x / w
                                val normY = downPos.y / h
                                val thresholdNormX = 32f / w
                                val rSq = thresholdNormX * thresholdNormX
                                val tappedStroke = pageStrokes.lastOrNull { s ->
                                    val strokeLayer = layers.find { it.id == s.layerId }
                                    if (strokeLayer != null && !strokeLayer.isVisible) return@lastOrNull false
                                    s.points.any { pt ->
                                        val pdx = pt.x - normX
                                        val pdy = (pt.y - normY) * (h / w)
                                        (pdx * pdx + pdy * pdy) <= rSq
                                    }
                                }

                                if (tappedStroke != null) {
                                    onStrokeTapped.invoke(tappedStroke)
                                    inFlightPoints = emptyList()
                                    return@awaitEachGesture
                                }
                            }

                            val strokeLayerId = if (currentTool == InkTool.HIGHLIGHTER) {
                                DocumentLayer.LAYER_HIGHLIGHTER_ID
                            } else {
                                activeLayerId ?: DocumentLayer.LAYER_PEN_ID
                            }

                            val isLayerLocked = layers.find { it.id == strokeLayerId }?.isLocked == true
                            if (!isLayerLocked) {
                                var stroke = Stroke(
                                    color = currentColor,
                                    width = currentWidth,
                                    tool = currentTool,
                                    layerId = strokeLayerId,
                                    audioSessionId = currentAudioSessionId,
                                    audioTimestampMs = currentAudioElapsedMs,
                                    timestampMs = currentAudioElapsedMs,
                                    points = pts
                                )

                                val isShapeTool = currentTool == InkTool.SHAPE_CIRCLE ||
                                        currentTool == InkTool.SHAPE_RECTANGLE ||
                                        currentTool == InkTool.SHAPE_ARROW ||
                                        currentTool == InkTool.SHAPE
                                if (smartGesturesEnabled && isShapeTool) {
                                    stroke = shapeDetectionService.detectShapeForTool(stroke, currentTool)
                                }

                                stroke.precomputePathGeometry()
                                onStrokeDrawn(stroke)
                            }
                        }
                        inFlightPoints = emptyList()
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val widthScale = (canvasWidth / 1080f).coerceIn(0.2f, 5.0f)

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas

            nativeCanvas.save()
            nativeCanvas.scale(canvasWidth, canvasHeight)

            // 1. Render Persisted Strokes
            for (stroke in pageStrokes) {
                val strokeLayer = layers.find { it.id == stroke.layerId }
                if (strokeLayer != null && !strokeLayer.isVisible) continue
                val layerAlpha = strokeLayer?.alpha ?: 1f

                if (stroke.cachedNormalizedPath == null) {
                    stroke.precomputePathGeometry()
                }
                val path = stroke.cachedNormalizedPath ?: continue

                val scaledWidth = (stroke.width * widthScale).coerceAtLeast(1f)
                val normWidth = scaledWidth / canvasWidth

                // Audio-Ink Synchronization: Active playback pulse glow
                val effectiveTime = if (stroke.timestampMs > 0L) stroke.timestampMs else stroke.audioTimestampMs
                val isSyncActive = stroke.isActivelyPlaying ||
                        (audioPlaybackPositionMs > 0L && effectiveTime > 0L &&
                                kotlin.math.abs(effectiveTime - audioPlaybackPositionMs) <= 1200L)

                if (isSyncActive) {
                    pulsePaint.strokeWidth = normWidth * 2.8f
                    pulsePaint.alpha = 160
                    nativeCanvas.drawPath(path, pulsePaint)
                }

                val paint = if (stroke.tool == InkTool.HIGHLIGHTER) {
                    highlighterPaint.apply {
                        strokeWidth = normWidth
                        color = AcademicHighlighterTokens.applyContrastSafeAlpha(stroke.color, layerAlpha)
                    }
                } else {
                    penPaint.apply {
                        strokeWidth = normWidth
                        val base = if (android.graphics.Color.alpha(stroke.color) == 0) (stroke.color and 0x00FFFFFF) or (0xFF shl 24) else stroke.color
                        color = androidx.core.graphics.ColorUtils.setAlphaComponent(base, (255 * layerAlpha).toInt())
                    }
                }

                nativeCanvas.drawPath(path, paint)
            }

            // 2. Render Active In-Flight Inking
            if (inFlightPoints.size >= 2) {
                inFlightPath.reset()
                CatmullRomInterpolator.createSmoothPath(inFlightPoints, inFlightPath)

                val scaledWidth = (inFlightWidth * widthScale).coerceAtLeast(1f)
                val normWidth = scaledWidth / canvasWidth

                val livePaint = if (inFlightTool == InkTool.HIGHLIGHTER) {
                    highlighterPaint.apply {
                        strokeWidth = normWidth
                        color = AcademicHighlighterTokens.applyContrastSafeAlpha(inFlightColor, 1.0f)
                    }
                } else if (inFlightTool == InkTool.LASSO) {
                    penPaint.apply {
                        strokeWidth = 2f / canvasWidth
                        color = android.graphics.Color.parseColor("#42A5F5")
                    }
                } else {
                    penPaint.apply {
                        strokeWidth = normWidth
                        val base = if (android.graphics.Color.alpha(inFlightColor) == 0) (inFlightColor and 0x00FFFFFF) or (0xFF shl 24) else inFlightColor
                        color = androidx.core.graphics.ColorUtils.setAlphaComponent(base, 255)
                    }
                }

                nativeCanvas.drawPath(inFlightPath, livePaint)
            } else if (inFlightPoints.size == 1) {
                val pt = inFlightPoints[0]
                val scaledWidth = (inFlightWidth * widthScale).coerceAtLeast(1f)
                val normRadius = (scaledWidth / 2f) / canvasWidth

                val dotPaint = if (inFlightTool == InkTool.HIGHLIGHTER) {
                    highlighterPaint.apply {
                        style = Paint.Style.FILL
                        color = AcademicHighlighterTokens.applyContrastSafeAlpha(inFlightColor, 1.0f)
                    }
                } else {
                    penPaint.apply {
                        style = Paint.Style.FILL
                        val base = if (android.graphics.Color.alpha(inFlightColor) == 0) (inFlightColor and 0x00FFFFFF) or (0xFF shl 24) else inFlightColor
                        color = androidx.core.graphics.ColorUtils.setAlphaComponent(base, 255)
                    }
                }

                nativeCanvas.drawCircle(pt.x, pt.y, normRadius, dotPaint)
                dotPaint.style = Paint.Style.STROKE
            }

            nativeCanvas.restore()
        }
    }
}
