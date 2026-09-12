```markdown
# Native Android Note-Taking & PDF Engine: Implementation Plan

This implementation plan outlines the architecture, data models, and step-by-step roadmap to build a high-performance, low-latency digital inking and PDF annotation app using 100% native Kotlin and Jetpack Compose.

---

## 1. Technical Stack & Dependencies

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Architecture:** Clean Architecture + MVI (Model-View-Intent)
* **Low-Latency Inking:** AndroidX Ink / `androidx.graphics.lowlatency.GLFrontBufferedRenderer`
* **PDF Rendering:** Native `android.graphics.pdf.PdfRenderer` or `Android-Pdfium` via JNI
* **Persistence:** Room Database (Metadata) + FlatBuffers / Protocol Buffers (Vector Stroke Data)
* **Dependency Injection:** Hilt / Dagger
* **Coroutines & Reactive State:** Kotlin Coroutines + `StateFlow` / `SharedFlow`

---

## 2. Core Architecture & Layered Diagram


```

┌───────────────────────────────────────────────────────────┐
│              Presentation Layer (Jetpack Compose)         │
│  - TopUnifiedToolbar (Compose)                            │
│  - ToolOptionModals & Contextual Popups                   │
│  - DocumentBrowserScreen & NoteEditorScreen               │
├───────────────────────────────────────────────────────────┤
│            Front-Buffered Drawing Layer (SurfaceView)     │
│  - GLFrontBufferedRenderer (Sub-15ms Inking & Prediction) │
│  - Active Stylus Stroke Interpolation (Catmull-Rom)       │
├───────────────────────────────────────────────────────────┤
│            Document & Geometry Engine (Kotlin / C++)      │
│  - QuadTree Spatial Indexer (Bounding Boxes, Eraser CSG)   │
│  - Gesture Recognition Engine (Flexcil-style Parser)      │
│  - PDF Tile Manager & Viewport Transformation Matrix      │
├───────────────────────────────────────────────────────────┤
│            Data & Storage Layer                           │
│  - Room Database (Notebooks, Pages, Bookmarks)            │
│  - Binary Stroke Serializer (Protobuf/FlatBuffers)        │
└───────────────────────────────────────────────────────────┘

```

---

## 3. Data Models & Vector Math Contracts

### 3.1 Stroke & Point Representation

```kotlin
data class StylusPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long
)

enum class ToolMode {
    PEN, HIGHLIGHTER, STROKE_ERASER, AREA_ERASER, LASSO, GESTURE
}

data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StylusPoint>,
    val color: Int,
    val baseWidth: Float,
    val toolMode: ToolMode,
    val boundingBox: RectF
)

```

### 3.2 QuadTree Spatial Indexing Interface

```kotlin
interface SpatialIndex {
    fun insert(stroke: Stroke)
    fun remove(strokeId: String)
    fun query(area: RectF): List<Stroke>
    fun clear()
}

```

---

## 4. Step-by-Step Implementation Roadmap

### Milestone 1: Project Setup & Core Presentation Shell

* [ ] Initialize Android project with Compose Material 3 and Gradle Version Catalogs (`libs.versions.toml`).
* [ ] Configure Hilt DI modules for database, repository, and viewport engine.
* [ ] Build the **Document Browser UI** with dark/light themes, sidebar navigation, tab filters (`All`, `PDF`, `Notes`, `Folders`), and document grid views.

### Milestone 2: Low-Latency Inking & Front-Buffered Renderer

* [ ] Implement `SurfaceView` interop with Compose via `AndroidView`.
* [ ] Integrate `GLFrontBufferedRenderer` and configure `MotionEventPredictor` to predict upcoming stylus points.
* [ ] Implement Catmull-Rom spline interpolation to smooth raw touch coordinates into continuous curves.
* [ ] Apply pressure-to-thickness dynamic scaling algorithms:

$$\text{Width} = \text{BaseWidth} \times \left( \alpha \cdot \text{Pressure} + (1 - \alpha) \cdot \frac{1}{\text{Velocity}} \right)$$



### Milestone 3: PDF Viewport & Tiled Matrix Transformation

* [ ] Create `ViewportState` managing zoom level, pan offsets, and screen-to-page matrix conversions.
* [ ] Implement asynchronous tiled PDF rendering using background coroutines to load visible page regions into memory.
* [ ] Build layer alignment so ink coordinates stay locked to exact PDF matrix locations under pinch-to-zoom.

### Milestone 4: Note Tools & Interaction Engine

* [ ] **Unified Top Toolbar (Compose):** Create a single-row toolbar with integrated long-press popups for pen customization, preset switching, and an overflow menu.
* [ ] **Lasso Selection:** Implement Ray-Casting Point-in-Polygon detection for selecting, moving, and scaling handwritten strokes.
* [ ] **Vector Eraser:**
* *Stroke Eraser:* Remove elements matching Axis-Aligned Bounding Box (AABB) intersection.
* *Segment/Pixel Eraser:* Split paths via boolean path clipping.


* [ ] **Flexcil-Style Gesture Mode:** Build a gesture interpreter detecting horizontal underlines (triggers PDF text selection) and enclosed circles/brackets (captures document clipping).

### Milestone 5: Binary Persistence & Export

* [ ] Define Protocol Buffers schemas for delta-compressed stroke arrays.
* [ ] Setup Room entities for folder hierarchies, document metadata, and page indices.
* [ ] Build background export workers to flatten vector strokes and PDF pages into standard PDF documents.
* [ ] Integrate automated instrumented tests for stylus touch latency and memory leak checks during heavy page panning.

```

```