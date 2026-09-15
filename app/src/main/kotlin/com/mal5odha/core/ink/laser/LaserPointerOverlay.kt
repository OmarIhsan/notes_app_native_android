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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Isolated custom View for rendering transient laser pointer strokes and spotlight reticles.
 *
 * Direct hardware-accelerated Canvas rendering clocked by [postInvalidateOnAnimation] (144Hz on Xiaomi Pad 6),
 * completely bypassing Compose recomposition and database persistence passes.
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
                    trailBuffer.clear()
                    isTouching = false
                    isHovering = false
                }
                postInvalidateOnAnimation()
            }
        }

    var config: LaserConfig = LaserConfig()
        set(value) {
            field = value
            updatePaints()
            postInvalidateOnAnimation()
        }

    var onSecondaryBarrelChanged: ((Boolean) -> Unit)? = null

    private val trailBuffer = LaserTrailBuffer(capacity = 1024)

    // Pre-allocated scratch buffers for zero-allocation rendering passes
    private val scratchCapacity = 1024
    private val scratchXs = FloatArray(scratchCapacity)
    private val scratchYs = FloatArray(scratchCapacity)
    private val scratchTimestamps = LongArray(scratchCapacity)
    private val scratchPressures = FloatArray(scratchCapacity)

    // Coordinate state
    private var pointerX = 0f
    private var pointerY = 0f
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isLaserActive) {
            isTouching = false
            return false
        }

        // Allow multi-touch gestures (e.g. 2-finger zoom/scroll) to pass through without interference
        if (event.pointerCount > 1) {
            parent?.requestDisallowInterceptTouchEvent(false)
            isTouching = false
            return false
        }

        val eventTime = SystemClock.uptimeMillis()
        pointerX = event.x
        pointerY = event.y
        pointerPressure = event.pressure

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isTouching = true
                isHovering = false
                if (config.mode == LaserMode.TRAIL) {
                    trailBuffer.push(event.x, event.y, eventTime, event.pressure)
                }
                postInvalidateOnAnimation()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isTouching = true
                if (config.mode == LaserMode.TRAIL) {
                    // Ingest batched sub-frame historical samples for true 144Hz stylus tracking
                    val historySize = event.historySize
                    for (h in 0 until historySize) {
                        trailBuffer.push(
                            event.getHistoricalX(h),
                            event.getHistoricalY(h),
                            event.getHistoricalEventTime(h),
                            event.getHistoricalPressure(h)
                        )
                    }
                    trailBuffer.push(event.x, event.y, eventTime, event.pressure)
                }
                postInvalidateOnAnimation()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isTouching = false
                if (config.mode == LaserMode.DOT) {
                    // Spotlight dot vanishes immediately on ACTION_UP
                    postInvalidateOnAnimation()
                } else {
                    // In TRAIL mode, trail keeps dissolving progressively
                    postInvalidateOnAnimation()
                }
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

        pointerX = event.x
        pointerY = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                isHovering = true
                postInvalidateOnAnimation()
                return true
            }

            MotionEvent.ACTION_HOVER_EXIT -> {
                isHovering = false
                postInvalidateOnAnimation()
                return true
            }
        }

        return super.onHoverEvent(event)
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
                // 1. Progressive rolling window dissolution
                trailBuffer.pruneExpired(now, config.durationMs)

                // 2. Zero-allocation snapshot of active trail points
                val pointCount = trailBuffer.copyTo(
                    scratchXs,
                    scratchYs,
                    scratchTimestamps,
                    scratchPressures
                )

                // 3. Render Dual-Pass Laser Trail with progressive decay and tapering
                if (pointCount >= 2) {
                    for (i in 0 until pointCount - 1) {
                        val nextIdx = i + 1
                        val age = now - scratchTimestamps[nextIdx]
                        val freshness = (1.0f - (age.toFloat() / config.durationMs)).coerceIn(0f, 1f)
                        val pressure = scratchPressures[nextIdx].coerceIn(0.2f, 1.5f)

                        if (freshness > 0.01f) {
                            val x0 = scratchXs[i]
                            val y0 = scratchYs[i]
                            val x1 = scratchXs[nextIdx]
                            val y1 = scratchYs[nextIdx]

                            // Tapering geometry from head (1.0x) to tail (0.2x)
                            val progressAlongTrail = (nextIdx.toFloat() / pointCount).coerceIn(0f, 1f)
                            val taperFactor = LaserConstants.TRAIL_TAIL_WIDTH_SCALE +
                                    (LaserConstants.TRAIL_HEAD_WIDTH_SCALE - LaserConstants.TRAIL_TAIL_WIDTH_SCALE) * progressAlongTrail

                            // Pass 1: Translucent Outer Aura (Halo)
                            val auraWidth = outerAuraWidthPx * taperFactor * pressure * freshness
                            auraPaint.strokeWidth = auraWidth
                            auraPaint.alpha = (255 * LaserConstants.TRAIL_MAX_AURA_ALPHA * freshness).toInt().coerceIn(0, 255)
                            canvas.drawLine(x0, y0, x1, y1, auraPaint)

                            // Pass 2: High-Intensity Sharp Core
                            val coreWidth = innerCoreWidthPx * taperFactor * pressure * freshness
                            corePaint.strokeWidth = coreWidth
                            corePaint.alpha = (255 * freshness).toInt().coerceIn(0, 255)
                            canvas.drawLine(x0, y0, x1, y1, corePaint)
                        }
                    }
                } else if (pointCount == 1) {
                    val age = now - scratchTimestamps[0]
                    val freshness = (1.0f - (age.toFloat() / config.durationMs)).coerceIn(0f, 1f)
                    if (freshness > 0.01f) {
                        val x = scratchXs[0]
                        val y = scratchYs[0]
                        auraPaint.alpha = (255 * LaserConstants.TRAIL_MAX_AURA_ALPHA * freshness).toInt().coerceIn(0, 255)
                        canvas.drawCircle(x, y, outerAuraWidthPx * 0.5f * freshness, auraPaint)
                        corePaint.alpha = (255 * freshness).toInt().coerceIn(0, 255)
                        canvas.drawCircle(x, y, innerCoreWidthPx * 0.5f * freshness, corePaint)
                    }
                }

                // 4. Continue animation loop while trail is dissolving or touch is active
                if (pointCount > 0 || (isLaserActive && (isTouching || isHovering))) {
                    postInvalidateOnAnimation()
                }
            }

            LaserMode.DOT -> {
                // Spotlight Dot Mode: glowing circular reticle active only while pressed
                if (isLaserActive && isTouching) {
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

    AndroidView(
        factory = { ctx ->
            LaserPointerView(ctx).apply {
                this.isLaserActive = isLaserActive
                this.config = config
                this.onSecondaryBarrelChanged = onSecondaryBarrelChanged
            }
        },
        update = { view ->
            view.isLaserActive = isLaserActive
            view.config = config
            view.onSecondaryBarrelChanged = onSecondaryBarrelChanged
        },
        modifier = modifier.pointerInput(isLaserActive) {
            if (!isLaserActive) return@pointerInput
            // Consume single-touch gestures in compose layer to prevent bubbling,
            // while allowing multi-touch (>1 pointer) to bubble up to viewport gesture detector
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val event = awaitPointerEvent()
                if (event.changes.size == 1) {
                    down.consume()
                }
                while (true) {
                    val nextEvent = awaitPointerEvent()
                    if (nextEvent.changes.size == 1) {
                        nextEvent.changes.forEach { it.consume() }
                    }
                    if (nextEvent.changes.none { it.pressed }) break
                }
            }
        }
    )
}
