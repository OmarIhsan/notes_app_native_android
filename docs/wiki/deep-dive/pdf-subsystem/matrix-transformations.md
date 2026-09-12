# Viewport Matrix Transformations & Coordinate Spaces

- **File Path**: [`app/src/main/kotlin/com/mal5odha/core/pdf/viewport/MatrixTransformUtils.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/pdf/viewport/MatrixTransformUtils.kt#L10-L61)
- **State File**: [`app/src/main/kotlin/com/mal5odha/core/pdf/viewport/ViewportState.kt`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/pdf/viewport/ViewportState.kt#L15-L195)
- **Subsystem**: PDF & Document Subsystem
- **Primary Classes**: `ViewportState`, `MatrixTransformUtils`

---

## 1. Mathematical Formulation of Coordinate Spaces

In a note-taking application supporting arbitrary pan, pinch-to-zoom, rotation, and multi-page layouts, there are three distinct coordinate reference frames:

1. **Screen Space ($S$)**: Raw physical pixels on the Android display device ($[0, \text{screenWidth}] \times [0, \text{screenHeight}]$).
2. **Document / Viewport Space ($D$)**: The virtual unbounded 2D canvas containing all pages arranged horizontally or vertically.
3. **Normalized Page Space ($P$)**: Resolution-independent normalized coordinates ($[0.0, 1.0] \times [0.0, 1.0]$) local to an individual page.

```mermaid
graph LR
    Screen["Screen Space (Pixels)"] <--> |Matrix M (DocumentToScreen)<br/>Matrix M⁻¹ (ScreenToDocument)| Doc["Document Space (Virtual Canvas)"]
    Doc <--> |Normalized Bounds Translation| Page["Page Space ([0.0, 1.0])"]
```

---

## 2. Affine Transformation Matrices: $M$ and $M^{-1}$

The forward transformation matrix $M$ converts Document coordinates to Screen coordinates:

$$M = \begin{bmatrix} s & 0 & t_x \\ 0 & s & t_y \\ 0 & 0 & 1 \end{bmatrix}$$

Where:
- $s$: Current pinch zoom scale factor ($\text{minScale} = 0.5\text{f}, \text{maxScale} = 5.0\text{f}$).
- $t_x, t_y$: Pan translation offsets in screen pixels (`offsetX`, `offsetY`).

### 2.1 Inversion Matrix $M^{-1}$
To map digitizer touch events ($x_s, y_s$) back into document space:

$$\begin{bmatrix} x_d \\ y_d \\ 1 \end{bmatrix} = M^{-1} \begin{bmatrix} x_s \\ y_s \\ 1 \end{bmatrix}$$

In [`ViewportState.kt:44-52`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/app/src/main/kotlin/com/mal5odha/core/pdf/viewport/ViewportState.kt#L44-L52), this is updated atomically under synchronization:

```kotlin
private fun updateMatricesInternal() {
    synchronized(lock) {
        documentToScreenMatrix.reset()
        documentToScreenMatrix.postScale(scale, scale)
        documentToScreenMatrix.postTranslate(offsetX, offsetY)

        documentToScreenMatrix.invert(screenToDocumentMatrix)
    }
}
```

---

## 3. High-Performance Point & Stroke Batch Transformation

Stylus paths contain hundreds of coordinates. Calling individual matrix operations per point creates excessive JNI / wrapper overhead. `MatrixTransformUtils` converts points into flat arrays and applies vectorized batch mapping:

```kotlin
// MatrixTransformUtils.kt:17-36
fun transformStroke(stroke: Stroke, matrix: Matrix): Stroke {
    val pts = FloatArray(stroke.points.size * 2)
    stroke.points.forEachIndexed { index, p ->
        pts[index * 2] = p.x
        pts[index * 2 + 1] = p.y
    }

    matrix.mapPoints(pts) // Native bulk hardware transformation

    val transformedPoints = stroke.points.mapIndexed { index, p ->
        Point(
            x = pts[index * 2],
            y = pts[index * 2 + 1],
            pressure = p.pressure,
            timestamp = p.timestamp
        )
    }.toMutableList()

    return stroke.copy(points = transformedPoints)
}
```

---

## 4. Pinch-to-Zoom Focal Anchor Preservation

When a user pinches with two fingers around focal point $F = (f_x, f_y)$ on screen, the document content directly beneath the fingers must remain stationary:

$$\begin{aligned}
\Delta s &= \text{newScale} / \text{oldScale} \\
t_x' &= f_x - \Delta s \cdot (f_x - t_x) \\
t_y' &= f_y - \Delta s \cdot (f_y - t_y)
\end{aligned}$$

This mathematical anchor prevents document jumping or drifting during fluid two-finger gestures.
