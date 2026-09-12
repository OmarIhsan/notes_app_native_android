# Zero-to-Hero Learning Path & Developer Handbook

- **Target Audience**: New engineers, mobile developers, and contributors
- **Prerequisites**: Basic understanding of mobile apps and object-oriented programming

---

## Part I: Technology Foundations & Cross-Language Comparisons

Mal5odha is built with modern native Android technologies. If you are coming from Python, React/Web, or iOS, here is how the core paradigms map:

### 1. Jetpack Compose vs React / Python Declarative UI
In Compose, UI is defined as composable functions that re-execute ("recompose") whenever observed state changes.

| Android / Kotlin | Web / React | Python (Streamlit / Flet) | Purpose |
|---|---|---|---|
| `@Composable fun MyWidget()` | `function MyWidget()` | `def my_widget():` | Declarative UI component |
| `val state by vm.uiState.collectAsState()` | `const [state] = useState()` | `state = get_state()` | Reactive UI state binding |
| `StateFlow<T>` / `SharedFlow<T>` | RxJS Observable / Store | `asyncio.Queue` / RxPy | Reactive asynchronous event streams |
| `viewModelScope.launch` | `useEffect(() => {})` | `asyncio.create_task()` | Structured concurrency lifecycle |

### 2. Low-Latency Graphics Architecture
Unlike standard UI components where the framework controls when pixels are drawn:
- **SurfaceView**: A dedicated drawing surface punched directly into the hardware window compositor.
- **CanvasFrontBufferedRenderer**: Renders directly to the hardware display buffer for sub-$10\text{ ms}$ latency before standard display composition.

---

## Part II: Codebase Architecture & Directory Map

```
notes_app_native_android/
├── app/src/main/
│   ├── kotlin/com/mal5odha/
│   │   ├── app/                    # Application Entry, Navigation & UI
│   │   │   ├── MainActivity.kt     # Single Activity host with Compose NavHost
│   │   │   ├── navigation/         # Type-safe navigation routes (Screen.kt)
│   │   │   ├── ui/components/      # Reusable widgets (Toolbars, Dialogs)
│   │   │   └── ui/screens/         # Full-screen views (Editor, Dashboard, Scanner)
│   │   └── core/                   # Shared Business Logic & Engines
│   │       ├── data/               # Room DB, Repositories, Serializers
│   │       ├── ink/                # Low-latency inking, Splines, QuadTree
│   │       └── pdf/                # PDF rendering, Tile Caching, Viewport
│   └── res/                        # Drawables, Vector Icons, Strings, Themes
└── gradle/
    └── libs.versions.toml          # Centralized Gradle dependency catalog
```

---

## Part III: Developer Setup & Daily Workflow

### 1. Prerequisites
- **JDK**: Java Development Kit 17 (recommended: Eclipse Temurin 17 or Android Studio bundled JDK).
- **IDE**: Android Studio Hedgehog (2023.1.1) or newer.
- **Android SDK**: Build tools 34.0.0, Platform API 34.

### 2. Common Gradle Tasks
Run these from the project root in terminal or PowerShell:

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install debug APK on connected tablet or emulator
./gradlew installDebug
```

---

## Appendices: Comprehensive 40+ Term Glossary

1. **AABB (Axis-Aligned Bounding Box)**: Rectangular spatial envelope used in QuadTree broadphase collision queries.
2. **ActivePageBounds**: The geometric boundary of the current page in document space, used for touch isolation.
3. **AndroidView**: Compose wrapper component used to embed platform `SurfaceView` within declarative Compose hierarchies.
4. **Anti-Spike Clamping**: Algorithm limiting Bézier control point displacement to half the chord distance to prevent cursive handwriting overshoots.
5. **BitmapLruCache**: Memory-bounded cache holding rendered PDF page bitmaps, capped at 25% of JVM heap.
6. **CanvasFrontBufferedRenderer**: AndroidX API enabling single-buffered direct hardware rendering for stylus.
7. **Catmull-Rom Spline**: A cubic interpolating spline passing through all control points with continuous $C^1$ velocity.
8. **Centripetal Parameterization**: Spline formulation with $\alpha = 0.5$ mathematically guaranteed to avoid self-intersection loops.
9. **Chord Distance**: Euclidean straight-line distance between two consecutive stylus sample points.
10. **Coroutines**: Lightweight, structured concurrent execution threads in Kotlin.
11. **Dagger Hilt**: Dependency injection framework generating compile-time dependency graphs.
12. **DocumentDao**: Room Data Access Object defining SQLite queries for notes, folders, and trash operations.
13. **DocumentEntity**: SQLite table row schema representing document metadata.
14. **DocumentPage**: Model representing a single page order, dimensions, and background type.
15. **DrawingSurface**: Custom `SurfaceView` implementing low-latency front-buffered inking callbacks.
16. **Front Buffer**: Hardware frame buffer displayed directly on glass, updated without waiting for VSYNC compositor passes.
17. **Historical MotionEvents**: Intermediate touch samples packaged within an Android `MotionEvent` between OS frame ticks.
18. **ISO 216 A4**: Standard page dimension (595 x 842 points at 72 PPI) used in PDF exports.
19. **InkTool**: Enumeration defining inking instruments (`PEN`, `HIGHLIGHTER`, `ERASER`, `LASSO`).
20. **Lasso Tool**: Tool allowing freeform encircling of vector strokes for grouped transformations.
21. **MatrixTransformUtils**: Mathematical helpers for affine mapping between Screen, Document, and Page spaces.
22. **ML Kit Digital Ink**: On-device machine learning library for shape classification and handwriting recognition.
23. **ML Kit Document Scanner**: Google Play Services scanner providing auto-cropping and perspective correction.
24. **Ml5Stroke**: Core data model representing a vector stroke consisting of points, color, width, and tool.
25. **MotionEventPredictor**: Machine-learning-based Kalman-filter predictor forecasting future stylus trajectory by 5–15ms.
26. **Multi-Buffer**: Double- or triple-buffered swapchain used for persistent display composition.
27. **MultiPageEditorViewModel**: State coordinator managing multi-page inking, autosave debouncing, and undo/redo stacks.
28. **Normalized Coordinates**: Coordinates normalized to $[0.0, 1.0]$, ensuring resolution independence.
29. **PageBackground**: Enum defining page types (`BLANK`, `PDF_PAGE`, `IMAGE`, `RULED`, `GRID`, `DOTTED`).
30. **ParcelFileDescriptor**: Android IPC file descriptor wrapper used to stream PDF documents securely.
31. **PdfDocument**: Android native class for generating vector-rich PDF files.
32. **PdfRenderer**: Native C++ PDF rasterization engine exposed by the Android OS framework.
33. **Point**: Inking coordinate with $(x, y)$ float positions, pressure scalar, and timestamp.
34. **QuadTree**: 2D spatial data structure recursively partitioning space into 4 quadrants for $O(\log N)$ search.
35. **RecycleBin**: Two-stage soft deletion mechanism preserving deleted documents until explicit permanent purging.
36. **Room**: Android Jetpack ORM abstraction layer over SQLite.
37. **StateFlow**: A state-holding observable flow that emits the current and new state updates to collectors.
38. **StrokeSelectionService**: Geometric engine calculating Ray-Casting point-in-polygon hits for lasso paths.
39. **UndoRedoManager**: State stack manager maintaining temporal undo and redo operations.
40. **UnifiedDocument**: Aggregate root model representing a complete note or imported PDF.
41. **ViewportState**: Affine transform manager handling zoom scale ($0.5\times - 5.0\times$) and pan translation offsets.
