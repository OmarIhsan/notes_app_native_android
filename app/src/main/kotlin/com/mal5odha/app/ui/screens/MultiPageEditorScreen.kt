package com.mal5odha.app.ui.screens

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import com.mal5odha.core.pdf.viewport.ZoomTransformSolver
import com.mal5odha.core.pdf.viewport.TwoFingerGestureMode
import com.mal5odha.core.pdf.viewport.TwoFingerGestureArbitrator
import com.mal5odha.core.pdf.viewport.TwoFingerIntentGestureDetector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mal5odha.core.data.preferences.ThemeMode
import com.mal5odha.app.ui.components.dock.AdaptiveDockScaffold
import com.mal5odha.app.ui.components.dock.DockAnchor
import com.mal5odha.app.ui.components.dock.DockDivider
import com.mal5odha.app.ui.components.dock.DockOrientation
import com.mal5odha.app.ui.components.dock.ToolCapsule
import com.mal5odha.app.ui.components.dock.ZeroTapColorWell
import com.mal5odha.app.ui.components.AudioRecordingPill
import com.mal5odha.app.ui.components.AudioSyncScrubber
import com.mal5odha.app.ui.components.LassoActionPill
import com.mal5odha.app.ui.components.PageThumbnailStrip
import com.mal5odha.app.ui.components.StrokeSelectionToolbar
import com.mal5odha.app.ui.components.LayerManagementSheet
import com.mal5odha.app.ui.components.RevisionHistorySheet
import com.mal5odha.app.ui.components.EditorLeftPanel
import com.mal5odha.app.ui.components.NoteOptionsBottomSheet
import com.mal5odha.app.ui.components.RenameNoteDialog
import com.mal5odha.app.ui.components.MoveToFolderDialog
import com.mal5odha.app.ui.components.NoteDetailsDialog
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.app.ui.theme.SecondaryGray
import com.mal5odha.core.data.models.DocumentPage
import com.mal5odha.core.data.models.PageBackground
import com.mal5odha.core.ink.models.DocumentLayer
import com.mal5odha.core.ink.models.EraserTarget
import com.mal5odha.core.ink.models.InkTool
import com.mal5odha.core.ink.ui.DirectPageCanvas
import com.mal5odha.core.pdf.viewport.ViewportState
import com.mal5odha.core.ink.laser.LaserConfig
import com.mal5odha.core.ink.laser.LaserConstants
import com.mal5odha.core.ink.laser.LaserMode
import com.mal5odha.core.ink.laser.LaserPointerOverlay
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Continuous document viewport engine matching Goodnotes and Jnotes standards:
 *
 * 1. Virtualized Continuous Scroll:
 *    - Renders pages inside a continuous LazyColumn stream.
 *    - Standardized page cards with drop shadows, page margins, and A4 aspect ratios.
 *
 * 2. Page-Local Inking & Touch Tool Discrimination:
 *    - Each page encapsulates its own local coordinate system [0.0..1.0].
 *    - Stylus drawing events are locked to the active page under the pen tip.
 *
 * 3. Dynamic Page Management:
 *    - Add new pages dynamically in the document stream.
 *    - Smooth auto-scrolling to selected thumbnail pages.
 */
