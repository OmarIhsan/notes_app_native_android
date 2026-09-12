package com.mal5odha.core.ink

import com.mal5odha.core.ink.models.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the state and history of drawing commands to enable Undo/Redo functionality. Designed to
 * separate history logic from the Android View layer.
 */
class UndoRedoManager {

    private val _history = MutableStateFlow<List<Stroke>>(emptyList())
    val history: StateFlow<List<Stroke>> = _history.asStateFlow()

    private val _redoStack = MutableStateFlow<List<Stroke>>(emptyList())
    val redoStack: StateFlow<List<Stroke>> = _redoStack.asStateFlow()

    val canUndo: Boolean
        get() = _history.value.isNotEmpty()

    val canRedo: Boolean
        get() = _redoStack.value.isNotEmpty()

    /** Records a new stroke action and clears the redo stack. */
    fun recordAction(stroke: Stroke) {
        _history.update { it + stroke }
        _redoStack.value = emptyList() // Any new action invalidates the redo future
    }

    /** Initializes the manager with a pre-existing list of strokes (e.g., from a file). */
    fun setInitialState(strokes: List<Stroke>) {
        _history.value = strokes.toList()
        _redoStack.value = emptyList()
    }

    /** Explicitly removes a stroke from history (e.g., via selection deletion). */
    fun removeStroke(stroke: Stroke) {
        _history.update { it.filter { s -> s.id != stroke.id } }
        _redoStack.value = emptyList() // Clearing redo to avoid state inconsistency after manual removal
    }

    /**
     * Undoes the last action, moving it to the redo stack. Returns the stroke that was undone, or
     * null if nothing to undo.
     */
    fun undo(): Stroke? {
        val currentHistory = _history.value
        if (currentHistory.isEmpty()) return null

        val strokeToUndo = currentHistory.last()
        _history.value = currentHistory.dropLast(1)
        _redoStack.update { it + strokeToUndo }

        return strokeToUndo
    }

    /**
     * Redoes the last undone action, moving it back to history. Returns the stroke that was redone,
     * or null if nothing to redo.
     */
    fun redo(): Stroke? {
        val currentRedo = _redoStack.value
        if (currentRedo.isEmpty()) return null

        val strokeToRedo = currentRedo.last()
        _redoStack.value = currentRedo.dropLast(1)
        _history.update { it + strokeToRedo }

        return strokeToRedo
    }

    /** Clears all history. */
    fun clear() {
        _history.value = emptyList()
        _redoStack.value = emptyList()
    }
}
