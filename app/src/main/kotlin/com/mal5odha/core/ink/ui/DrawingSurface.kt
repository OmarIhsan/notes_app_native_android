package com.mal5odha.core.ink.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceView
import android.view.View
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.input.motionprediction.MotionEventPredictor
import com.mal5odha.core.ink.UndoRedoManager
import com.mal5odha.core.ink.math.CatmullRomInterpolator
import com.mal5odha.core.ink.models.InkTool
import com.mal5odha.core.ink.models.Point
import com.mal5odha.core.ink.models.Stroke
import com.mal5odha.core.ink.services.StrokeSelectionService
import com.mal5odha.core.pdf.viewport.ViewportState

/**
 * Primary inking SurfaceView conforming to Goodnotes/Jnotes architecture standards:
 *
 * 1. Page-Local Inking Coordinates:
 *    - Strokes store normalized page-relative coordinates:
 *        xPage = (xScreen - pageBounds.left) / pageBounds.width
 *        yPage = (yScreen - pageBounds.top) / pageBounds.height
 *    - Resolution-independent across diverse tablet / phone displays and PDF export.
 *
 * 2. Active Page Touch Isolation:
 *    - Drawing touches are strictly verified against [activePageBounds].
 *    - Touches initiating outside the target page boundary are rejected.
 *
 * 3. Lifecycle Disposal:
 *    - Explicit [destroy] releases [GLFrontBufferedRenderer] and sets visibility to [View.GONE]
 *      to eliminate any buffer bleed when transitioning screens.
 */
