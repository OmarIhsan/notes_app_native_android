package com.mal5odha.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mal5odha.app.ui.theme.ActionDarkBlue
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.core.ink.models.EraserMode
import com.mal5odha.core.ink.models.InkTool

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopToolbar(
    title: String,
    currentTool: InkTool,
    onToolSelected: (InkTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    selectedWidth: Float,
    onWidthSelected: (Float) -> Unit,
    onNavigateBack: () -> Unit,
    onMediaClicked: () -> Unit,
    onClearCanvas: () -> Unit,
    onPageSetupClicked: () -> Unit,
    onExportClicked: () -> Unit,
    pageCounter: String? = null,
    eraserMode: EraserMode = EraserMode.STROKE,
    onEraserModeChanged: (EraserMode) -> Unit = {},
    eraserTarget: com.mal5odha.core.ink.models.EraserTarget = com.mal5odha.core.ink.models.EraserTarget.ALL,
    onEraserTargetChanged: (com.mal5odha.core.ink.models.EraserTarget) -> Unit = {},
    eraserThickness: Float = 40f,
    onEraserThicknessChanged: (Float) -> Unit = {},
    smartGesturesEnabled: Boolean = true,
    onSmartGesturesToggled: (Boolean) -> Unit = {},
    audioSyncState: com.mal5odha.core.audio.AudioSyncState? = null,
    onStartRecording: () -> Unit = {},
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onPlayPausePlayback: () -> Unit = {},
    onSeekAudioTo: (Long) -> Unit = {},
    onSkipAudio: (Long) -> Unit = {},
    onStopPlayback: () -> Unit = {},
    isReadOnlyMode: Boolean = false,
    onToggleReadOnlyMode: () -> Unit = {},
    searchUiState: com.mal5odha.app.ui.screens.SearchUiState = com.mal5odha.app.ui.screens.SearchUiState(),
    onToggleSearchBar: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onNextSearchResult: () -> Unit = {},
    onPreviousSearchResult: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onOpenLayersSheet: () -> Unit = {},
    onOpenHistorySheet: () -> Unit = {},
    isDualPageSpread: Boolean = false,
    onToggleDualPageSpread: () -> Unit = {},
    onTitleRenamed: (String) -> Unit = {},
    onToggleThemeMode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showColorMenu by remember { mutableStateOf(false) }
    var showWidthMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var activeToolSheet by remember { mutableStateOf<InkTool?>(null) }
    val haptic = LocalHapticFeedback.current

    val recentColors by com.mal5odha.app.ui.theme.NotePaletteDefaults.recentColors.collectAsState()
    val quickColors: List<Int> = remember(recentColors) {
        val recentArgb = recentColors.map { it.toArgb() }
        (recentArgb + com.mal5odha.app.ui.theme.NotePaletteDefaults.InkingPaletteArgb).distinct()
    }
    val strokeWidths = listOf(2f, 5f, 10f, 20f, 40f)

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shadowElevation = 4.dp,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button (48dp outer touch target, 22dp centered icon)
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ActionDarkBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Title & Page Badge (Tap to Rename in-place)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showRenameDialog = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ActionDarkBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename Title",
                        tint = ActionDarkBlue.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )

                    if (!pageCounter.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryCyanBlue.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = pageCounter,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyanBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Inking / Editing Controls (Hidden in Read-Only / Viewing Mode)
                AnimatedVisibility(
                    visible = !isReadOnlyMode,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Center-Left: Undo / Redo (48dp touch targets)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onUndo, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = ActionDarkBlue, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onRedo, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Redo, contentDescription = "Redo", tint = ActionDarkBlue, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Spacer(modifier = Modifier.width(6.dp))

                        // Center: Tools group
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolButton(
                                tool = InkTool.PEN,
                                icon = Icons.Default.Edit,
                                description = "Pen",
                                currentTool = currentTool,
                                onToolSelected = {
                                    if (currentTool == InkTool.PEN) {
                                        activeToolSheet = if (activeToolSheet == InkTool.PEN) null else InkTool.PEN
                                    } else {
                                        onToolSelected(InkTool.PEN)
                                        activeToolSheet = null
                                    }
                                },
                                onToolLongClicked = {
                                    onToolSelected(InkTool.PEN)
                                    activeToolSheet = InkTool.PEN
                                }
                            )
                            ToolButton(
                                tool = InkTool.HIGHLIGHTER,
                                icon = Icons.Default.Brush,
                                description = "Highlighter",
                                currentTool = currentTool,
                                onToolSelected = {
                                    if (currentTool == InkTool.HIGHLIGHTER) {
                                        activeToolSheet = if (activeToolSheet == InkTool.HIGHLIGHTER) null else InkTool.HIGHLIGHTER
                                    } else {
                                        onToolSelected(InkTool.HIGHLIGHTER)
                                        activeToolSheet = null
                                    }
                                },
                                onToolLongClicked = {
                                    onToolSelected(InkTool.HIGHLIGHTER)
                                    activeToolSheet = InkTool.HIGHLIGHTER
                                }
                            )
                            ToolButton(
                                tool = InkTool.ERASER,
                                icon = Icons.Default.Clear,
                                description = "Eraser",
                                currentTool = currentTool,
                                onToolSelected = {
                                    if (currentTool == InkTool.ERASER) {
                                        activeToolSheet = if (activeToolSheet == InkTool.ERASER) null else InkTool.ERASER
                                    } else {
                                        onToolSelected(InkTool.ERASER)
                                        activeToolSheet = null
                                    }
                                },
                                onToolLongClicked = {
                                    onToolSelected(InkTool.ERASER)
                                    activeToolSheet = InkTool.ERASER
                                }
                            )
                            ToolButton(
                                tool = InkTool.LASSO,
                                icon = Icons.Default.Crop,
                                description = "Lasso",
                                currentTool = currentTool,
                                onToolSelected = {
                                    onToolSelected(InkTool.LASSO)
                                    activeToolSheet = null
                                }
                            )
                            ToolButton(
                                tool = InkTool.SHAPE,
                                icon = Icons.Default.Category,
                                description = "Shape",
                                currentTool = currentTool,
                                onToolSelected = {
                                    onToolSelected(InkTool.SHAPE)
                                    activeToolSheet = null
                                }
                            )
                            ToolButton(
                                tool = InkTool.TEXT,
                                icon = Icons.Default.Title,
                                description = "Text",
                                currentTool = currentTool,
                                onToolSelected = {
                                    onToolSelected(InkTool.TEXT)
                                    activeToolSheet = null
                                }
                            )

                            IconButton(onClick = onMediaClicked, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Insert Media",
                                    tint = ActionDarkBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Spacer(modifier = Modifier.width(6.dp))

                        // Center-Right: Color & Width Pickers
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Color Picker Button (48dp touch target, 24dp inner visual chip)
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { showColorMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(selectedColor))
                                            .border(1.5.dp, ActionDarkBlue, CircleShape)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showColorMenu,
                                    onDismissRequest = { showColorMenu = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        quickColors.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        com.mal5odha.app.ui.theme.NotePaletteDefaults.recordUsedColorArgb(color)
                                                        onColorSelected(color)
                                                        showColorMenu = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(color))
                                                        .border(
                                                            2.dp,
                                                            if (selectedColor == color) PrimaryCyanBlue else Color.Transparent,
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Width Picker Button (48dp touch target, 22dp inner icon)
                            Box {
                                IconButton(onClick = { showWidthMenu = true }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.LineWeight, contentDescription = "Stroke Width", tint = ActionDarkBlue, modifier = Modifier.size(22.dp))
                                }

                                DropdownMenu(
                                    expanded = showWidthMenu,
                                    onDismissRequest = { showWidthMenu = false }
                                ) {
                                    strokeWidths.forEach { width ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .height(width.coerceIn(2f, 20f).dp)
                                                            .width(40.dp)
                                                            .background(ActionDarkBlue, RoundedCornerShape(2.dp))
                                                    )
                                                    Text("${width.toInt()} px", style = MaterialTheme.typography.bodyMedium)
                                                    if (selectedWidth == width) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = PrimaryCyanBlue,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                onWidthSelected(width)
                                                showWidthMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Viewing Mode Indicator Pill (Prominent Tap-to-Edit standard across Goodnotes & Samsung Notes)
                AnimatedVisibility(
                    visible = isReadOnlyMode,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Surface(
                        onClick = onToggleReadOnlyMode,
                        shape = RoundedCornerShape(20.dp),
                        color = PrimaryCyanBlue.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PrimaryCyanBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(horizontal = 4.dp).heightIn(min = 44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = PrimaryCyanBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Viewing Mode – Tap to Edit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ActionDarkBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Mic Button (Audio Recording Trigger with zero layout shifting, 48dp touch target)
                IconButton(
                    onClick = {
                        if (audioSyncState?.state == com.mal5odha.core.audio.AudioState.RECORDING) {
                            onStopRecording()
                        } else {
                            onStartRecording()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    val isRecording = audioSyncState?.state == com.mal5odha.core.audio.AudioState.RECORDING
                    Icon(
                        imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Audio Recording" else "Record Audio Memo",
                        tint = if (isRecording) Color.Red else ActionDarkBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Mode Switcher Toggle Button (Edit Mode <-> Read-Only / Viewing Mode - 48dp)
                IconButton(
                    onClick = onToggleReadOnlyMode,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isReadOnlyMode) Icons.Default.Edit else Icons.Default.MenuBook,
                        contentDescription = if (isReadOnlyMode) "Switch to Inking Mode" else "Switch to Viewing Mode",
                        tint = if (isReadOnlyMode) PrimaryCyanBlue else ActionDarkBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Universal Search Button (48dp)
                IconButton(
                    onClick = onToggleSearchBar,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Universal Document Search",
                        tint = if (searchUiState.isSearchBarVisible) PrimaryCyanBlue else ActionDarkBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Layer Management Drawer Button (48dp)
                IconButton(
                    onClick = onOpenLayersSheet,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Document Layers",
                        tint = ActionDarkBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Right: More dropdown menu (48dp)
                Box {
                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = ActionDarkBlue, modifier = Modifier.size(22.dp))
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Dual-Page Spread") },
                            leadingIcon = { Icon(Icons.Default.AutoStories, contentDescription = null, tint = if (isDualPageSpread) PrimaryCyanBlue else ActionDarkBlue) },
                            trailingIcon = {
                                Switch(
                                    checked = isDualPageSpread,
                                    onCheckedChange = { onToggleDualPageSpread() }
                                )
                            },
                            onClick = { onToggleDualPageSpread() }
                        )
                        DropdownMenuItem(
                            text = { Text("Revision History") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onOpenHistorySheet()
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Page Organizer") },
                            leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onPageSetupClicked()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Canvas") },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onClearCanvas()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Smart Gestures")
                                    Switch(
                                        checked = smartGesturesEnabled,
                                        onCheckedChange = { onSmartGesturesToggled(it) },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Gesture, contentDescription = null, tint = if (smartGesturesEnabled) PrimaryCyanBlue else Color.Gray) },
                            onClick = {
                                onSmartGesturesToggled(!smartGesturesEnabled)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to PDF") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onExportClicked()
                            }
                        )
                        if (onToggleThemeMode != null) {
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Toggle Dark / Light Theme") },
                                leadingIcon = { Icon(Icons.Default.BrightnessMedium, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onToggleThemeMode()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Expandable Universal Search Bar Overlay
        AnimatedVisibility(
            visible = searchUiState.isSearchBarVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, PrimaryCyanBlue.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = PrimaryCyanBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchUiState.query,
                        onValueChange = onSearchQueryChanged,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = ActionDarkBlue),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchUiState.query.isEmpty()) {
                                Text(
                                    "Search handwriting, notes & PDF...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchUiState.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryCyanBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (searchUiState.results.isNotEmpty()) {
                        Text(
                            text = "${searchUiState.currentResultIndex + 1} of ${searchUiState.results.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActionDarkBlue,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = onPreviousSearchResult,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", tint = ActionDarkBlue, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onNextSearchResult,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", tint = ActionDarkBlue, modifier = Modifier.size(18.dp))
                        }
                    } else if (searchUiState.query.isNotBlank()) {
                        Text(
                            text = "No matches",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onCloseSearch,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close search", tint = ActionDarkBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Expandable Tool Customization Popover Sheet (Pen, Highlighter, Eraser)
        AnimatedVisibility(
            visible = activeToolSheet != null && !isReadOnlyMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            when (activeToolSheet) {
                InkTool.PEN -> {
                    PenCustomizationSheet(
                        selectedColor = selectedColor,
                        onColorSelected = onColorSelected,
                        selectedWidth = selectedWidth,
                        onWidthSelected = onWidthSelected,
                        onDismiss = { activeToolSheet = null }
                    )
                }
                InkTool.HIGHLIGHTER -> {
                    HighlighterCustomizationSheet(
                        selectedColor = selectedColor,
                        onColorSelected = onColorSelected,
                        selectedWidth = selectedWidth,
                        onWidthSelected = onWidthSelected,
                        onDismiss = { activeToolSheet = null }
                    )
                }
                InkTool.ERASER -> {
                    EraserCustomizationSheet(
                        eraserMode = eraserMode,
                        onEraserModeChanged = onEraserModeChanged,
                        eraserTarget = eraserTarget,
                        onEraserTargetChanged = onEraserTargetChanged,
                        eraserThickness = eraserThickness,
                        onEraserThicknessChanged = onEraserThicknessChanged,
                        onClearPage = {
                            activeToolSheet = null
                            onClearCanvas()
                        },
                        onDismiss = { activeToolSheet = null }
                    )
                }
                else -> Unit
            }
        }

        if (showRenameDialog) {
            RenameNoteDialog(
                initialTitle = title,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newTitle ->
                    showRenameDialog = false
                    onTitleRenamed(newTitle)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolButton(
    tool: InkTool,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    currentTool: InkTool,
    onToolSelected: () -> Unit,
    onToolLongClicked: () -> Unit = {}
) {
    val isSelected = currentTool == tool
    val backgroundColor = if (isSelected) PrimaryCyanBlue.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isSelected) PrimaryCyanBlue else ActionDarkBlue
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToolSelected()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToolLongClicked()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PenCustomizationSheet(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    selectedWidth: Float,
    onWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = com.mal5odha.app.ui.theme.NotePaletteDefaults.InkingPaletteArgb

    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.wrapContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pen Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ActionDarkBlue)
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
            }

            // Thickness Preset Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(2.5f to "Fine", 5.0f to "Medium", 9.0f to "Bold").forEach { (width, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(selectedWidth - width) < 0.5f,
                        onClick = { onWidthSelected(width) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Continuous Width Slider + Live Preview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${selectedWidth.toInt()} pt",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ActionDarkBlue,
                    modifier = Modifier.width(36.dp)
                )

                Slider(
                    value = selectedWidth,
                    onValueChange = onWidthSelected,
                    valueRange = 1f..30f,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(selectedWidth.coerceIn(2f, 26f).dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor))
                    )
                }
            }

            // 12-Color Palette Grid
            Text("Color Palette", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                palette.take(6).forEach { color ->
                    ColorCircle(color = color, isSelected = selectedColor == color, onSelect = onColorSelected)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                palette.drop(6).forEach { color ->
                    ColorCircle(color = color, isSelected = selectedColor == color, onSelect = onColorSelected)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HighlighterCustomizationSheet(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    selectedWidth: Float,
    onWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val pastelPalette = listOf(
        android.graphics.Color.parseColor("#FFF176"), // Fluorescent Yellow
        android.graphics.Color.parseColor("#A5D6A7"), // Pastel Mint
        android.graphics.Color.parseColor("#90CAF9"), // Pastel Sky Blue
        android.graphics.Color.parseColor("#F48FB1"), // Soft Pink
        android.graphics.Color.parseColor("#CE93D8"), // Lilac Purple
        android.graphics.Color.parseColor("#FFCC80")  // Peach
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.wrapContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Highlighter Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ActionDarkBlue)
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
            }

            // Thickness Preset Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(12f to "Fine", 24f to "Medium", 38f to "Broad").forEach { (width, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(selectedWidth - width) < 1f,
                        onClick = { onWidthSelected(width) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Width Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${selectedWidth.toInt()} pt",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ActionDarkBlue,
                    modifier = Modifier.width(36.dp)
                )

                Slider(
                    value = selectedWidth,
                    onValueChange = onWidthSelected,
                    valueRange = 8f..50f,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = selectedWidth.coerceIn(4f, 26f).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(selectedColor).copy(alpha = 0.5f))
                )
            }

            // Pastel Colors
            Text("Pastel Inks", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pastelPalette.forEach { color ->
                    ColorCircle(color = color, isSelected = selectedColor == color, onSelect = onColorSelected)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EraserCustomizationSheet(
    eraserMode: EraserMode,
    onEraserModeChanged: (EraserMode) -> Unit,
    eraserTarget: com.mal5odha.core.ink.models.EraserTarget = com.mal5odha.core.ink.models.EraserTarget.ALL,
    onEraserTargetChanged: (com.mal5odha.core.ink.models.EraserTarget) -> Unit = {},
    eraserThickness: Float,
    onEraserThicknessChanged: (Float) -> Unit,
    onClearPage: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.wrapContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Eraser Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ActionDarkBlue)
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
            }

            // Mode Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = eraserMode == EraserMode.STROKE,
                    onClick = { onEraserModeChanged(EraserMode.STROKE) },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Stroke Eraser") }
                )
                FilterChip(
                    selected = eraserMode == EraserMode.PRECISION,
                    onClick = { onEraserModeChanged(EraserMode.PRECISION) },
                    leadingIcon = { Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Precision Eraser") }
                )
            }

            // Target Filtering (Commercial feature - Selective Layer Erasure)
            Text(
                text = "Target Elements",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ActionDarkBlue
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = eraserTarget == com.mal5odha.core.ink.models.EraserTarget.ALL,
                    onClick = { onEraserTargetChanged(com.mal5odha.core.ink.models.EraserTarget.ALL) },
                    label = { Text("All Strokes") }
                )
                FilterChip(
                    selected = eraserTarget == com.mal5odha.core.ink.models.EraserTarget.ACTIVE_LAYER_ONLY,
                    onClick = { onEraserTargetChanged(com.mal5odha.core.ink.models.EraserTarget.ACTIVE_LAYER_ONLY) },
                    label = { Text("Active Layer") }
                )
                FilterChip(
                    selected = eraserTarget == com.mal5odha.core.ink.models.EraserTarget.HIGHLIGHTER_ONLY,
                    onClick = { onEraserTargetChanged(com.mal5odha.core.ink.models.EraserTarget.HIGHLIGHTER_ONLY) },
                    label = { Text("Highlighters") }
                )
            }

            // Eraser Size Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${eraserThickness.toInt()} px",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ActionDarkBlue,
                    modifier = Modifier.width(42.dp)
                )

                Slider(
                    value = eraserThickness,
                    onValueChange = onEraserThicknessChanged,
                    valueRange = 15f..100f,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((eraserThickness / 3.5f).coerceIn(6f, 28f).dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .border(1.dp, Color.DarkGray, CircleShape)
                    )
                }
            }

            // Clear Canvas Action
            OutlinedButton(
                onClick = onClearPage,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Entire Page")
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: Int,
    isSelected: Boolean,
    onSelect: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                com.mal5odha.app.ui.theme.NotePaletteDefaults.recordUsedColorArgb(color)
                onSelect(color)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) PrimaryCyanBlue else Color.LightGray.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
    }
}
