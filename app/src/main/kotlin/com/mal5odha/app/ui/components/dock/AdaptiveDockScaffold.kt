package com.mal5odha.app.ui.components.dock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 4 Anchored Dock Positions for academic tablet ergonomics:
 * - TopCenter: Default presentation/typing mode (Horizontal)
 * - BottomCenter: Thumb-reachability mode for smaller tablets (11") (Horizontal)
 * - CenterLeft: Non-dominant hand spine for right-handed writers (Vertical)
 * - CenterRight: Non-dominant hand spine for left-handed writers (Vertical)
 */
enum class DockAnchor {
    TopCenter,
    BottomCenter,
    CenterLeft,
    CenterRight
}

enum class DockOrientation {
    Horizontal,
    Vertical
}

val DockAnchor.orientation: DockOrientation
    get() = when (this) {
        DockAnchor.TopCenter, DockAnchor.BottomCenter -> DockOrientation.Horizontal
        DockAnchor.CenterLeft, DockAnchor.CenterRight -> DockOrientation.Vertical
    }

val LateralDockWidth = 56.dp

/**
 * Visual shell for the floating tool dock using Material 3 design tokens:
 * - Elevation: 6.dp
 * - Shape: RoundedCornerShape(24.dp)
 * - Fixed thickness: 56.dp
 * - Row layout when anchored Top/Bottom, Column layout when anchored Left/Right
 */
@Composable
fun ToolCapsule(
    anchor: DockAnchor,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val orientation = anchor.orientation
    val shape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surfaceColor = if (isDark) Color(0xFF1E2124) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

    Surface(
        modifier = modifier
            .then(
                if (orientation == DockOrientation.Horizontal) {
                    Modifier.height(LateralDockWidth).wrapContentWidth()
                } else {
                    Modifier.width(LateralDockWidth).wrapContentHeight()
                }
            ),
        shape = shape,
        color = surfaceColor,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        if (orientation == DockOrientation.Horizontal) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DragHandleIndicator(orientation = DockOrientation.Horizontal)
                content()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DragHandleIndicator(orientation = DockOrientation.Vertical)
                content()
            }
        }
    }
}

@Composable
private fun DragHandleIndicator(orientation: DockOrientation) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val handleColor = if (isDark) Color(0x66FFFFFF) else Color(0x66000000)

    if (orientation == DockOrientation.Horizontal) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(handleColor, CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(4.dp)
                .background(handleColor, CircleShape)
        )
    }
}

@Composable
fun DockDivider(
    orientation: DockOrientation,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dividerColor = if (isDark) Color(0x33FFFFFF) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    if (orientation == DockOrientation.Horizontal) {
        Box(
            modifier = modifier
                .width(1.dp)
                .height(24.dp)
                .background(dividerColor)
        )
    } else {
        Box(
            modifier = modifier
                .height(1.dp)
                .width(24.dp)
                .background(dividerColor)
        )
    }
}

/**
 * AdaptiveDockScaffold wraps the document viewport (DirectPageCanvas) and overlays
 * an ergonomically draggable and magnetically snapping tool dock.
 *
 * Respects WindowInsets.safeDrawing so the dock never collides with system navigation bars
 * or the camera cutout.
 */