class DrawingSurface(context: Context) :
    SurfaceView(context), CanvasFrontBufferedRenderer.Callback<Stroke>, android.view.SurfaceHolder.Callback {

    // ─── Active Page Bounds (Document Space) ─────────────────────────────────────

    var activePageBounds: RectF = RectF()
        set(value) {
            field = value
            frontBufferRenderer?.commit()
        }

    // ─── Viewport State & Matrix ─────────────────────────────────────────────────

    var viewportState = ViewportState()
        set(value) {
            field = value
            field.onViewportChanged = { scale, offsetX, offsetY ->
                onViewportTransformed?.invoke(scale, offsetX, offsetY)
                frontBufferRenderer?.commit()
            }
        }

    var onViewportTransformed: ((scale: Float, offsetX: Float, offsetY: Float) -> Unit)? = null

    // ─── Renderer & Predictor ────────────────────────────────────────────────────

    private var frontBufferRenderer: CanvasFrontBufferedRenderer<Stroke>? = null
    private val motionPredictor = MotionEventPredictor.newInstance(this)

    // ─── Palm Rejection & Multi-Touch Detectors ──────────────────────────────────

    private var isStylusActive = false
    private var lastStylusTimestamp = 0L
    private val palmTouchMajorThresholdPx = 65f * resources.displayMetrics.density

    private var isMultiTouchPanning = false
    private var lastFocusX = 0f
    private var lastFocusY = 0f

    var stylusOnlyMode: Boolean = true
    var onPinchZoom: ((scaleFactor: Float, focusX: Float, focusY: Float) -> Unit)? = null

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onPinchZoom?.invoke(detector.scaleFactor, detector.focusX, detector.focusY)
                viewportState.zoom(
                    zoomFactor = detector.scaleFactor,
                    focalX = detector.focusX,
                    focalY = detector.focusY
                )
                return true
            }
        }
    )

    // ─── State ───────────────────────────────────────────────────────────────────

    val undoRedoManager = UndoRedoManager()
    private var currentStroke: Stroke? = null

    private var lastSegmentWidth: Float = 5f
    private var activeStrokeDynamicWidth: Float = 5f

    var onSelectionChanged: ((List<Stroke>) -> Unit)? = null
    var onStrokeDrawn: ((Stroke) -> Unit)? = null
    var onStrokeRemoved: ((Stroke) -> Unit)? = null
    var onStrokeRestored: ((Stroke) -> Unit)? = null

    // ─── Tool Configuration ──────────────────────────────────────────────────────

    var currentTool: InkTool = InkTool.PEN
    var currentStrokeColor: Int = Color.BLACK
    var currentStrokeWidth: Float = 5f
    var eraserMode: com.mal5odha.core.ink.models.EraserMode = com.mal5odha.core.ink.models.EraserMode.STROKE
    var eraserTarget: com.mal5odha.core.ink.models.EraserTarget = com.mal5odha.core.ink.models.EraserTarget.ALL
    var eraserThickness: Float = 40f
    var activeLayerId: String? = null
    var layers: List<com.mal5odha.core.ink.models.DocumentLayer> = com.mal5odha.core.ink.models.DocumentLayer.defaultLayers()
    var smartGesturesEnabled: Boolean = true
    var isAudioPlaybackActive: Boolean = false
    var audioPlaybackPositionMs: Long = -1L
    var onStrokeTapped: ((Stroke) -> Unit)? = null
    var isReadOnlyMode: Boolean = false
        set(value) {
            field = value
            if (value) {
                cancelActiveGestures()
            }
        }

    // ─── Rendering Assets (pre-allocated) ────────────────────────────────────────

    private val activePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val predictPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        alpha = 140
    }

    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#42A5F5")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val strokePath = Path()
    private val incrementalPath = Path()
    private val scratchCoords = FloatArray(2)
    private val predictedPath = Path()
    private val selectionPath = Path()

    private val selectedStrokes = mutableListOf<Stroke>()
    private val strokeSelectionService = StrokeSelectionService()
    private val lassoToolPainter = LassoToolPainter()
    private val shapeDetectionService = com.mal5odha.core.ink.services.ShapeDetectionService()
    private val spatialIndex = com.mal5odha.core.ink.spatial.QuadTree(bounds = RectF(0f, 0f, 1f, 1f))

    private val lassoPolygonPoints = mutableListOf<Point>()
    private var activeHandleType: HandleType = HandleType.NONE
    private var selectionCenterPageX = 0f
    private var selectionCenterPageY = 0f
    private var lastTouchNormX = 0f
    private var lastTouchNormY = 0f

    // ─── Hold-to-Snap State ──────────────────────────────────────────────────────
    private var isSnappedToShape = false
    private var snappedStroke: Stroke? = null
    private var recognizedShapeType = com.mal5odha.core.ink.services.RecognizedShapeType.NONE
    private var snapAnchorPoint: Point? = null
    private var snapStationaryTouchNormX = 0f
    private var snapStationaryTouchNormY = 0f
    private val snapHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var snapRunnable: Runnable? = null

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    init {
        isFocusable = true
        setZOrderOnTop(true)
        holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        setBackgroundColor(Color.TRANSPARENT)
        holder.addCallback(this)

        viewportState.onViewportChanged = { scale, offsetX, offsetY ->
            onViewportTransformed?.invoke(scale, offsetX, offsetY)
            frontBufferRenderer?.commit()
        }
    }

    override fun surfaceCreated(holder: android.view.SurfaceHolder) {
        reinitializeRenderer()
    }

    override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (activePageBounds.isEmpty) {
            activePageBounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        }
        reinitializeRenderer()
        try {
            frontBufferRenderer?.commit()
        } catch (e: Exception) {
            android.util.Log.w("DrawingSurface", "Failed to commit frontBufferRenderer in surfaceChanged", e)
        }
    }

    override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
        // Safely cancel gestures and release front-buffered renderer without forcing View.GONE
        // to prevent collapsing the layout bounds during temporary surface recreation
        snapRunnable?.let { snapHandler.removeCallbacks(it) }
        try {
            frontBufferRenderer?.release(true)
        } catch (e: Exception) {
            android.util.Log.w("DrawingSurface", "Error releasing frontBufferRenderer on surfaceDestroyed", e)
        }
        frontBufferRenderer = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        visibility = View.VISIBLE
        reinitializeRenderer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (activePageBounds.isEmpty && w > 0 && h > 0) {
            activePageBounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        }
    }

    /**
     * Re-initializes the front buffer renderer if it was previously released
     * or recycled in a virtualized lazy layout stream.
     */
    fun reinitializeRenderer() {
        visibility = View.VISIBLE
        if (frontBufferRenderer == null && isAttachedToWindow && holder.surface.isValid) {
            try {
                frontBufferRenderer = CanvasFrontBufferedRenderer(this, this)
                post {
                    try {
                        frontBufferRenderer?.commit()
                    } catch (e: Exception) {
                        android.util.Log.w("DrawingSurface", "Failed to commit on reinitializeRenderer", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DrawingSurface", "Failed to instantiate CanvasFrontBufferedRenderer", e)
            }
        }
    }

    /**
     * Resets active inking and gesture state when recycled without tearing down the renderer.
     */
    fun cancelActiveGestures() {
        snapRunnable?.let { snapHandler.removeCallbacks(it) }
        currentStroke = null
        selectedStrokes.clear()
        selectionPath.reset()
    }

    /**
     * Explicit lifecycle release — hides surface and releases GLFrontBufferedRenderer
     * to eliminate ghosting artifacts when navigating away from the editor.
     */
    fun destroy() {
        snapRunnable?.let { snapHandler.removeCallbacks(it) }
        try {
            frontBufferRenderer?.release(true)
        } catch (e: Exception) {
            android.util.Log.w("DrawingSurface", "Error releasing frontBufferRenderer in destroy()", e)
        }
        frontBufferRenderer = null
    }

    // ─── CanvasFrontBufferedRenderer Callbacks ───────────────────────────────────

    override fun onDrawFrontBufferedLayer(
        canvas: Canvas,
        bufferWidth: Int,
        bufferHeight: Int,
        param: Stroke
    ) {
        val bounds = getEffectiveBounds()
        canvas.save()
        // Concatenate Viewport Transformation Matrix M
        canvas.concat(viewportState.getTransformationMatrix())
        // Strictly clip inking to active page bounds so strokes never bleed into page margins
        canvas.clipRect(bounds)

        // If live hold-to-snap is active, render the complete snapped vector shape
        if (isSnappedToShape && snappedStroke != null) {
            val pagePoints = toPagePoints(snappedStroke!!.points, bounds)
            CatmullRomInterpolator.createSmoothPath(pagePoints, strokePath)
            applyPaintForStroke(activePaint, snappedStroke!!, overrideWidth = activeStrokeDynamicWidth, alpha = 255)
            canvas.drawPath(strokePath, activePaint)
            canvas.restore()
            return
        }

        // Incremental rendering: render only the latest curve segment connecting (Pn-2, Pn-1, Pn)
        val points = param.points
        val count = points.size
        if (count >= 2) {
            incrementalPath.reset()
            val p0 = points[maxOf(0, count - 3)]
            val p1 = points[count - 2]
            val p2 = points[count - 1]

            val x0 = bounds.left + p0.x * bounds.width()
            val y0 = bounds.top + p0.y * bounds.height()
            val x1 = bounds.left + p1.x * bounds.width()
            val y1 = bounds.top + p1.y * bounds.height()
            val x2 = bounds.left + p2.x * bounds.width()
            val y2 = bounds.top + p2.y * bounds.height()

            if (count == 2) {
                incrementalPath.moveTo(x1, y1)
                incrementalPath.lineTo(x2, y2)
            } else {
                incrementalPath.moveTo((x0 + x1) / 2f, (y0 + y1) / 2f)
                incrementalPath.quadTo(x1, y1, (x1 + x2) / 2f, (y1 + y2) / 2f)
            }

            applyPaintForStroke(activePaint, param, overrideWidth = activeStrokeDynamicWidth, alpha = 255)
            canvas.drawPath(incrementalPath, activePaint)
        } else if (count == 1) {
            val p0 = points[0]
            val px = bounds.left + p0.x * bounds.width()
            val py = bounds.top + p0.y * bounds.height()
            incrementalPath.reset()
            incrementalPath.moveTo(px, py)
            incrementalPath.lineTo(px + 0.5f, py)
            applyPaintForStroke(activePaint, param, overrideWidth = activeStrokeDynamicWidth, alpha = 255)
            canvas.drawPath(incrementalPath, activePaint)
        }

        // Render predicted ink ghost trail
        if (currentTool != InkTool.LASSO && currentTool != InkTool.ERASER) {
            renderPredictedSegment(canvas, param, bounds)
        }

        canvas.restore()
    }

    override fun onDrawMultiBufferedLayer(
        canvas: Canvas,
        bufferWidth: Int,
        bufferHeight: Int,
        params: Collection<Stroke>
    ) {
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        val bounds = getEffectiveBounds()
        canvas.save()
        // Concatenate Viewport Transformation Matrix M
        canvas.concat(viewportState.getTransformationMatrix())
        // Strictly clip multi-buffered completed strokes to page boundaries
        canvas.clipRect(bounds)

        // Redraw all completed strokes in page space, respecting layer visibility and alpha
        for (stroke in undoRedoManager.history.value) {
            val strokeLayer = layers.find { it.id == stroke.layerId }
            if (strokeLayer != null && !strokeLayer.isVisible) {
                continue
            }
            val layerAlpha = strokeLayer?.alpha ?: 1.0f
            drawSingleStroke(canvas, stroke, bounds, isSelected = selectedStrokes.contains(stroke), layerAlpha = layerAlpha)
        }

        if (currentTool == InkTool.LASSO) {
            lassoToolPainter.drawLassoPath(canvas, selectionPath)
            lassoToolPainter.drawSelectionBounds(canvas, selectionPath, selectedStrokes.isNotEmpty())
        }

        canvas.restore()
    }

    override fun onFrontBufferedLayerRenderComplete(
        frontBufferedLayerSurfaceControl: SurfaceControlCompat,
        transaction: SurfaceControlCompat.Transaction
    ) { /* sync point */ }

    override fun onMultiBufferedLayerRenderComplete(
        frontBufferedLayerSurfaceControl: SurfaceControlCompat,
        multiBufferedLayerSurfaceControl: SurfaceControlCompat,
        transaction: SurfaceControlCompat.Transaction
    ) { /* sync point */ }

    // ─── Hover Event Handling for Proximity Palm Rejection ─────────────────────
    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (isReadOnlyMode) {
            return super.onHoverEvent(event)
        }
        val toolType = event.getToolType(0)
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            isStylusActive = true
            lastStylusTimestamp = System.currentTimeMillis()
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.onHoverEvent(event)
    }

    // ─── Touch Dispatch with Strict Tool Discrimination & Page Bounds Isolation ──

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isReadOnlyMode) {
            parent?.requestDisallowInterceptTouchEvent(false)
            return false
        }

        if (currentTool == InkTool.TEXT) {
            return false
        }

        // Bypass Choreographer VSYNC batching for digitizer-rate hardware sampling (240Hz+)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            requestUnbufferedDispatch(event)
        }

        val pointerCount = event.pointerCount
        val primaryToolType = event.getToolType(0)
        val isStylus = primaryToolType == MotionEvent.TOOL_TYPE_STYLUS || primaryToolType == MotionEvent.TOOL_TYPE_ERASER

        if (isStylus) {
            isStylusActive = true
            lastStylusTimestamp = System.currentTimeMillis()
        }

        // ── 1. Multi-Touch (2+ Pointers) -> Viewport Pan & Zoom ──────────────────
        if (pointerCount >= 2) {
            parent?.requestDisallowInterceptTouchEvent(true)
            if (currentStroke != null) {
                currentStroke = null
                frontBufferRenderer?.commit()
            }

            var sumX = 0f
            var sumY = 0f
            for (i in 0 until pointerCount) {
                sumX += event.getX(i)
                sumY += event.getY(i)
            }
            val focusX = sumX / pointerCount
            val focusY = sumY / pointerCount

            scaleGestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    isMultiTouchPanning = true
                    lastFocusX = focusX
                    lastFocusY = focusY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isMultiTouchPanning) {
                        val dx = focusX - lastFocusX
                        val dy = focusY - lastFocusY
                        viewportState.pan(dx, dy)
                        lastFocusX = focusX
                        lastFocusY = focusY
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (pointerCount <= 2) {
                        isMultiTouchPanning = false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isMultiTouchPanning = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            return true
        }

        // ── 2. Single Pointer Handling ────────────────────────────────────────────
        if (isMultiTouchPanning) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                isMultiTouchPanning = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            return true
        }

        // Single Finger Discrimination & Palm Rejection
        if (!isStylus) {
            val now = System.currentTimeMillis()
            val stylusRecentlyActive = (now - lastStylusTimestamp) < 1500L
            val isLargePalmContact = event.touchMajor > palmTouchMajorThresholdPx

            if (stylusRecentlyActive || isLargePalmContact) {
                // Reject stray palm touches: consume event so it doesn't draw or scroll
                return true
            }

            if (stylusOnlyMode) {
                // In Stylus-Preferred Mode: reject single-finger drawing so parent LazyColumn can scroll smoothly
                parent?.requestDisallowInterceptTouchEvent(false)
                return false
            }
        }

        // Lock parent scroll container when active stylus or finger is drawing
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        val screenX = event.x
        val screenY = event.y
        val pressure = event.pressure.coerceIn(0f, 1f)

        // Transform Screen -> Viewport Document Space (M⁻¹)
        val pageSpacePt = viewportState.screenToPagePoint(screenX, screenY)
        val bounds = getEffectiveBounds()

        // ── 3. Isolate Active Inking to Target Page ──────────────────────────────
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (pageSpacePt.x < bounds.left || pageSpacePt.x > bounds.right ||
                pageSpacePt.y < bounds.top || pageSpacePt.y > bounds.bottom
            ) {
                // Ignore touch starting outside target page bounds
                parent?.requestDisallowInterceptTouchEvent(false)
                return false
            }
        }

        // Compute Normalized Page-Relative Coordinates [0.0 .. 1.0]
        val normX = ((pageSpacePt.x - bounds.left) / bounds.width()).coerceIn(0f, 1f)
        val normY = ((pageSpacePt.y - bounds.top) / bounds.height()).coerceIn(0f, 1f)

        motionPredictor.record(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleActionDown(normX, normY, pressure, bounds)
            MotionEvent.ACTION_MOVE -> handleActionMove(event, normX, normY, pressure, bounds)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handleActionUp(normX, normY, pressure, bounds)
        }

        return true
    }

    private fun performStrokeErasure(normX: Float, normY: Float, bounds: RectF): Boolean {
        val radiusNormX = (eraserThickness / 2f) / bounds.width().coerceAtLeast(1f)
        val radiusNormY = (eraserThickness / 2f) / bounds.height().coerceAtLeast(1f)
        val queryRect = RectF(
            normX - radiusNormX,
            normY - radiusNormY,
            normX + radiusNormX,
            normY + radiusNormY
        )

        val candidates = spatialIndex.query(queryRect)
        if (candidates.isEmpty()) return false

        val touchPageX = bounds.left + normX * bounds.width()
        val touchPageY = bounds.top + normY * bounds.height()
        val pageRadiusSq = (eraserThickness / 2f) * (eraserThickness / 2f)
        val toRemove = mutableListOf<Stroke>()
        val width = bounds.width()
        val height = bounds.height()

        for (stroke in candidates) {
            // Layer and eraser target filtering
            if (eraserTarget == com.mal5odha.core.ink.models.EraserTarget.HIGHLIGHTER_ONLY && stroke.tool != InkTool.HIGHLIGHTER) {
                continue
            }
            if (eraserTarget == com.mal5odha.core.ink.models.EraserTarget.ACTIVE_LAYER_ONLY && stroke.layerId != activeLayerId) {
                continue
            }
            val isLayerLocked = layers.find { it.id == stroke.layerId }?.isLocked == true
            if (isLayerLocked) {
                continue
            }

            val intersects = stroke.points.any { pt ->
                val px = bounds.left + pt.x * width
                val py = bounds.top + pt.y * height
                val dx = px - touchPageX
                val dy = py - touchPageY
                (dx * dx + dy * dy) <= pageRadiusSq
            }
            if (intersects) {
                toRemove.add(stroke)
            }
        }

        if (toRemove.isNotEmpty()) {
            for (stroke in toRemove) {
                undoRedoManager.removeStroke(stroke)
                spatialIndex.remove(stroke.id)
                onStrokeRemoved?.invoke(stroke)
            }
            frontBufferRenderer?.commit()
            return true
        }
        return false
    }

    private fun triggerHoldToSnap(bounds: RectF) {
        val stroke = currentStroke ?: return
        if (stroke.points.size < 4) return
        if (currentTool == InkTool.ERASER || currentTool == InkTool.LASSO || currentTool == InkTool.TEXT) return

        val pageStroke = stroke.copy(points = toPagePoints(stroke.points, bounds).toMutableList())
        val result = shapeDetectionService.detectShape(pageStroke)
        if (result.type != com.mal5odha.core.ink.services.RecognizedShapeType.NONE) {
            isSnappedToShape = true
            recognizedShapeType = result.type
            snapAnchorPoint = result.anchorPoint ?: pageStroke.points.first()
            val normPoints = result.stroke.points.map { p ->
                Point(
                    x = ((p.x - bounds.left) / bounds.width()).coerceIn(0f, 1f),
                    y = ((p.y - bounds.top) / bounds.height()).coerceIn(0f, 1f),
                    pressure = p.pressure
                )
            }.toMutableList()
            snappedStroke = result.stroke.copy(points = normPoints)
            currentStroke = snappedStroke
            try {
                performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
            frontBufferRenderer?.renderFrontBufferedLayer(snappedStroke!!)
        }
    }

    private fun handleActionDown(normX: Float, normY: Float, pressure: Float, bounds: RectF) {
        if (currentTool == InkTool.TEXT) return

        if (isAudioPlaybackActive) {
            val tappedStroke = undoRedoManager.history.value.lastOrNull { s ->
                s.points.any { p -> kotlin.math.hypot(p.x - normX, p.y - normY) < 0.035f }
            }
            if (tappedStroke != null && tappedStroke.audioTimestampMs > 0L) {
                onStrokeTapped?.invoke(tappedStroke)
                try {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Exception) {}
                return
            }
        }

        if (currentTool == InkTool.ERASER && eraserMode == com.mal5odha.core.ink.models.EraserMode.STROKE) {
            performStrokeErasure(normX, normY, bounds)
            return
        }

        if (currentTool == InkTool.LASSO) {
            val pageX = bounds.left + normX * bounds.width()
            val pageY = bounds.top + normY * bounds.height()
            activeHandleType = lassoToolPainter.hitTestHandle(pageX, pageY, selectionPath, selectedStrokes.isNotEmpty())
            if (activeHandleType != HandleType.NONE) {
                val rectF = RectF()
                selectionPath.computeBounds(rectF, true)
                selectionCenterPageX = rectF.centerX()
                selectionCenterPageY = rectF.centerY()
            } else {
                selectedStrokes.clear()
                selectionPath.reset()
                selectionPath.moveTo(pageX, pageY)
                lassoPolygonPoints.clear()
                lassoPolygonPoints.add(Point(normX, normY))
            }
            lastTouchNormX = normX
            lastTouchNormY = normY
            onSelectionChanged?.invoke(selectedStrokes.toList())
            frontBufferRenderer?.commit()
        } else {
            selectedStrokes.clear()
            selectionPath.reset()
            isSnappedToShape = false
            snappedStroke = null
            recognizedShapeType = com.mal5odha.core.ink.services.RecognizedShapeType.NONE
            snapStationaryTouchNormX = normX
            snapStationaryTouchNormY = normY
            snapRunnable?.let { snapHandler.removeCallbacks(it) }
            snapRunnable = Runnable { triggerHoldToSnap(bounds) }
            snapHandler.postDelayed(snapRunnable!!, 450L)

            val strokeLayerId = if (currentTool == InkTool.HIGHLIGHTER) {
                com.mal5odha.core.ink.models.DocumentLayer.LAYER_HIGHLIGHTER_ID
            } else {
                activeLayerId ?: com.mal5odha.core.ink.models.DocumentLayer.LAYER_PEN_ID
            }
            val isCurrentLayerLocked = layers.find { it.id == strokeLayerId }?.isLocked == true
            if (isCurrentLayerLocked) {
                // Ignore touch on locked layer
                return
            }

            lastSegmentWidth = if (currentTool == InkTool.ERASER) eraserThickness else currentStrokeWidth
            val newStroke = Stroke(
                color = currentStrokeColor,
                width = lastSegmentWidth,
                tool = currentTool,
                layerId = strokeLayerId
            )
            // Store point in normalized page-relative coordinates
            newStroke.points.add(Point(normX, normY, pressure))
            currentStroke = newStroke
            frontBufferRenderer?.renderFrontBufferedLayer(newStroke)
        }
    }

    private fun handleActionMove(
        event: MotionEvent,
        normX: Float,
        normY: Float,
        pressure: Float,
        bounds: RectF
    ) {
        if (currentTool == InkTool.ERASER && eraserMode == com.mal5odha.core.ink.models.EraserMode.STROKE) {
            performStrokeErasure(normX, normY, bounds)
            return
        }

        if (currentTool == InkTool.LASSO) {
            val pageX = bounds.left + normX * bounds.width()
            val pageY = bounds.top + normY * bounds.height()
            val dNormX = normX - lastTouchNormX
            val dNormY = normY - lastTouchNormY
            val dx = dNormX * bounds.width()
            val dy = dNormY * bounds.height()

            when (activeHandleType) {
                HandleType.INSIDE -> {
                    selectedStrokes.forEach { stroke ->
                        stroke.points.forEach { pt ->
                            pt.x = (pt.x + dNormX).coerceIn(0f, 1f)
                            pt.y = (pt.y + dNormY).coerceIn(0f, 1f)
                        }
                    }
                    val matrix = android.graphics.Matrix()
                    matrix.postTranslate(dx, dy)
                    selectionPath.transform(matrix)
                    lastTouchNormX = normX
                    lastTouchNormY = normY
                }
                HandleType.TOP_LEFT, HandleType.TOP_RIGHT, HandleType.BOTTOM_LEFT, HandleType.BOTTOM_RIGHT -> {
                    val prevDist = kotlin.math.hypot(
                        bounds.left + lastTouchNormX * bounds.width() - selectionCenterPageX,
                        bounds.top + lastTouchNormY * bounds.height() - selectionCenterPageY
                    ).coerceAtLeast(10f)
                    val newDist = kotlin.math.hypot(pageX - selectionCenterPageX, pageY - selectionCenterPageY).coerceAtLeast(10f)
                    val scaleFactor = (newDist / prevDist).coerceIn(0.2f, 5.0f)

                    val normCenterX = ((selectionCenterPageX - bounds.left) / bounds.width()).coerceIn(0f, 1f)
                    val normCenterY = ((selectionCenterPageY - bounds.top) / bounds.height()).coerceIn(0f, 1f)

                    selectedStrokes.forEach { stroke ->
                        stroke.points.forEach { pt ->
                            pt.x = (normCenterX + (pt.x - normCenterX) * scaleFactor).coerceIn(0f, 1f)
                            pt.y = (normCenterY + (pt.y - normCenterY) * scaleFactor).coerceIn(0f, 1f)
                        }
                    }
                    val matrix = android.graphics.Matrix()
                    matrix.postScale(scaleFactor, scaleFactor, selectionCenterPageX, selectionCenterPageY)
                    selectionPath.transform(matrix)
                    lastTouchNormX = normX
                    lastTouchNormY = normY
                }
                HandleType.ROTATE -> {
                    val prevAngle = kotlin.math.atan2(
                        bounds.top + lastTouchNormY * bounds.height() - selectionCenterPageY,
                        bounds.left + lastTouchNormX * bounds.width() - selectionCenterPageX
                    )
                    val newAngle = kotlin.math.atan2(pageY - selectionCenterPageY, pageX - selectionCenterPageX)
                    val deltaAngleRad = newAngle - prevAngle
                    val deltaAngleDeg = Math.toDegrees(deltaAngleRad.toDouble()).toFloat()

                    val cosA = kotlin.math.cos(deltaAngleRad).toFloat()
                    val sinA = kotlin.math.sin(deltaAngleRad).toFloat()
                    val normCenterX = ((selectionCenterPageX - bounds.left) / bounds.width()).coerceIn(0f, 1f)
                    val normCenterY = ((selectionCenterPageY - bounds.top) / bounds.height()).coerceIn(0f, 1f)

                    selectedStrokes.forEach { stroke ->
                        stroke.points.forEach { pt ->
                            val rx = pt.x - normCenterX
                            val ry = pt.y - normCenterY
                            pt.x = (normCenterX + (rx * cosA - ry * sinA)).coerceIn(0f, 1f)
                            pt.y = (normCenterY + (rx * sinA + ry * cosA)).coerceIn(0f, 1f)
                        }
                    }
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(deltaAngleDeg, selectionCenterPageX, selectionCenterPageY)
                    selectionPath.transform(matrix)
                    lastTouchNormX = normX
                    lastTouchNormY = normY
                }
                HandleType.NONE -> {
                    selectionPath.lineTo(pageX, pageY)
                    lassoPolygonPoints.add(Point(normX, normY))
                }
            }
            frontBufferRenderer?.commit()
        } else {
            val stroke = currentStroke ?: return

            if (isSnappedToShape && snappedStroke != null) {
                // Live shape manipulation before lifting
                val anchor = snapAnchorPoint
                if (anchor != null) {
                    val currentPt = Point(bounds.left + normX * bounds.width(), bounds.top + normY * bounds.height())
                    val pageManipulated = shapeDetectionService.manipulateSnappedShape(
                        original = snappedStroke!!.copy(points = toPagePoints(snappedStroke!!.points, bounds).toMutableList()),
                        type = recognizedShapeType,
                        anchor = anchor,
                        current = currentPt
                    )
                    val normPoints = pageManipulated.points.map { p ->
                        Point(
                            x = ((p.x - bounds.left) / bounds.width()).coerceIn(0f, 1f),
                            y = ((p.y - bounds.top) / bounds.height()).coerceIn(0f, 1f),
                            pressure = p.pressure
                        )
                    }.toMutableList()
                    snappedStroke = pageManipulated.copy(points = normPoints)
                    currentStroke = snappedStroke
                    frontBufferRenderer?.renderFrontBufferedLayer(snappedStroke!!)
                    return
                }
            } else {
                val distPx = kotlin.math.hypot(
                    (normX - snapStationaryTouchNormX) * bounds.width(),
                    (normY - snapStationaryTouchNormY) * bounds.height()
                )
                if (distPx > 8f) {
                    snapStationaryTouchNormX = normX
                    snapStationaryTouchNormY = normY
                    snapRunnable?.let { snapHandler.removeCallbacks(it) }
                    snapRunnable = Runnable { triggerHoldToSnap(bounds) }
                    snapHandler.postDelayed(snapRunnable!!, 450L)
                }
            }

            val width = bounds.width()
            val height = bounds.height()

            // Map historical coalesced events to normalized coordinates without object churn
            for (i in 0 until event.historySize) {
                viewportState.screenToPagePointInPlace(
                    event.getHistoricalX(i),
                    event.getHistoricalY(i),
                    scratchCoords
                )
                val hNormX = ((scratchCoords[0] - bounds.left) / width).coerceIn(0f, 1f)
                val hNormY = ((scratchCoords[1] - bounds.top) / height).coerceIn(0f, 1f)
                val hp = event.getHistoricalPressure(i).coerceIn(0f, 1f)
                val ht = event.getHistoricalEventTime(i)

                val prevPoint = stroke.points.lastOrNull()
                val newPoint = Point(hNormX, hNormY, hp, ht)
                if (prevPoint != null) {
                    val p1x = bounds.left + prevPoint.x * width
                    val p1y = bounds.top + prevPoint.y * height
                    val p2x = bounds.left + newPoint.x * width
                    val p2y = bounds.top + newPoint.y * height
                    lastSegmentWidth = CatmullRomInterpolator.calculateDynamicWidth(
                        baseWidth = currentStrokeWidth,
                        pressure = hp,
                        x1 = p1x,
                        y1 = p1y,
                        t1 = prevPoint.timestamp,
                        x2 = p2x,
                        y2 = p2y,
                        t2 = newPoint.timestamp,
                        previousWidth = lastSegmentWidth
                    )
                }
                stroke.points.add(newPoint)
            }

            // Current event point in normalized coordinates
            val prevPoint = stroke.points.lastOrNull()
            val currPoint = Point(normX, normY, pressure, event.eventTime)
            if (prevPoint != null) {
                val p1x = bounds.left + prevPoint.x * width
                val p1y = bounds.top + prevPoint.y * height
                val p2x = bounds.left + currPoint.x * width
                val p2y = bounds.top + currPoint.y * height
                lastSegmentWidth = CatmullRomInterpolator.calculateDynamicWidth(
                    baseWidth = currentStrokeWidth,
                    pressure = pressure,
                    x1 = p1x,
                    y1 = p1y,
                    t1 = prevPoint.timestamp,
                    x2 = p2x,
                    y2 = p2y,
                    t2 = currPoint.timestamp,
                    previousWidth = lastSegmentWidth
                )
                activeStrokeDynamicWidth = lastSegmentWidth
            }
            stroke.points.add(currPoint)

            frontBufferRenderer?.renderFrontBufferedLayer(stroke)
        }
    }

    private fun handleActionUp(normX: Float, normY: Float, pressure: Float, bounds: RectF) {
        if (currentTool == InkTool.ERASER && eraserMode == com.mal5odha.core.ink.models.EraserMode.STROKE) {
            return
        }

        if (currentTool == InkTool.LASSO) {
            if (activeHandleType != HandleType.NONE) {
                activeHandleType = HandleType.NONE
                selectedStrokes.forEach { s ->
                    onStrokeRemoved?.invoke(s)
                    onStrokeDrawn?.invoke(s)
                    spatialIndex.remove(s.id)
                    s.precomputePathGeometry()
                    spatialIndex.insert(s)
                }
            } else {
                selectionPath.close()
                selectedStrokes.clear()

                // Compute bounding box of lasso polygon in normalized coordinates
                var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
                for (p in lassoPolygonPoints) {
                    if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
                    if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
                }
                val lassoNormBounds = RectF(minX, minY, maxX, maxY)

                // O(log N) spatial index query for candidate strokes inside lasso AABB
                val candidateStrokes = spatialIndex.query(lassoNormBounds)

                // Map candidate strokes to page space for selection hit-testing
                val pageStrokes = candidateStrokes.map { s ->
                    s.copy(points = toPagePoints(s.points, bounds).toMutableList())
                }
                val polygonPagePoints = lassoPolygonPoints.map {
                    Point(bounds.left + it.x * bounds.width(), bounds.top + it.y * bounds.height())
                }
                val selectedPageStrokes = strokeSelectionService.calculateSelectedStrokes(
                    selectionPath,
                    pageStrokes,
                    polygonPagePoints
                )
                val selectedIds = selectedPageStrokes.map { it.id }.toSet()
                selectedStrokes.addAll(candidateStrokes.filter { selectedIds.contains(it.id) })
            }
            onSelectionChanged?.invoke(selectedStrokes.toList())
            frontBufferRenderer?.commit()
        } else {
            snapRunnable?.let { snapHandler.removeCallbacks(it) }

            if (isSnappedToShape && snappedStroke != null) {
                val finalStroke = snappedStroke!!
                finalStroke.precomputePathGeometry()
                undoRedoManager.recordAction(finalStroke)
                spatialIndex.insert(finalStroke)
                onStrokeDrawn?.invoke(finalStroke)
                frontBufferRenderer?.commit()
                isSnappedToShape = false
                snappedStroke = null
                currentStroke = null
                return
            }

            currentStroke?.let { stroke ->
                stroke.points.add(Point(normX, normY, pressure))
                val pagePoints = toPagePoints(stroke.points, bounds)

                if (smartGesturesEnabled && (currentTool == InkTool.PEN || currentTool == InkTool.HIGHLIGHTER)) {
                    // Check 1: Circle/Loop Gesture-to-Select
                    val loopBounds = shapeDetectionService.detectCircleLoopGesture(pagePoints)
                    if (loopBounds != null) {
                        val pageStrokes = undoRedoManager.history.value.map { s ->
                            s.copy(points = toPagePoints(s.points, bounds).toMutableList())
                        }
                        val selectedPageStrokes = strokeSelectionService.calculateSelectedStrokes(
                            selectionPath = Path().apply { addRect(loopBounds, Path.Direction.CW) },
                            allStrokes = pageStrokes,
                            polygonPoints = pagePoints
                        )
                        if (selectedPageStrokes.isNotEmpty()) {
                            selectedStrokes.clear()
                            val selectedIds = selectedPageStrokes.map { it.id }.toSet()
                            selectedStrokes.addAll(undoRedoManager.history.value.filter { selectedIds.contains(it.id) })
                            currentTool = InkTool.LASSO
                            selectionPath.reset()
                            selectionPath.addRect(loopBounds, Path.Direction.CW)
                            onSelectionChanged?.invoke(selectedStrokes.toList())
                            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            currentStroke = null
                            frontBufferRenderer?.commit()
                            return
                        }
                    }

                    // Check 2: Bracket Gesture '[' or ']' to group adjacent items
                    val bracketBounds = shapeDetectionService.detectBracketGesture(pagePoints)
                    if (bracketBounds != null) {
                        val pageStrokes = undoRedoManager.history.value.map { s ->
                            s.copy(points = toPagePoints(s.points, bounds).toMutableList())
                        }
                        val bracketPath = Path().apply { addRect(bracketBounds, Path.Direction.CW) }
                        val selectedPageStrokes = strokeSelectionService.calculateSelectedStrokes(
                            selectionPath = bracketPath,
                            allStrokes = pageStrokes
                        )
                        if (selectedPageStrokes.isNotEmpty()) {
                            selectedStrokes.clear()
                            val selectedIds = selectedPageStrokes.map { it.id }.toSet()
                            selectedStrokes.addAll(undoRedoManager.history.value.filter { selectedIds.contains(it.id) })
                            currentTool = InkTool.LASSO
                            selectionPath.reset()
                            selectionPath.addRect(bracketBounds, Path.Direction.CW)
                            onSelectionChanged?.invoke(selectedStrokes.toList())
                            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            currentStroke = null
                            frontBufferRenderer?.commit()
                            return
                        }
                    }

                    // Check 3: Smart Text Underline & Highlight Snapping
                    val snappedHighlight = shapeDetectionService.detectHorizontalHighlightOrUnderline(
                        stroke.copy(points = pagePoints.toMutableList())
                    )
                    if (snappedHighlight != null) {
                        val normPoints = snappedHighlight.points.map { p ->
                            Point(
                                x = ((p.x - bounds.left) / bounds.width()).coerceIn(0f, 1f),
                                y = ((p.y - bounds.top) / bounds.height()).coerceIn(0f, 1f),
                                pressure = p.pressure,
                                timestamp = p.timestamp
                            )
                        }.toMutableList()
                        val perfectedStroke = stroke.copy(
                            points = normPoints,
                            width = snappedHighlight.width
                        )
                        perfectedStroke.precomputePathGeometry()
                        undoRedoManager.recordAction(perfectedStroke)
                        spatialIndex.insert(perfectedStroke)
                        onStrokeDrawn?.invoke(perfectedStroke)
                        frontBufferRenderer?.commit()
                        currentStroke = null
                        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        return
                    }
                }

                val finalStroke = if (currentTool == InkTool.SHAPE) {
                    val perfected = shapeDetectionService.detectAndPerfectShape(stroke.copy(points = pagePoints.toMutableList()))
                    // Re-normalize perfected shape points back to page space
                    perfected.copy(
                        points = perfected.points.map { p ->
                            Point(
                                x = ((p.x - bounds.left) / bounds.width()).coerceIn(0f, 1f),
                                y = ((p.y - bounds.top) / bounds.height()).coerceIn(0f, 1f),
                                pressure = p.pressure,
                                timestamp = p.timestamp
                            )
                        }.toMutableList()
                    )
                } else {
                    stroke
                }
                finalStroke.precomputePathGeometry()
                undoRedoManager.recordAction(finalStroke)
                spatialIndex.insert(finalStroke)
                onStrokeDrawn?.invoke(finalStroke)
                frontBufferRenderer?.commit()
            }
            currentStroke = null
        }
    }

    // ─── Predicted Ink Ghost Rendering ───────────────────────────────────────────

    private fun renderPredictedSegment(canvas: Canvas, stroke: Stroke, bounds: RectF) {
        val predictedEvent = motionPredictor.predict() ?: return
        val lastPoint = stroke.points.lastOrNull() ?: return
        val lastPageX = bounds.left + lastPoint.x * bounds.width()
        val lastPageY = bounds.top + lastPoint.y * bounds.height()

        predictedPath.reset()
        predictedPath.moveTo(lastPageX, lastPageY)

        for (i in 0 until predictedEvent.historySize) {
            val p = viewportState.screenToPagePoint(
                predictedEvent.getHistoricalX(i),
                predictedEvent.getHistoricalY(i)
            )
            predictedPath.lineTo(p.x, p.y)
        }
        val endPoint = viewportState.screenToPagePoint(predictedEvent.x, predictedEvent.y)
        predictedPath.lineTo(endPoint.x, endPoint.y)

        predictPaint.color = stroke.color
        predictPaint.strokeWidth = stroke.width
        canvas.drawPath(predictedPath, predictPaint)

        predictedEvent.recycle()
    }

    // ─── Stroke Rendering ─────────────────────────────────────────────────────────

    private fun drawSingleStroke(
        canvas: Canvas,
        stroke: Stroke,
        bounds: RectF,
        isSelected: Boolean = false,
        layerAlpha: Float = 1.0f
    ) {
        canvas.save()
        canvas.clipRect(bounds)

        // Visual Karaoke / Ghost Inking for Audio Note Replay
        val baseAlpha = if (isAudioPlaybackActive && stroke.audioSessionId != null) {
            if (stroke.audioTimestampMs > audioPlaybackPositionMs) {
                75 // 30% alpha for future strokes
            } else {
                255 // 100% full illumination for played strokes
            }
        } else {
            255
        }
        val strokeAlpha = (baseAlpha * layerAlpha.coerceIn(0f, 1f)).toInt()

        if (stroke.cachedNormalizedPath == null) {
            stroke.precomputePathGeometry()
        }

        val cachedPath = stroke.cachedNormalizedPath
        if (cachedPath != null) {
            canvas.save()
            canvas.translate(bounds.left, bounds.top)
            canvas.scale(bounds.width(), bounds.height())

            val referenceWidth = 1080f
            val widthScale = if (bounds.width() > 0f) (bounds.width() / referenceWidth).coerceIn(0.2f, 5.0f) else 1.0f
            val scaledWidth = (stroke.width * widthScale).coerceAtLeast(0.5f)
            val normWidth = scaledWidth / bounds.width().coerceAtLeast(1f)

            applyPaintForStroke(activePaint, stroke, overrideWidth = normWidth, alpha = strokeAlpha)
            canvas.drawPath(cachedPath, activePaint)
            canvas.restore()
        } else {
            val pagePoints = toPagePoints(stroke.points, bounds)
            CatmullRomInterpolator.createSmoothPath(pagePoints, strokePath)
            val referenceWidth = 1080f
            val widthScale = if (bounds.width() > 0f) (bounds.width() / referenceWidth).coerceIn(0.2f, 5.0f) else 1.0f
            val scaledWidth = (stroke.width * widthScale).coerceAtLeast(0.5f)
            applyPaintForStroke(activePaint, stroke, overrideWidth = scaledWidth, alpha = strokeAlpha)
            canvas.drawPath(strokePath, activePaint)
        }

        if (isSelected) {
            val normBox = stroke.computeBoundingBox()
            val strokeBounds = RectF(
                bounds.left + normBox.left * bounds.width() - 5f,
                bounds.top + normBox.top * bounds.height() - 5f,
                bounds.left + normBox.right * bounds.width() + 5f,
                bounds.top + normBox.bottom * bounds.height() + 5f
            )
            canvas.drawRect(strokeBounds, selectionPaint)
        }

        canvas.restore()
    }

    private fun applyPaintForStroke(
        paint: Paint,
        stroke: Stroke,
        overrideWidth: Float? = null,
        alpha: Int
    ) {
        paint.color = stroke.color
        paint.strokeWidth = overrideWidth ?: stroke.width

        when (stroke.tool) {
            InkTool.HIGHLIGHTER -> {
                val targetAlpha = if (alpha < 255) (128 * 0.3f).toInt() else 128
                paint.color = androidx.core.graphics.ColorUtils.setAlphaComponent(stroke.color, targetAlpha)
                paint.blendMode = android.graphics.BlendMode.MULTIPLY
            }
            InkTool.ERASER -> {
                paint.alpha = 255
                paint.blendMode = android.graphics.BlendMode.CLEAR
            }
            else -> {
                paint.alpha = alpha
                paint.blendMode = android.graphics.BlendMode.SRC_OVER
            }
        }
    }

    fun setAudioPlaybackProgress(active: Boolean, positionMs: Long) {
        val changed = isAudioPlaybackActive != active || audioPlaybackPositionMs != positionMs
        isAudioPlaybackActive = active
        audioPlaybackPositionMs = positionMs
        if (changed) {
            frontBufferRenderer?.commit()
        }
    }

    // ─── Helper Coordinate Conversions ───────────────────────────────────────────

    private fun getEffectiveBounds(): RectF {
        return if (!activePageBounds.isEmpty) {
            activePageBounds
        } else {
            val w = width.toFloat().coerceAtLeast(1080f)
            val h = height.toFloat().coerceAtLeast(1920f)
            RectF(0f, 0f, w, h)
        }
    }

    private fun toPagePoints(points: List<Point>, bounds: RectF): List<Point> {
        return points.map { p ->
            // If already denormalized (> 1.0) fallback to raw, otherwise denormalize
            val px = if (p.x <= 1.0f && bounds.width() > 0f) bounds.left + p.x * bounds.width() else p.x
            val py = if (p.y <= 1.0f && bounds.height() > 0f) bounds.top + p.y * bounds.height() else p.y
            Point(px, py, p.pressure, p.timestamp)
        }
    }

    private fun rebuildSpatialIndex() {
        spatialIndex.clear()
        for (stroke in undoRedoManager.history.value) {
            spatialIndex.insert(stroke)
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────────

    fun undo() {
        undoRedoManager.undo()?.let { removed ->
            spatialIndex.remove(removed.id)
            onStrokeRemoved?.invoke(removed)
            frontBufferRenderer?.commit()
        }
    }

    fun redo() {
        undoRedoManager.redo()?.let { restored ->
            spatialIndex.insert(restored)
            onStrokeRestored?.invoke(restored)
            frontBufferRenderer?.commit()
        }
    }

    fun setStrokes(newStrokes: List<Stroke>) {
        newStrokes.forEach {
            if (it.cachedNormalizedPath == null) {
                it.precomputePathGeometry()
            }
        }
        undoRedoManager.setInitialState(newStrokes)
        rebuildSpatialIndex()
        selectedStrokes.clear()
        frontBufferRenderer?.commit()
    }

    fun clear() {
        undoRedoManager.clear()
        spatialIndex.clear()
        selectedStrokes.clear()
        frontBufferRenderer?.commit()
    }

    fun deleteSelectedStrokes() {
        if (selectedStrokes.isEmpty()) return
        selectedStrokes.toList().forEach { stroke ->
            undoRedoManager.removeStroke(stroke)
            spatialIndex.remove(stroke.id)
            onStrokeRemoved?.invoke(stroke)
        }
        selectedStrokes.clear()
        selectionPath.reset()
        onSelectionChanged?.invoke(emptyList())
        frontBufferRenderer?.commit()
    }

    fun cutSelectedStrokes() {
        if (selectedStrokes.isEmpty()) return
        com.mal5odha.core.ink.models.StrokeClipboard.copy(selectedStrokes)
        deleteSelectedStrokes()
    }

    fun copySelectedStrokes() {
        if (selectedStrokes.isEmpty()) return
        com.mal5odha.core.ink.models.StrokeClipboard.copy(selectedStrokes)
    }

    fun pasteStrokes() {
        if (!com.mal5odha.core.ink.models.StrokeClipboard.hasContent) return
        val bounds = getEffectiveBounds()
        val normOffset = 30f / bounds.width().coerceAtLeast(1f)
        val pasted = com.mal5odha.core.ink.models.StrokeClipboard.copiedStrokes.map { original ->
            val p = original.copy(
                id = java.util.UUID.randomUUID().toString(),
                points = original.points.map { pt ->
                    pt.copy(
                        x = (pt.x + normOffset).coerceIn(0f, 1f),
                        y = (pt.y + normOffset).coerceIn(0f, 1f)
                    )
                }.toMutableList()
            )
            undoRedoManager.recordAction(p)
            spatialIndex.insert(p)
            onStrokeDrawn?.invoke(p)
            p
        }
        selectedStrokes.clear()
        selectedStrokes.addAll(pasted)
        selectionPath.reset()
        if (pasted.isNotEmpty()) {
            val pPoints = pasted.flatMap { toPagePoints(it.points, bounds) }
            val bbox = com.mal5odha.core.ink.math.CatmullRomInterpolator.calculateBoundingBox(pPoints, 8f)
            selectionPath.addRect(bbox, Path.Direction.CW)
        }
        onSelectionChanged?.invoke(selectedStrokes.toList())
        frontBufferRenderer?.commit()
    }

    fun changeSelectedStrokesColor(newColor: Int) {
        if (selectedStrokes.isEmpty()) return
        val updated = selectedStrokes.map { stroke ->
            onStrokeRemoved?.invoke(stroke)
            val newStroke = stroke.copy(color = newColor)
            undoRedoManager.removeStroke(stroke)
            undoRedoManager.recordAction(newStroke)
            spatialIndex.remove(stroke.id)
            spatialIndex.insert(newStroke)
            onStrokeDrawn?.invoke(newStroke)
            newStroke
        }
        selectedStrokes.clear()
        selectedStrokes.addAll(updated)
        onSelectionChanged?.invoke(selectedStrokes.toList())
        frontBufferRenderer?.commit()
    }

    fun duplicateSelectedStrokes() {
        if (selectedStrokes.isEmpty()) return
        val newStrokes = mutableListOf<Stroke>()
        val bounds = getEffectiveBounds()
        val normOffset = 40f / bounds.width().coerceAtLeast(1f)

        selectedStrokes.forEach { original ->
            val duplicated = original.copy(
                id = java.util.UUID.randomUUID().toString(),
                points = original.points.map {
                    it.copy(
                        x = (it.x + normOffset).coerceIn(0f, 1f),
                        y = (it.y + normOffset).coerceIn(0f, 1f)
                    )
                }.toMutableList()
            )
            undoRedoManager.recordAction(duplicated)
            spatialIndex.insert(duplicated)
            onStrokeDrawn?.invoke(duplicated)
            newStrokes.add(duplicated)
        }
        selectedStrokes.clear()
        selectedStrokes.addAll(newStrokes)
        val matrix = android.graphics.Matrix()
        matrix.postTranslate(40f, 40f)
        selectionPath.transform(matrix)
        onSelectionChanged?.invoke(selectedStrokes.toList())
        frontBufferRenderer?.commit()
    }
}
