# Project Overview & Architecture

- **Application**: Mal5odha Notes (Native Android)
- **Primary Package**: `com.mal5odha.app` / `com.mal5odha.core`
- **Target Platforms**: Android Tablets & Phones (API 26+)

---

## 1. High-Level Vision

Mal5odha is a native Android handwriting, PDF annotation, and document management platform engineered to deliver the responsive, fluid feel of physical pen on paper while leveraging modern digital capabilities:

- **Ultra-Responsive Stylus Inking**: Sub-$10\text{ ms}$ latency powered by `CanvasFrontBufferedRenderer` and motion prediction.
- **Multi-Page Vector Documents**: Multi-page note notebooks supporting custom paper backgrounds (blank, lined/ruled, grid, dotted) and PDF overlays.
- **High-DPI PDF Annotation**: Native Android PDF rasterization with zero-lag tile caching and vector PDF export.
- **Camera Document Scanner**: Integrated ML Kit scanner for auto-edge detection, perspective rectification, and document digitization.
- **Robust Storage Architecture**: Hybrid persistence utilizing Room SQLite for metadata and compact atomic binary streams for vector ink.

---

## 2. System Architecture Layers

The codebase adheres strictly to Clean Architecture and MVI principles:

```
┌─────────────────────────────────────────────────────────────┐
│                       Presentation Layer                    │
│  - Jetpack Compose & Material 3 UI Components               │
│  - MultiPageEditorScreen, DashboardScreen, ScannerScreen    │
│  - DrawingSurface (SurfaceView Low-Latency Inking)          │
│  - ViewModels (MultiPageEditorViewModel, DashboardViewModel)│
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow & User Intents
┌──────────────────────────────▼──────────────────────────────┐
│                         Core Engines                        │
│  - Digital Inking Engine (Splines, QuadTree, Eraser, Undo)  │
│  - PDF Rendering Engine (PdfRendererManager, BitmapCache)   │
│  - Math & Geometry (MatrixTransformUtils, ViewportState)    │
│  - Document Intelligence (ML Kit Shape Detection)           │
└──────────────────────────────┬──────────────────────────────┘
                               │ Repositories & Data Streams
┌──────────────────────────────▼──────────────────────────────┐
│                      Data & Storage Layer                   │
│  - Room SQLite Database (DocumentEntity, DocumentDao)       │
│  - Binary Stroke Serializer (BinaryStrokeSerializer)        │
│  - Sandboxed Filesystem IO (NoteFileSystemRepositoryImpl)   │
└─────────────────────────────────────────────────────────────┘
```
