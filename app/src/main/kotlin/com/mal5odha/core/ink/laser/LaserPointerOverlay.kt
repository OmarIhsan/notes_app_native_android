package com.mal5odha.core.ink.laser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Isolated custom View for rendering transient laser pointer strokes and spotlight reticles.
 *
 * Direct hardware-accelerated Canvas rendering clocked by [postInvalidateOnAnimation] (144Hz on Xiaomi Pad 6),
 * completely bypassing Compose recomposition and database persistence passes.
 *
 * Architecture Features:
 * 1. Segmented Stroke Architecture: Each touch-down to touch-up sequence forms an isolated [LaserStrokeSegment]
 *    to prevent polyline bridging across disconnected gestures.
 * 2. Complete Release Lifecycle: [onPointerUp] and [onPointerCancel] immediately terminate active segments,
 *    clear cached touch coordinates, and dismiss the spotlight reticle in Dot Mode.
 * 3. Continuous Decay Clocking: The rendering loop continues clocking frames until all decaying segments
 *    have completely dissolved, even after touch release.
 */
class LaserPointerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var isLaserActive: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    clearAll()
                }
                postInvalidateOnAnimation()
            }
        }

    var config: LaserConfig = LaserConfig()
        set(value) {
            if (field.mode != value.mode) {
                // Switching modes resets touch state cleanly
                onPointerCancel()
            }
            field = value
            updatePaints()
            postInvalidateOnAnimation()
        }

    var onSecondaryBarrelChanged: ((Boolean) -> Unit)? = null

    // Segmented stroke storage to prevent polyline bridging
    private val strokeSegments = mutableListOf<LaserStrokeSegment>()
    private var currentSegment: LaserStrokeSegment? = null
    private val segmentsLock = Any()

    // Coordinate state (initialized to NaN to prevent stale draws)
    private var pointerX = Float.NaN
    private var pointerY = Float.NaN
    private var pointerPressure = 1f
    private var isTouching = false
    private var isHovering = false

    // Density conversions
    private val density = context.resources.displayMetrics.density
    private val outerAuraWidthPx = LaserConstants.TRAIL_OUTER_AURA_WIDTH_DP * density
    private val innerCoreWidthPx = LaserConstants.TRAIL_INNER_CORE_WIDTH_DP * density
    private val dotAuraRadiusPx = LaserConstants.DOT_AURA_RADIUS_DP * density
    private val dotCoreRadiusPx = LaserConstants.DOT_CORE_RADIUS_DP * density

    // Pre-allocated Paints
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = LaserConstants.TRAIL_CORE_COLOR.toArgb()
    }

    private val dotAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = LaserConstants.TRAIL_CORE_COLOR.toArgb()
    }

    init {
        setWillNotDraw(false)
        isFocusable = true
        isFocusableInTouchMode = true
        updatePaints()
    }

    private fun updatePaints() {
        val colorInt = config.composeColor.toArgb()
        auraPaint.color = colorInt
        dotAuraPaint.color = colorInt
    }

    /**
     * Initiates a new isolated stroke segment.
     */
    fun onPointerDown(x: Float, y: Float, eventTime: Long, pressure: Float = 1.0f) {
        if (!isLaserActive) return
        isTouching = true
        isHovering = false
        pointerX = x
        pointerY = y
        pointerPressure = pressure

        if (config.mode == LaserMode.TRAIL) {
            val segment = LaserStrokeSegment(id = eventTime)
            segment.points.add(LaserPoint(x, y, eventTime, pressure))
            synchronized(segmentsLock) {
                strokeSegments.add(segment)
                currentSegment = segment
            }
        }
        postInvalidateOnAnimation()
    }

    /**
     * Appends move coordinates to the current stroke segment.
     */
    fun onPointerMove(
        x: Float,
        y: Float,
        eventTime: Long,
        pressure: Float = 1.0f,
        historicalPoints: List<LaserPoint> = emptyList()
    ) {
        if (!isLaserActive) return
        isTouching = true
        pointerX = x
        pointerY = y
        pointerPressure = pressure

        if (config.mode == LaserMode.TRAIL) {
            synchronized(segmentsLock) {
                var seg = currentSegment
                if (seg == null) {
                    // Start fresh segment if down was missed or gesture resumed
                    seg = LaserStrokeSegment(id = eventTime)
                    strokeSegments.add(seg)
                    currentSegment = seg
                }

                if (historicalPoints.isNotEmpty()) {
                    for (hp in historicalPoints) {
                        // Deduplicate consecutive identical timestamps/coordinates
                        val last = seg.points.lastOrNull()
                        if (last == null || last.x != hp.x || last.y != hp.y || last.timestampMs != hp.timestampMs) {
                            seg.points.add(hp)
                        }
                    }
                }

                val last = seg.points.lastOrNull()
                if (last == null || last.x != x || last.y != y || last.timestampMs != eventTime) {
                    seg.points.add(LaserPoint(x, y, eventTime, pressure))
                }
            }
        }
        postInvalidateOnAnimation()
    }

    /**
     * Complete release lifecycle on touch up.
     * Disconnects stroke segment and immediately dismisses spotlight reticle in Dot Mode.
     */
    fun onPointerUp() {
        isTouching = false
        pointerX = Float.NaN
        pointerY = Float.NaN
        pointerPressure = 1f
        synchronized(segmentsLock) {
            currentSegment = null
        }
        postInvalidateOnAnimation()
    }

    /**
     * Complete release lifecycle on touch cancel.
     * Disconnects stroke segment and dismisses reticle.
     */
    fun onPointerCancel() {
        isTouching = false
        pointerX = Float.NaN
        pointerY = Float.NaN
        pointerPressure = 1f
        synchronized(segmentsLock) {
            currentSegment = null
        }
        postInvalidateOnAnimation()
    }

    /**
     * Clears all active segments and resets touch state.
     */
    fun clearAll() {
        isTouching = false
        isHovering = false
        pointerX = Float.NaN
        pointerY = Float.NaN
        pointerPressure = 1f
        synchronized(segmentsLock) {
            currentSegment = null
            strokeSegments.clear()
        }
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isLaserActive) {
            onPointerCancel()
            return false
        }

        // Allow multi-touch gestures (e.g. 2-finger zoom/scroll) to pass through without interference
        if (event.pointerCount > 1) {
            parent?.requestDisallowInterceptTouchEvent(false)
            onPointerCancel()
            return false
        }

        val eventTime = SystemClock.uptimeMillis()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                onPointerDown(event.x, event.y, eventTime, event.pressure)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val historyList = if (config.mode == LaserMode.TRAIL && event.historySize > 0) {
                    val list = ArrayList<LaserPoint>(event.historySize)
                    for (h in 0 until event.historySize) {
                        list.add(
                            LaserPoint(
                                event.getHistoricalX(h),
                                event.getHistoricalY(h),
                                event.getHistoricalEventTime(h),
                                event.getHistoricalPressure(h)
                            )
                        )
                    }
                    list
                } else {
                    emptyList()
                }
                onPointerMove(event.x, event.y, eventTime, event.pressure, historyList)
                return true
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onPointerUp()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onPointerCancel()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val isSecondaryPressed = (event.buttonState and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0
        onSecondaryBarrelChanged?.invoke(isSecondaryPressed)

        if (!isLaserActive) {
            isHovering = false
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                isHovering = true
                pointerX = event.x
                pointerY = event.y
                postInvalidateOnAnimation()
                return true
            }

            MotionEvent.ACTION_HOVER_EXIT -> {
                isHovering = false
                if (!isTouching) {
                    pointerX = Float.NaN
                    pointerY = Float.NaN
                }
                postInvalidateOnAnimation()
                return true
            }
        }

        return super.onHoverEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearAll()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            onPointerCancel()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY || keyCode == 309) {
            onSecondaryBarrelChanged?.invoke(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY || keyCode == 309) {
            onSecondaryBarrelChanged?.invoke(false)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = SystemClock.uptimeMillis()

        when (config.mode) {
            LaserMode.TRAIL -> {
                var hasActivePoints = false

                synchronized(segmentsLock) {
                    // 1. Progressive rolling window dissolution per segment
                    val iterator = strokeSegments.iterator()
                    while (iterator.hasNext()) {
                        val segment = iterator.next()
                        // Discard points older than config.durationMs relative to now
                        segment.points.removeAll { pt -> (now - pt.timestampMs) > config.durationMs }

                        // If all points expired and stroke is completed (finger lifted), prune segment
                        if (segment.points.isEmpty() && segment !== currentSegment) {
                            iterator.remove()
                        }
                    }

                    // 2. Render each isolated stroke segment independently (strictly NO BRIDGING)
                    for (segment in strokeSegments) {
                        val points = segment.points
                        val pointCount = points.size
                        if (pointCount > 0) {
                            hasActivePoints = true
                        }

                        if (pointCount >= 2) {
                            for (i in 0 until pointCount - 1) {
                                val nextIdx = i + 1
                                val p0 = points[i]
                                val p1 = points[nextIdx]
                                val age = now - p1.timestampMs
                                val freshness = (1.0f - (age.toFloat() / config.durationMs)).coerceIn(0f, 1f)
                                val pressure = p1.pressure.coerceIn(0.2f, 1.5f)

                                if (freshness > 0.01f) {
                                    // Tapering geometry from head (1.0x) to tail (0.2x) within THIS segment
                                    val progressAlongTrail = (nextIdx.toFloat() / pointCount).coerceIn(0f, 1f)
                                    val taperFactor = LaserConstants.TRAIL_TAIL_WIDTH_SCALE +
                                            (LaserConstants.TRAIL_HEAD_WIDTH_SCALE - LaserConstants.TRAIL_TAIL_WIDTH_SCALE) * progressAlongTrail

                                    // Pass 1: Translucent Outer Aura (Halo)
                                    val auraWidth = outerAuraWidthPx * taperFactor * pressure * freshness
                                    auraPaint.strokeWidth = auraWidth
                                    auraPaint.alpha = (255 * LaserConstants.TRAIL_MAX_AURA_ALPHA * freshness).toInt().coerceIn(0, 255)
                                    canvas.drawLine(p0.x, p0.y, p1.x, p1.y, auraPaint)

                                    // Pass 2: High-Intensity Sharp Core
                                    val coreWidth = innerCoreWidthPx * taperFactor * pressure * freshness
                                    corePaint.strokeWidth = coreWidth
                                    corePaint.alpha = (255 * freshness).toInt().coerceIn(0, 255)
                                    canvas.drawLine(p0.x, p0.y, p1.x, p1.y, corePaint)
                                }
                            }
                        } else if (pointCount == 1) {
                            val p = points[0]
                            val age = now - p.timestampMs
                            val freshness = (1.0f - (age.toFloat() / config.durationMs)).coerceIn(0f, 1f)
                            if (freshness > 0.01f) {
                                auraPaint.alpha = (255 * LaserConstants.TRAIL_MAX_AURA_ALPHA * freshness).toInt().coerceIn(0, 255)
                                canvas.drawCircle(p.x, p.y, outerAuraWidthPx * 0.5f * freshness, auraPaint)
                                corePaint.alpha = (255 * freshness).toInt().coerceIn(0, 255)
                                canvas.drawCircle(p.x, p.y, innerCoreWidthPx * 0.5f * freshness, corePaint)
                            }
                        }
                    }
                }

                // 3. Keep decay clock running until all segments have completely dissolved
                if (hasActivePoints || (isLaserActive && (isTouching || isHovering))) {
                    postInvalidateOnAnimation()
                }
            }

            LaserMode.DOT -> {
                // Spotlight Dot Mode: glowing circular reticle active ONLY while pressed
                if (isLaserActive && isTouching && !pointerX.isNaN() && !pointerY.isNaN()) {
                    val colorInt = config.composeColor.toArgb()

                    // Outer Aura with radial gradient falloff
                    val auraShader = RadialGradient(
                        pointerX,
                        pointerY,
                        dotAuraRadiusPx,
                        intArrayOf(
                            (colorInt and 0x00FFFFFF) or ((0xFF * LaserConstants.DOT_AURA_ALPHA).toInt() shl 24),
                            colorInt and 0x00FFFFFF
                        ),
                        floatArrayOf(0.2f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                    dotAuraPaint.shader = auraShader
                    canvas.drawCircle(pointerX, pointerY, dotAuraRadiusPx, dotAuraPaint)
                    dotAuraPaint.shader = null

                    // Inner bright core dot
                    dotCorePaint.color = LaserConstants.TRAIL_CORE_COLOR.toArgb()
                    canvas.drawCircle(pointerX, pointerY, dotCoreRadiusPx, dotCorePaint)

                    // Accent rim for laser hue
                    auraPaint.strokeWidth = 1.5f * density
                    auraPaint.alpha = 200
                    canvas.drawCircle(pointerX, pointerY, dotCoreRadiusPx, auraPaint)

                    postInvalidateOnAnimation()
                }
            }
        }
    }
}

/**
 * Compose wrapper for [LaserPointerView].
 *
 * Positioned in the document overlay pass directly above the multi-page LazyColumn.
 */
@Composable
fun LaserPointerOverlay(
    isLaserActive: Boolean,
    config: LaserConfig,
    onSecondaryBarrelChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!isLaserActive) return

    var laserViewRef by remember { mutableStateOf<LaserPointerView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            laserViewRef?.clearAll()
        }
    }

    AndroidView(
        factory = { ctx ->
            LaserPointerView(ctx).apply {
                this.isLaserActive = isLaserActive
                this.config = config
                this.onSecondaryBarrelChanged = onSecondaryBarrelChanged
                laserViewRef = this
            }
        },
        update = { view ->
            view.isLaserActive = isLaserActive
            view.config = config
            view.onSecondaryBarrelChanged = onSecondaryBarrelChanged
            laserViewRef = view
        },
        modifier = modifier.pointerInput(isLaserActive) {
            if (!isLaserActive) return@pointerInput
            // Consume single-touch gestures in compose layer to prevent bubbling,
            // while allowing multi-touch (>1 pointer) to bubble up to viewport gesture detector
            awaitEachGesture {
                try {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val event = awaitPointerEvent()
                    if (event.changes.size == 1) {
                        down.consume()
                        val change = event.changes.first()
                        laserViewRef?.onPointerDown(
                            change.position.x,
                            change.position.y,
                            SystemClock.uptimeMillis(),
                            change.pressure
                        )
                    }
                    while (true) {
                        val nextEvent = awaitPointerEvent()
                        if (nextEvent.changes.size == 1) {
                            val change = nextEvent.changes.first()
                            change.consume()
                            if (change.pressed) {
                                laserViewRef?.onPointerMove(
                                    change.position.x,
                                    change.position.y,
                                    SystemClock.uptimeMillis(),
                                    change.pressure
                                )
                            }
                        }
                        if (nextEvent.changes.none { it.pressed }) {
                            laserViewRef?.onPointerUp()
                            break
                        }
                    }
                } finally {
                    laserViewRef?.onPointerCancel()
                }
            }
        }
    )
}