@Composable
fun AdaptiveDockScaffold(
    modifier: Modifier = Modifier,
    initialAnchor: DockAnchor = DockAnchor.TopCenter,
    onAnchorChanged: (DockAnchor) -> Unit = {},
    dockContent: @Composable (anchor: DockAnchor, orientation: DockOrientation) -> Unit,
    content: @Composable () -> Unit
) {
    var currentAnchor by remember { mutableStateOf(initialAnchor) }
    var isDragging by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    var dockSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current

        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val leftInsetPx = with(density) { safeInsets.calculateLeftPadding(layoutDirection).toPx() }
        val topInsetPx = with(density) { safeInsets.calculateTopPadding().toPx() }
        val rightInsetPx = with(density) { safeInsets.calculateRightPadding(layoutDirection).toPx() }
        val bottomInsetPx = with(density) { safeInsets.calculateBottomPadding().toPx() }
        val edgeMarginPx = with(density) { 16.dp.toPx() }

        fun calculateTarget(anchor: DockAnchor, dWidth: Float, dHeight: Float): Offset {
            return when (anchor) {
                DockAnchor.TopCenter -> Offset(
                    x = ((containerWidthPx - dWidth) / 2f).coerceAtLeast(leftInsetPx + edgeMarginPx),
                    y = topInsetPx + edgeMarginPx
                )
                DockAnchor.BottomCenter -> Offset(
                    x = ((containerWidthPx - dWidth) / 2f).coerceAtLeast(leftInsetPx + edgeMarginPx),
                    y = (containerHeightPx - bottomInsetPx - dHeight - edgeMarginPx).coerceAtLeast(topInsetPx)
                )
                DockAnchor.CenterLeft -> Offset(
                    x = leftInsetPx + edgeMarginPx,
                    y = ((containerHeightPx - dHeight) / 2f).coerceAtLeast(topInsetPx + edgeMarginPx)
                )
                DockAnchor.CenterRight -> Offset(
                    x = (containerWidthPx - rightInsetPx - dWidth - edgeMarginPx).coerceAtLeast(leftInsetPx),
                    y = ((containerHeightPx - dHeight) / 2f).coerceAtLeast(topInsetPx + edgeMarginPx)
                )
            }
        }

        // Automatic synchronization to target anchor position when screen resizes or anchor changes
        LaunchedEffect(currentAnchor, containerWidthPx, containerHeightPx, dockSize) {
            if (!isDragging && dockSize.width > 0 && dockSize.height > 0 && containerWidthPx > 0 && containerHeightPx > 0) {
                val target = calculateTarget(
                    currentAnchor,
                    dockSize.width.toFloat(),
                    dockSize.height.toFloat()
                )
                if (animOffsetX.value == 0f && animOffsetY.value == 0f) {
                    animOffsetX.snapTo(target.x)
                    animOffsetY.snapTo(target.y)
                } else {
                    launch {
                        animOffsetX.animateTo(
                            target.x,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                    launch {
                        animOffsetY.animateTo(
                            target.y,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
            }
        }

        // 1. Base Layer: The Main Document Viewport / Canvas
        content()

        // 2. Overlay Layer: The Draggable & Snapping Tool Dock
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        animOffsetX.value.roundToInt(),
                        animOffsetY.value.roundToInt()
                    )
                }
                .graphicsLayer {
                    alpha = if (dockSize == IntSize.Zero) 0f else 1f
                }
                .onSizeChanged { newSize ->
                    dockSize = newSize
                }
                .pointerInput(containerWidthPx, containerHeightPx, dockSize) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val curDockW = dockSize.width.toFloat()
                            val curDockH = dockSize.height.toFloat()
                            val minX = leftInsetPx
                            val maxX = (containerWidthPx - rightInsetPx - curDockW).coerceAtLeast(minX)
                            val minY = topInsetPx
                            val maxY = (containerHeightPx - bottomInsetPx - curDockH).coerceAtLeast(minY)

                            val clampedX = (animOffsetX.value + dragAmount.x).coerceIn(minX, maxX)
                            val clampedY = (animOffsetY.value + dragAmount.y).coerceIn(minY, maxY)

                            coroutineScope.launch { animOffsetX.snapTo(clampedX) }
                            coroutineScope.launch { animOffsetY.snapTo(clampedY) }
                        },
                        onDragEnd = {
                            isDragging = false
                            val curDockW = dockSize.width.toFloat().coerceAtLeast(1f)
                            val curDockH = dockSize.height.toFloat().coerceAtLeast(1f)
                            val centerX = animOffsetX.value + curDockW / 2f
                            val centerY = animOffsetY.value + curDockH / 2f

                            val distTop = (centerY - topInsetPx).coerceAtLeast(0f)
                            val distBottom = ((containerHeightPx - bottomInsetPx) - centerY).coerceAtLeast(0f)
                            val distLeft = (centerX - leftInsetPx).coerceAtLeast(0f)
                            val distRight = ((containerWidthPx - rightInsetPx) - centerX).coerceAtLeast(0f)

                            val minDist = minOf(distTop, distBottom, distLeft, distRight)
                            val newAnchor = when (minDist) {
                                distTop -> DockAnchor.TopCenter
                                distBottom -> DockAnchor.BottomCenter
                                distLeft -> DockAnchor.CenterLeft
                                else -> DockAnchor.CenterRight
                            }

                            currentAnchor = newAnchor
                            onAnchorChanged(newAnchor)
                        },
                        onDragCancel = {
                            isDragging = false
                            val target = calculateTarget(
                                currentAnchor,
                                dockSize.width.toFloat(),
                                dockSize.height.toFloat()
                            )
                            coroutineScope.launch {
                                animOffsetX.animateTo(
                                    target.x,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                            }
                            coroutineScope.launch {
                                animOffsetY.animateTo(
                                    target.y,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                            }
                        }
                    )
                }
        ) {
            dockContent(currentAnchor, currentAnchor.orientation)
        }
    }
}
