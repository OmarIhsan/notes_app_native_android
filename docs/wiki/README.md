# Mal5odha Architecture & Technical Wiki

Welcome to the comprehensive technical architecture and subsystem documentation for the **Mal5odha** native Android handwritten note-taking, PDF annotation, and document scanner application.

---

## Table of Contents

### 1. Onboarding
- [**Principal-Level Architectural Guide**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/onboarding/principal-guide.md): The core architectural insight, dual-buffering pipeline, Mermaid system topology, domain ERD, and trade-off analysis.
- [**Zero-to-Hero Learning Path**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/onboarding/zero-to-hero-guide.md): Progressive onboarding, cross-language comparisons (Kotlin/Compose vs Python/React), local dev setup, and 40+ term glossary.

### 2. Getting Started
- [**Project Overview & Architecture**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/getting-started/project-overview.md): Clean Architecture & MVI layer overview.
- [**Environment Setup & Build Instructions**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/getting-started/build-and-environment-setup.md): Gradle version catalog, SDK targets, and build commands.
- [**Navigation & Route Directory**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/getting-started/navigation-and-feature-routes.md): Compose NavHost routing topology.
- [**Quick Reference & Common Tasks**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/getting-started/quick-reference.md): How to add ink tools, paper templates, and debug latency.

### 3. Deep Dive Subsystems
#### A. Digital Inking Engine
- [**DrawingSurface & Low-Latency Rendering**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/inking-engine/drawing-surface-view.md): `SurfaceView`, `CanvasFrontBufferedRenderer`, and touch isolation.
- [**Catmull-Rom Splines & Anti-Spike Clamping**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/inking-engine/stroke-math-and-interpolation.md): Centripetal parameterization ($\alpha = 0.5$) and Bézier conversion.
- [**QuadTree & Lasso Selection**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/inking-engine/spatial-indexing-and-selection.md): $O(\log N)$ spatial partitioning and point-in-polygon tests.
- [**Undo/Redo & Inking History**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/inking-engine/undo-redo-state-stack.md): Action recording and multi-page state management.

#### B. PDF & Document Subsystem
- [**PdfRendererManager & BitmapLruCache**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/pdf-subsystem/pdf-renderer-and-cache.md): Native `PdfRenderer` concurrency and 25% heap cache.
- [**MatrixTransformUtils & ViewportState**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/pdf-subsystem/matrix-transformations.md): Bidirectional Screen $\leftrightarrow$ Document $\leftrightarrow$ Page affine matrices.
- [**PdfExportService & Vector Blending**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/pdf-subsystem/pdf-export-service.md): Multi-layer compositing and A4 72 PPI vector generation.

#### C. Data Persistence & Storage Layer
- [**Room Entities & DocumentDao**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/data-and-persistence/room-database-architecture.md): Relational metadata, folder trees, and RecycleBin.
- [**BinaryStrokeSerializer Specification**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/data-and-persistence/binary-stroke-serializer.md): Packed binary format with `0x4D4C3553` magic header.
- [**NoteFileSystemRepository & Sandboxed IO**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/data-and-persistence/note-filesystem-repository.md): Private internal storage and atomic rename writes.

#### D. Presentation & Document Intelligence
- [**MultiPageEditorViewModel & State Coordination**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/intelligence-and-editor-ui/multipage-editor-viewmodel.md): MVI state flows and debounced autosave.
- [**Shape Detection & Document Scanner**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/intelligence-and-editor-ui/ml-shape-and-scanner.md): ML Kit digital ink shape recognition and document scanning.
- [**Adaptive Toolbars & Interaction Design**](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/docs/wiki/deep-dive/intelligence-and-editor-ui/floating-and-top-toolbars.md): Ergonomic tablet toolbars and lasso actions.