@Composable
fun MultiPageEditorScreen(
    documentId: String?,
    onNavigateBack: () -> Unit = {},
    viewModel: MultiPageEditorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentThemeMode by settingsViewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (currentThemeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val capsuleIconTint = if (isDarkTheme) Color(0xFFE2E8F0) else ActionDarkBlue
    val pageStrokes by viewModel.pageStrokes.collectAsState()
    val pageTextAnnotations by viewModel.pageTextAnnotations.collectAsState()
    val pageMediaAnnotations by viewModel.pageMediaAnnotations.collectAsState()
    val audioSyncState by viewModel.audioSyncState.collectAsState()
    val audioPlaybackPositionMs by viewModel.audioPlaybackPositionMs.collectAsState()
    val isReadOnly by viewModel.isReadOnly.collectAsState()
    val isLeftPanelOpen by viewModel.isLeftPanelOpen.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val searchUiState by viewModel.searchUiState.collectAsState()
    val outlineTree by viewModel.outlineTree.collectAsState()
    val layers by viewModel.layers.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    val eraserTarget by viewModel.eraserTarget.collectAsState()
    val isDualPageSpread by viewModel.isDualPageSpread.collectAsState()
    val mutations by viewModel.mutations.collectAsState()

    var currentTool by remember { mutableStateOf(InkTool.PEN) }
    var laserConfig by remember { mutableStateOf(LaserConfig()) }
    var showLaserOptionsPopup by remember { mutableStateOf(false) }

    var selectedStrokes by remember { mutableStateOf<List<com.mal5odha.core.ink.models.Stroke>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(android.graphics.Color.BLACK) }
    var selectedWidth by remember { mutableStateOf(5f) }
    var eraserMode by remember { mutableStateOf(com.mal5odha.core.ink.models.EraserMode.STROKE) }
    var eraserThickness by remember { mutableFloatStateOf(40f) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showLayerSheet by remember { mutableStateOf(false) }
    var showRevisionSheet by remember { mutableStateOf(false) }
    var showNoteOptionsSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var noteDetailsInfo by remember { mutableStateOf<Pair<Long, Int>?>(null) }

    var documentZoomScale by remember { mutableFloatStateOf(1f) }
    var zoomState by remember { mutableStateOf(ZoomTransformSolver.ZoomState()) }
    var smartGesturesEnabled by remember { mutableStateOf(true) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // ─── Active Visible Page Index Tracking ──────────────────────────────────────
    val currentVisiblePageIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }?.index ?: visibleItems.first().index
            }
        }
    }

    val pages = uiState.document?.pages ?: emptyList()
    val totalPagesCount = pages.size.coerceAtLeast(1)
    val activePage = pages.getOrNull(currentVisiblePageIndex.coerceIn(0, totalPagesCount - 1))
    val activePageId = activePage?.id

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && activePageId != null) {
            viewModel.addImageAnnotation(activePageId, uri)
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioRecording()
        }
    }

    // Primary load trigger
    LaunchedEffect(documentId) {
        if (documentId != null) {
            viewModel.loadDocument(documentId)
        }
    }

    // Lifecycle cleanup on exit & thumbnail regeneration
    DisposableEffect(Unit) {
        onDispose {
            viewModel.refreshThumbnail()
        }
    }

    val paperColor = remember(uiState.document?.paperColor, isDarkTheme) {
        uiState.document?.paperColor?.let { Color(it) } ?: (if (isDarkTheme) Color(0xFF141416) else Color(0xFFF5F5F7))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        AdaptiveDockScaffold(
            initialAnchor = DockAnchor.TopCenter,
            dockContent = { anchor, orientation ->
                ToolCapsule(anchor = anchor) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = capsuleIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // In-Editor Sidebar Toggle (Thumbnails, Outline, Bookmarks)
                    IconButton(
                        onClick = { viewModel.toggleLeftPanel() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewSidebar,
                            contentDescription = "Toggle Sidebar",
                            tint = if (isLeftPanelOpen) PrimaryCyanBlue else capsuleIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DockDivider(orientation = orientation)

                    // Binary View / Edit Mode Toggle (AutoStories icon for Edit Mode, Eye icon for Reading Mode)
                    IconButton(
                        onClick = { viewModel.toggleReadOnly() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isReadOnly) Icons.Default.Visibility else Icons.Default.AutoStories,
                            contentDescription = if (isReadOnly) "Reading Mode (Tap to Edit)" else "Edit Mode (Tap to Read-Only)",
                            tint = if (isReadOnly) PrimaryCyanBlue else capsuleIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DockDivider(orientation = orientation)

                    // Read-Only Mode Navigation Controls: Page Jump & Bookmark Toggle
                    if (isReadOnly) {
                        IconButton(
                            onClick = {
                                if (currentVisiblePageIndex > 0) {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(currentVisiblePageIndex - 1)
                                    }
                                }
                            },
                            enabled = currentVisiblePageIndex > 0,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Page",
                                tint = if (currentVisiblePageIndex > 0) capsuleIconTint else capsuleIconTint.copy(alpha = 0.38f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF2C2F33) else Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            val pageLabel = if (orientation == DockOrientation.Horizontal) {
                                "${currentVisiblePageIndex + 1} / $totalPagesCount"
                            } else {
                                "${currentVisiblePageIndex + 1}"
                            }
                            Text(
                                text = pageLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = capsuleIconTint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentVisiblePageIndex < totalPagesCount - 1) {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(currentVisiblePageIndex + 1)
                                    }
                                }
                            },
                            enabled = currentVisiblePageIndex < totalPagesCount - 1,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Page",
                                tint = if (currentVisiblePageIndex < totalPagesCount - 1) capsuleIconTint else capsuleIconTint.copy(alpha = 0.38f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DockDivider(orientation = orientation)

                        IconButton(
                            onClick = {
                                activePageId?.let { viewModel.toggleBookmark(it) }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            val isBookmarked = activePage?.isBookmarked == true
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Page",
                                tint = if (isBookmarked) Color(0xFFFFA000) else capsuleIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DockDivider(orientation = orientation)
                    }

                    if (!isReadOnly) {
                        IconButton(
                            onClick = {
                                currentTool = InkTool.PEN
                                if (selectedColor == android.graphics.Color.parseColor("#FFF176")) {
                                    selectedColor = android.graphics.Color.BLACK
                                    selectedWidth = 5f
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Pen",
                                tint = if (currentTool == InkTool.PEN) PrimaryCyanBlue else capsuleIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                currentTool = InkTool.HIGHLIGHTER
                                if (selectedColor == android.graphics.Color.BLACK) {
                                    selectedColor = android.graphics.Color.parseColor("#FFE082")
                                    selectedWidth = 24f
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Highlighter",
                                tint = if (currentTool == InkTool.HIGHLIGHTER) PrimaryCyanBlue else capsuleIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { currentTool = InkTool.ERASER },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Eraser",
                                tint = if (currentTool == InkTool.ERASER) PrimaryCyanBlue else capsuleIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // ─── Shape Tools: Circle, Rectangle, Arrow ──────────────────
                        IconButton(
                            onClick = { currentTool = InkTool.SHAPE_CIRCLE },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Circle Shape",
                                tint = if (currentTool == InkTool.SHAPE_CIRCLE) PrimaryCyanBlue else capsuleIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { currentTool = InkTool.SHAPE_RECTANGLE },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropSquare,
                                contentDescription = "Rectangle Shape",
                                tint = if (currentTool == InkTool.SHAPE_RECTANGLE) PrimaryCyanBlue else capsuleIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { currentTool = InkTool.SHAPE_ARROW },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Arrow Shape",
                                tint = if (currentTool == InkTool.SHAPE_ARROW) PrimaryCyanBlue else capsuleIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { currentTool = InkTool.LASSO },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Lasso Selection",
                                tint = if (currentTool == InkTool.LASSO) PrimaryCyanBlue else capsuleIconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // ─── Laser Pointer Tool (Presentation Mode) ─────────────
                        Box {
                            IconButton(
                                onClick = {
                                    if (currentTool == InkTool.LASER) {
                                        showLaserOptionsPopup = !showLaserOptionsPopup
                                    } else {
                                        currentTool = InkTool.LASER
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Highlight,
                                    contentDescription = "Laser Pointer",
                                    tint = if (currentTool == InkTool.LASER) PrimaryCyanBlue else capsuleIconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showLaserOptionsPopup,
                                onDismissRequest = { showLaserOptionsPopup = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mode: ${if (laserConfig.mode == LaserMode.TRAIL) "Comet Tail" else "Spotlight Dot"}") },
                                    onClick = {
                                        laserConfig = laserConfig.copy(
                                            mode = if (laserConfig.mode == LaserMode.TRAIL) LaserMode.DOT else LaserMode.TRAIL
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (laserConfig.mode == LaserMode.TRAIL) Icons.Default.Gesture else Icons.Default.Lens,
                                            contentDescription = null
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            LaserConstants.PALETTE_HEX.forEach { hex ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(hex))
                                                        .clickable {
                                                            laserConfig = laserConfig.copy(colorHex = hex)
                                                        }
                                                        .then(
                                                            if (laserConfig.colorHex == hex) {
                                                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                            } else Modifier
                                                        )
                                                )
                                            }
                                        }
                                    },
                                    onClick = {}
                                )

                                if (laserConfig.mode == LaserMode.TRAIL) {
                                    DropdownMenuItem(
                                        text = { Text("Decay: ${laserConfig.durationMs}ms") },
                                        onClick = {
                                            val nextDuration = when (laserConfig.durationMs) {
                                                LaserConstants.DURATION_FAST_MS -> LaserConstants.DURATION_DEFAULT_MS
                                                LaserConstants.DURATION_DEFAULT_MS -> LaserConstants.DURATION_LONG_MS
                                                else -> LaserConstants.DURATION_FAST_MS
                                            }
                                            laserConfig = laserConfig.copy(durationMs = nextDuration)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        DockDivider(orientation = orientation)

                        // Academic Zero-Tap Color Well (Black, Blue, Crimson)
                        ZeroTapColorWell(
                            selectedColor = selectedColor,
                            onColorSelected = { color ->
                                selectedColor = color
                                if (currentTool == InkTool.ERASER) {
                                    currentTool = InkTool.PEN
                                }
                            },
                            isVertical = orientation == DockOrientation.Vertical
                        )

                        DockDivider(orientation = orientation)

                        IconButton(
                            onClick = { if (activePageId != null) viewModel.undo(activePageId) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = capsuleIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { if (activePageId != null) viewModel.redo(activePageId) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redo,
                                contentDescription = "Redo",
                                tint = capsuleIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DockDivider(orientation = orientation)
                    }

                    // Dynamic Theme Mode Toggle Button (Light/Dark Instant Switch)
                    IconButton(
                        onClick = { settingsViewModel.toggleThemeMode() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = if (isDarkTheme) Color(0xFFFFD54F) else capsuleIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DockDivider(orientation = orientation)

                    IconButton(
                        onClick = { showNoteOptionsSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Note Options",
                            tint = capsuleIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // In-Editor Collapsible Left Panel (240 dp)
                AnimatedVisibility(
                    visible = isLeftPanelOpen,
                    enter = slideInHorizontally { -it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut()
                ) {
                    EditorLeftPanel(
                        pages = pages,
                        activePageId = activePageId,
                        outlineTree = outlineTree,
                        onSelectPage = { index ->
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(index)
                            }
                        },
                        onToggleBookmark = { pageId -> viewModel.toggleBookmark(pageId) },
                        onClosePanel = { viewModel.setLeftPanelOpen(false) }
                    )
                }

                val deskColor = if (isDarkTheme) Color(0xFF141416) else Color(0xFFF5F5F7)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(deskColor)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            val slop = viewConfiguration.touchSlop
                            val detector = TwoFingerIntentGestureDetector(touchSlop = slop, zoomSlop = slop, invertScrollDirection = true)

                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val activePointers = event.changes.filter { it.pressed }

                                    if (activePointers.size >= 2) {
                                        // Multi-touch active: consume so child canvases don't receive ink
                                        activePointers.forEach { it.consume() }

                                        val currentCentroid = activePointers.fold(Offset.Zero) { acc, c ->
                                            acc + c.position
                                        } / activePointers.size.toFloat()

                                        val currentSpan = activePointers.fold(0f) { acc, c ->
                                            acc + (c.position - currentCentroid).getDistance()
                                        } / activePointers.size.toFloat()

                                        if (detector.mode == TwoFingerGestureMode.UNDECIDED && detector.isInitialState()) {
                                            detector.onPointersDown(currentCentroid, currentSpan)
                                        }

                                        val step = detector.onPointersMove(
                                            currentCentroid = currentCentroid,
                                            currentSpan = currentSpan,
                                            isZoomed = zoomState.scale > 1.05f
                                        )

                                        when (step.mode) {
                                            TwoFingerGestureMode.SCROLL_LOCKED -> {
                                                if (step.verticalScrollDelta != 0f) {
                                                    lazyListState.dispatchRawDelta(step.verticalScrollDelta)
                                                }
                                            }
                                            TwoFingerGestureMode.ZOOM_LOCKED -> {
                                                val result = ZoomTransformSolver.calculateZoomStep(
                                                    previousState = zoomState,
                                                    zoomChange = step.zoomChange,
                                                    panDeltaX = step.panDeltaX,
                                                    panDeltaY = 0f, // continuous vertical scrolling suppressed
                                                    focalPointX = currentCentroid.x,
                                                    viewportWidth = size.width.toFloat()
                                                )
                                                zoomState = result.zoomState
                                            }
                                            TwoFingerGestureMode.UNDECIDED -> {
                                                // Zero transformation dispatched while accumulating slop
                                            }
                                        }
                                    } else {
                                        detector.onPointersUp()
                                        if (activePointers.isEmpty()) {
                                            // Settle zoom state cleanly with zero drift
                                            if (zoomState.scale <= 1.0f) {
                                                zoomState = ZoomTransformSolver.ZoomState(scale = 1.0f, panX = 0f)
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                        }
                ) {
            when {
                uiState.isLoading || uiState.document == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = PrimaryCyanBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.Button(onClick = { if (documentId != null) viewModel.loadDocument(documentId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                pages.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pages available", color = SecondaryGray)
                    }
                }
                else -> {
                    // ─── Virtualized Continuous Document Page Stream ────────────────────
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomState.scale
                                scaleY = zoomState.scale
                                translationX = zoomState.panX
                                translationY = 0f
                                transformOrigin = TransformOrigin(0f, 0f)
                            },
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isDualPageSpread) {
                            val pagePairs = pages.chunked(2)
                            itemsIndexed(pagePairs, key = { _, pair -> pair.first().id }) { pairIndex, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Page
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val leftPage = pair[0]
                                        val isLeftActive = leftPage.id == activePageId || (activePageId == null && (pairIndex * 2) == currentVisiblePageIndex)
                                        PageCard(
                                            page = leftPage,
                                            pages = pages,
                                            isActive = isLeftActive,
                                            pageStrokesList = pageStrokes[leftPage.id] ?: emptyList(),
                                            textList = pageTextAnnotations[leftPage.id] ?: emptyList(),
                                            mediaList = pageMediaAnnotations[leftPage.id] ?: emptyList(),
                                            documentZoomScale = documentZoomScale,
                                            currentTool = currentTool,
                                            isReadOnly = isReadOnly,
                                            audioPlaybackPositionMs = audioPlaybackPositionMs,
                                            selectedColor = selectedColor,
                                            selectedWidth = selectedWidth,
                                            eraserMode = eraserMode,
                                            eraserTarget = eraserTarget,
                                            eraserThickness = eraserThickness,
                                            layers = layers,
                                            activeLayerId = activeLayerId,
                                            smartGesturesEnabled = smartGesturesEnabled,
                                            audioSyncState = audioSyncState,
                                            searchUiState = searchUiState,
                                            isDualSpread = true,
                                            onStrokeDrawn = { stroke -> viewModel.saveStroke(leftPage.id, stroke) },
                                            onStrokeRemoved = { strokeId -> viewModel.removeStroke(leftPage.id, strokeId) },
                                            onSelectionChanged = { selection -> selectedStrokes = selection },
                                            onSaveText = { text -> viewModel.saveTextAnnotation(leftPage.id, text) },
                                            onRemoveText = { id -> viewModel.removeTextAnnotation(leftPage.id, id) },
                                            onSaveMedia = { media -> viewModel.saveMediaAnnotation(leftPage.id, media) },
                                            onRemoveMedia = { id -> viewModel.removeMediaAnnotation(leftPage.id, id) },
                                            onSeekAudio = { stroke -> viewModel.seekToStrokeAudio(stroke) },
                                            onPinchZoom = { factor ->
                                                documentZoomScale = (documentZoomScale * factor).coerceIn(0.5f, 3.0f)
                                            }
                                        )
                                    }

                                    // Right Page
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (pair.size > 1) {
                                            val rightPage = pair[1]
                                            val isRightActive = rightPage.id == activePageId || (activePageId == null && (pairIndex * 2 + 1) == currentVisiblePageIndex)
                                            PageCard(
                                                page = rightPage,
                                                pages = pages,
                                                isActive = isRightActive,
                                                pageStrokesList = pageStrokes[rightPage.id] ?: emptyList(),
                                                textList = pageTextAnnotations[rightPage.id] ?: emptyList(),
                                                mediaList = pageMediaAnnotations[rightPage.id] ?: emptyList(),
                                                documentZoomScale = documentZoomScale,
                                                currentTool = currentTool,
                                                isReadOnly = isReadOnly,
                                                audioPlaybackPositionMs = audioPlaybackPositionMs,
                                                selectedColor = selectedColor,
                                                selectedWidth = selectedWidth,
                                                eraserMode = eraserMode,
                                                eraserTarget = eraserTarget,
                                                eraserThickness = eraserThickness,
                                                layers = layers,
                                                activeLayerId = activeLayerId,
                                                smartGesturesEnabled = smartGesturesEnabled,
                                                audioSyncState = audioSyncState,
                                                searchUiState = searchUiState,
                                                isDualSpread = true,
                                                onStrokeDrawn = { stroke -> viewModel.saveStroke(rightPage.id, stroke) },
                                                onStrokeRemoved = { strokeId -> viewModel.removeStroke(rightPage.id, strokeId) },
                                                onSelectionChanged = { selection -> selectedStrokes = selection },
                                                onSaveText = { text -> viewModel.saveTextAnnotation(rightPage.id, text) },
                                                onRemoveText = { id -> viewModel.removeTextAnnotation(rightPage.id, id) },
                                                onSaveMedia = { media -> viewModel.saveMediaAnnotation(rightPage.id, media) },
                                                onRemoveMedia = { id -> viewModel.removeMediaAnnotation(rightPage.id, id) },
                                                onSeekAudio = { stroke -> viewModel.seekToStrokeAudio(stroke) },
                                                onPinchZoom = { factor ->
                                                    documentZoomScale = (documentZoomScale * factor).coerceIn(0.5f, 3.0f)
                                                }
                                            )
                                        } else {
                                            val leftAspectRatio = if (pair[0].heightPt > 0f) pair[0].widthPt / pair[0].heightPt else (595f / 842f)
                                            Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(leftAspectRatio))
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                                val isPageActive = page.id == activePageId || (activePageId == null && index == currentVisiblePageIndex)
                                PageCard(
                                    page = page,
                                    pages = pages,
                                    isActive = isPageActive,
                                    pageStrokesList = pageStrokes[page.id] ?: emptyList(),
                                    textList = pageTextAnnotations[page.id] ?: emptyList(),
                                    mediaList = pageMediaAnnotations[page.id] ?: emptyList(),
                                    documentZoomScale = documentZoomScale,
                                    currentTool = currentTool,
                                    isReadOnly = isReadOnly,
                                    audioPlaybackPositionMs = audioPlaybackPositionMs,
                                    selectedColor = selectedColor,
                                    selectedWidth = selectedWidth,
                                    eraserMode = eraserMode,
                                    eraserTarget = eraserTarget,
                                    eraserThickness = eraserThickness,
                                    layers = layers,
                                    activeLayerId = activeLayerId,
                                    smartGesturesEnabled = smartGesturesEnabled,
                                    audioSyncState = audioSyncState,
                                    searchUiState = searchUiState,
                                    isDualSpread = false,
                                    onStrokeDrawn = { stroke -> viewModel.saveStroke(page.id, stroke) },
                                    onStrokeRemoved = { strokeId -> viewModel.removeStroke(page.id, strokeId) },
                                    onSelectionChanged = { selection -> selectedStrokes = selection },
                                    onSaveText = { text -> viewModel.saveTextAnnotation(page.id, text) },
                                    onRemoveText = { id -> viewModel.removeTextAnnotation(page.id, id) },
                                    onSaveMedia = { media -> viewModel.saveMediaAnnotation(page.id, media) },
                                    onRemoveMedia = { id -> viewModel.removeMediaAnnotation(page.id, id) },
                                    onSeekAudio = { stroke -> viewModel.seekToStrokeAudio(stroke) },
                                    onPinchZoom = { factor ->
                                        documentZoomScale = (documentZoomScale * factor).coerceIn(0.5f, 3.0f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

                // ─── Unified Top Toolbar (Deprecated in favor of AdaptiveDockScaffold above) ─

                // ─── Floating Audio Recording & Playback Pill (Isolated from Toolbar) ──────
                androidx.compose.animation.AnimatedVisibility(
                    visible = audioSyncState.state != com.mal5odha.core.audio.AudioState.IDLE,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 76.dp)
                        .padding(WindowInsets.safeDrawing.asPaddingValues())
                ) {
                    AudioRecordingPill(
                        syncState = audioSyncState,
                        onStartRecording = {
                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        },
                        onPauseRecording = { viewModel.pauseAudioRecording() },
                        onResumeRecording = { viewModel.resumeAudioRecording() },
                        onStopRecording = { viewModel.stopAudioRecording() },
                        onPlayPausePlayback = { viewModel.playPauseAudioPlayback() },
                        onSeekTo = { viewModel.seekAudioTo(it) },
                        onSkip = { viewModel.skipAudio(it) },
                        onStopPlayback = { viewModel.stopAudioPlayback() }
                    )
                }

                // ─── Floating Stroke Selection / Lasso Action Toolbar ──────────────────────
                if (selectedStrokes.isNotEmpty() && currentTool == InkTool.LASSO && !isReadOnly) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val activeStrokeColor = selectedStrokes.firstOrNull()?.color
                    StrokeSelectionToolbar(
                        selectedCount = selectedStrokes.size,
                        activeColor = activeStrokeColor,
                        onColorSelected = { newColor ->
                            val newColorArgb = newColor.toArgb()
                            selectedColor = newColorArgb
                            com.mal5odha.app.ui.theme.NotePaletteDefaults.recordUsedColor(newColor)
                            val updated = viewModel.updateSelectedStrokesColor(
                                newColorArgb = newColorArgb,
                                pageId = activePageId,
                                strokes = selectedStrokes
                            )
                            selectedStrokes = updated
                        },
                        onDelete = {
                            if (activePageId != null && selectedStrokes.isNotEmpty()) {
                                selectedStrokes.forEach { stroke ->
                                    viewModel.removeStroke(activePageId, stroke.id)
                                }
                                selectedStrokes = emptyList()
                            }
                        },
                        onDuplicate = {
                            if (activePageId != null && selectedStrokes.isNotEmpty()) {
                                val duplicated = selectedStrokes.map { stroke ->
                                    val dup = stroke.copy(
                                        id = UUID.randomUUID().toString(),
                                        points = stroke.points.map { p -> p.copy(x = p.x + 0.015f, y = p.y + 0.015f) }.toMutableList()
                                    )
                                    dup.precomputePathGeometry()
                                    viewModel.saveStroke(activePageId, dup)
                                    dup
                                }
                                selectedStrokes = duplicated
                            }
                        },
                        onCopy = {
                            android.widget.Toast.makeText(context, "Copied ${selectedStrokes.size} strokes", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onCut = {
                            if (activePageId != null && selectedStrokes.isNotEmpty()) {
                                selectedStrokes.forEach { stroke ->
                                    viewModel.removeStroke(activePageId, stroke.id)
                                }
                                selectedStrokes = emptyList()
                                android.widget.Toast.makeText(context, "Cut selection", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp)
                    )
                }

                // ─── Low-Profile Waveform Audio Sync Scrubber (Milestone 4) ─────────────
                val isAudioPlayingOrPaused = audioSyncState.state == com.mal5odha.core.audio.AudioState.PLAYING ||
                        audioSyncState.state == com.mal5odha.core.audio.AudioState.PLAYBACK_PAUSED

                androidx.compose.animation.AnimatedVisibility(
                    visible = isAudioPlayingOrPaused || audioSyncState.durationMs > 0L,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 74.dp)
                        .padding(WindowInsets.safeDrawing.asPaddingValues())
                ) {
                    AudioSyncScrubber(
                        currentPositionMs = audioPlaybackPositionMs,
                        durationMs = audioSyncState.durationMs,
                        isPlaying = audioSyncState.state == com.mal5odha.core.audio.AudioState.PLAYING,
                        onPlayPauseToggled = { viewModel.playPauseAudioPlayback() },
                        onSeekTo = { pos -> viewModel.seekAudioTo(pos) },
                        onSkip = { delta -> viewModel.skipAudio(delta) },
                        onClose = { viewModel.stopAudioPlayback() }
                    )
                }

                // ─── Bottom Thumbnail Strip & Add Page Action ──────────────────────
                PageThumbnailStrip(
                    pages = pages,
                    currentPageIndex = currentVisiblePageIndex,
                    onPageSelected = { index ->
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(index)
                        }
                    },
                    onAddPage = {
                        viewModel.addNewPage(PageBackground.BLANK)
                        coroutineScope.launch {
                            delay(100)
                            lazyListState.animateScrollToItem(pages.size)
                        }
                    },
                    onOverviewClicked = {
                        viewModel.setLeftPanelOpen(!isLeftPanelOpen)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = WindowInsets.safeDrawing
                                .asPaddingValues()
                                .calculateBottomPadding()
                        )
                )

                // ─── PDF Export Dialog ─────────────────────────────────────────────
                if (showExportDialog) {
                    val exportState by viewModel.exportState.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    com.mal5odha.app.ui.components.ExportOptionsDialog(
                        isExporting = exportState.isExporting,
                        progress = exportState.progress,
                        exportError = exportState.error,
                        onDismiss = {
                            viewModel.dismissExportState()
                            showExportDialog = false
                        },
                        onExportPdf = { mode ->
                            viewModel.exportDocument(mode) { uri ->
                                showExportDialog = false
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(shareIntent, "Share Document PDF")
                                )
                            }
                        }
                    )
                }


                // ─── Layer Management Sheet ─────────────────────────────────────────
                if (showLayerSheet) {
                    LayerManagementSheet(
                        layers = layers,
                        activeLayerId = activeLayerId,
                        onSelectActiveLayer = { viewModel.selectActiveLayer(it) },
                        onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                        onToggleLock = { viewModel.toggleLayerLock(it) },
                        onOpacityChanged = { id, alpha -> viewModel.updateLayerOpacity(id, alpha) },
                        onAddLayer = { viewModel.addCustomLayer(it) },
                        onReorderLayers = { from, to -> viewModel.reorderLayers(from, to) },
                        onDeleteLayer = { viewModel.deleteLayer(it) },
                        onDismiss = { showLayerSheet = false }
                    )
                }

                // ─── Revision History Sheet ─────────────────────────────────────────
                if (showRevisionSheet) {
                    RevisionHistorySheet(
                        mutations = mutations,
                        onRollbackToMutation = { viewModel.rollbackToMutation(it) },
                        onDismiss = { showRevisionSheet = false }
                    )
                }
                // ─── Floating Zoom Reset Chip ─────────────────────────────────────────
                androidx.compose.animation.AnimatedVisibility(
                    visible = zoomState.scale > 1.05f,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 120.dp)
                ) {
                    Surface(
                        onClick = {
                            zoomState = ZoomTransformSolver.ZoomState(scale = 1.0f, panX = 0f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Reset Zoom",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${(zoomState.scale * 100).toInt()}% (Reset)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ─── Ephemeral Presentation Laser Pointer Overlay ───────────────────
                LaserPointerOverlay(
                    isLaserActive = currentTool == InkTool.LASER,
                    config = laserConfig,
                    modifier = Modifier.fillMaxSize()
                )
            } // Close Box(modifier = Modifier.weight(1f))
            } // Close Row
        } // Close AdaptiveDockScaffold

        // ─── Contextual Note Options Bottom Sheet & Dialogs ──────────────────
        if (showNoteOptionsSheet && uiState.document != null) {
            val currentDoc = uiState.document!!
            NoteOptionsBottomSheet(
                document = currentDoc,
                onDismiss = { showNoteOptionsSheet = false },
                onRename = {
                    showNoteOptionsSheet = false
                    showRenameDialog = true
                },
                onMove = {
                    showNoteOptionsSheet = false
                    showMoveDialog = true
                },
                onDuplicate = {
                    showNoteOptionsSheet = false
                    viewModel.duplicateDocument()
                },
                onToggleStar = {
                    showNoteOptionsSheet = false
                    viewModel.toggleStarred()
                },
                onExport = {
                    showNoteOptionsSheet = false
                    showExportDialog = true
                },
                onDetails = {
                    showNoteOptionsSheet = false
                    viewModel.getNoteDetails { size, pCount ->
                        noteDetailsInfo = Pair(size, pCount)
                        showDetailsDialog = true
                    }
                },
                onDelete = {
                    showNoteOptionsSheet = false
                    viewModel.deleteDocument()
                    onNavigateBack()
                }
            )
        }

        if (showRenameDialog && uiState.document != null) {
            RenameNoteDialog(
                initialTitle = uiState.document!!.title,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newTitle ->
                    viewModel.renameDocument(newTitle)
                    showRenameDialog = false
                }
            )
        }

        if (showMoveDialog && uiState.document != null) {
            MoveToFolderDialog(
                folders = folders,
                currentFolderId = uiState.document!!.parentFolderId,
                onDismiss = { showMoveDialog = false },
                onFolderSelected = { targetFolderId ->
                    viewModel.moveDocument(targetFolderId)
                    showMoveDialog = false
                }
            )
        }

        if (showDetailsDialog && uiState.document != null && noteDetailsInfo != null) {
            NoteDetailsDialog(
                document = uiState.document!!,
                fileSizeBytes = noteDetailsInfo!!.first,
                pageCount = noteDetailsInfo!!.second,
                onDismiss = {
                    showDetailsDialog = false
                    noteDetailsInfo = null
                }
            )
        }
    }
}

@Composable
private fun PageCard(
    page: DocumentPage,
    pages: List<DocumentPage>,
    isActive: Boolean = false,
    pageStrokesList: List<com.mal5odha.core.ink.models.Stroke>,
    textList: List<com.mal5odha.core.ink.models.TextAnnotation>,
    mediaList: List<com.mal5odha.core.ink.models.MediaAnnotation>,
    documentZoomScale: Float = 1f,
    currentTool: InkTool,
    isReadOnly: Boolean = false,
    audioPlaybackPositionMs: Long = 0L,
    selectedColor: Int,
    selectedWidth: Float,
    eraserMode: com.mal5odha.core.ink.models.EraserMode,
    eraserTarget: com.mal5odha.core.ink.models.EraserTarget,
    eraserThickness: Float,
    layers: List<com.mal5odha.core.ink.models.DocumentLayer>,
    activeLayerId: String?,
    smartGesturesEnabled: Boolean,
    audioSyncState: com.mal5odha.core.audio.AudioSyncState,
    searchUiState: SearchUiState,
    isDualSpread: Boolean,
    onStrokeDrawn: (com.mal5odha.core.ink.models.Stroke) -> Unit,
    onStrokeRemoved: (String) -> Unit,
    onSelectionChanged: (List<com.mal5odha.core.ink.models.Stroke>) -> Unit,
    onSaveText: (com.mal5odha.core.ink.models.TextAnnotation) -> Unit,
    onRemoveText: (String) -> Unit,
    onSaveMedia: (com.mal5odha.core.ink.models.MediaAnnotation) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onSeekAudio: (com.mal5odha.core.ink.models.Stroke) -> Unit,
    onPinchZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageAspectRatio = page.aspectRatio

    // Phase C: Animated active page elevation & highlight border
    val cardElevation by animateDpAsState(
        targetValue = if (isActive) 6.dp else 2.dp,
        label = "pageCardElevation"
    )
    val cardBorder = if (isActive) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    } else {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    }

    Card(
        modifier = if (isDualSpread) {
            modifier
                .fillMaxWidth()
                .aspectRatio(pageAspectRatio)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        } else {
            modifier
                .wrapContentSize()
                .widthIn(max = 900.dp)
                .fillMaxWidth(0.92f)
                .aspectRatio(pageAspectRatio)
                .defaultMinSize(minHeight = 400.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .clipToBounds()
        ) {
            // Light background fill
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
            val density = androidx.compose.ui.platform.LocalDensity.current
            val pageWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val pageHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .then(
                        if (currentTool == InkTool.TEXT && !isReadOnly) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val normX = (offset.x / pageWidthPx).coerceIn(0.05f, 0.6f)
                                        val normY = (offset.y / pageHeightPx).coerceIn(0.05f, 0.85f)
                                        val newAnnotation = com.mal5odha.core.ink.models.TextAnnotation(
                                            x = normX,
                                            y = normY,
                                            text = ""
                                        )
                                        onSaveText(newAnnotation)
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                // 1. Base Layer: Document Template Pattern
                PageBackgroundPattern(type = page.background)

                // 2. Base Layer: PDF Page Bitmap / Imported Document Image
                if (!page.backgroundData.isNullOrEmpty()) {
                    val lruCache = remember { com.mal5odha.core.pdf.cache.BitmapLruCache() }
                    val bitmapInfo = remember(page.backgroundData, documentZoomScale) {
                        lruCache.loadFromFile(page.backgroundData)?.asImageBitmap()
                    }
                    if (bitmapInfo != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmapInfo,
                            contentDescription = "Page Background Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }

                // 3. Middle Layer: Media Annotations Overlay
                mediaList.forEach { annotation ->
                    com.mal5odha.app.ui.components.MediaAnnotationOverlay(
                        annotation = annotation,
                        pageWidthPx = pageWidthPx,
                        pageHeightPx = pageHeightPx,
                        isReadOnly = isReadOnly,
                        onAnnotationChanged = onSaveMedia,
                        onDelete = { onRemoveMedia(annotation.id) }
                    )
                }

                // 4. Unified Direct Document Inking Canvas
                if (pageWidthPx > 1f && pageHeightPx > 1f) {
                    DirectPageCanvas(
                        pageId = page.id,
                        pageStrokes = pageStrokesList,
                        currentTool = currentTool,
                        currentColor = selectedColor,
                        currentWidth = selectedWidth,
                        eraserTarget = eraserTarget,
                        eraserThickness = eraserThickness,
                        isReadOnly = isReadOnly,
                        audioPlaybackPositionMs = audioPlaybackPositionMs,
                        currentAudioElapsedMs = audioSyncState.elapsedMs,
                        currentAudioSessionId = audioSyncState.currentSessionId,
                        layers = layers,
                        activeLayerId = activeLayerId,
                        smartGesturesEnabled = smartGesturesEnabled,
                        onStrokeDrawn = onStrokeDrawn,
                        onStrokeRemoved = onStrokeRemoved,
                        onStrokeTapped = onSeekAudio,
                        onSelectionChanged = onSelectionChanged,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 5. Rich Text Annotations
                textList.forEach { annotation ->
                    com.mal5odha.app.ui.components.TextAnnotationOverlay(
                        annotation = annotation,
                        pageWidthPx = pageWidthPx,
                        pageHeightPx = pageHeightPx,
                        isToolActive = currentTool == InkTool.TEXT && !isReadOnly,
                        isReadOnly = isReadOnly,
                        onAnnotationChanged = onSaveText,
                        onDelete = { onRemoveText(annotation.id) }
                    )
                }

                // 6. Search Match Highlights Overlay
                val pageIdx = pages.indexOfFirst { it.id == page.id }
                val matchingHits = remember(searchUiState.results, pageIdx) {
                    if (pageIdx >= 0) searchUiState.results.filter { it.pageIndex == pageIdx } else emptyList()
                }
                if (matchingHits.isNotEmpty()) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        matchingHits.forEach { hit ->
                            val isCurrentHit = searchUiState.currentResultIndex >= 0 &&
                                    searchUiState.results.getOrNull(searchUiState.currentResultIndex) == hit

                            val highlightColor = if (isCurrentHit) Color(0xFFFF9800).copy(alpha = 0.45f) else Color(0xFFFFEB3B).copy(alpha = 0.35f)
                            val strokeColor = if (isCurrentHit) Color(0xFFFF9800) else Color(0xFFFBC02D)

                            val left = (hit.bounds.left * size.width).coerceIn(0f, size.width)
                            val top = (hit.bounds.top * size.height).coerceIn(0f, size.height)
                            val w = (hit.bounds.width() * size.width).coerceIn(12f, size.width - left)
                            val h = (hit.bounds.height() * size.height).coerceIn(12f, size.height - top)

                            drawRoundRect(
                                color = highlightColor,
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(w, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRoundRect(
                                color = strokeColor,
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(w, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageBackgroundPattern(type: PageBackground, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 1.dp.toPx()
        val color = Color.Gray.copy(alpha = 0.25f)
        
        when (type) {
            PageBackground.RULED -> {
                val step = 32.dp.toPx()
                var y = step
                while (y < size.height) {
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                    y += step
                }
            }
            PageBackground.GRID -> {
                val step = 32.dp.toPx()
                var x = step
                while (x < size.width) {
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                    y += step
                }
            }
            PageBackground.DOTTED -> {
                val step = 32.dp.toPx()
                var x = step
                while (x < size.width) {
                    var y = step
                    while (y < size.height) {
                        drawCircle(
                            color = color,
                            radius = 2f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            }
            else -> {}
        }
    }
}
