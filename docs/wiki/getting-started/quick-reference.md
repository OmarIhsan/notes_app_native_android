# Quick Reference & Developer Runbooks

- **Subsystem**: Developer Workflows & Runbooks

---

## 1. How to Add a New Inking Tool

To introduce a new digital inking instrument (e.g., Pencil, Marker):

1. **Extend Tool Enum**:
   Update `InkTool` in [`com.mal5odha.core.ink.models.InkTool`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/ink/models/InkTool.kt):
   ```kotlin
   enum class InkTool { PEN, HIGHLIGHTER, ERASER, LASSO, PENCIL }
   ```
2. **Configure Paint Dynamics**:
   In [`DrawingSurface.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/ink/ui/DrawingSurface.kt), add the tool's paint parameters (stroke alpha, cap style, color blending).
3. **Register UI Action**:
   Add the tool icon to [`FloatingToolbar.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/app/ui/components/FloatingToolbar.kt) and wire click events to `editorViewModel.setTool(InkTool.PENCIL)`.
4. **Update Export Painter**:
   Add corresponding vector paint handling in [`PdfExportService.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/pdf/PdfExportService.kt).

---

## 2. How to Add a New Paper Background Pattern

1. **Update Enum**:
   In [`UnifiedDocument.kt:3-10`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/data/models/UnifiedDocument.kt#L3-L10), add the new background enum:
   ```kotlin
   enum class PageBackground { BLANK, PDF_PAGE, IMAGE, RULED, GRID, DOTTED, CORNELL }
   ```
2. **Implement Drawing Logic**:
   In `DrawingSurface.kt` (under background pass) and `PdfExportService.kt`, render the background pattern grid / lines:
   ```kotlin
   PageBackground.CORNELL -> drawCornellNotesLayout(canvas, bounds, paint)
   ```
3. **Update UI Picker**:
   Add the template option to `NewDocumentOptionsDialog.kt`.

---

## 3. How to Debug Inking Latency & Frame Drops

1. **Enable StrictMode**:
   Verify no disk writes occur on the main thread during drawing (`StrictMode.setThreadPolicy(...)`).
2. **GPU Profiling in Developer Options**:
   Enable **"Profile HWUI rendering"** -> **"On screen as bars"**. Ensure drawing bars remain comfortably below the $8.33\text{ ms}$ (for $120\text{ Hz}$) or $16.6\text{ ms}$ (for $60\text{ Hz}$) green threshold.
3. **Inspect MotionPrediction**:
   In logcat, monitor prediction errors:
   ```bash
   adb logcat -s DrawingSurface:D MotionPredictor:D
   ```
