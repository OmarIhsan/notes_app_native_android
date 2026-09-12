---
name: "android-jetpack-compose"
description: "Best practices for Jetpack Compose, Material Design 3, MVI state flow management, recomposition stabilization, and SurfaceView interop."
---

# Android Jetpack Compose & MVI Best Practices

## 1. Unidirectional Data Flow & State Stability
- **Immutable State:** Expose UI state strictly through immutable `StateFlow<UiState>` from ViewModels.
- **Dispatchers:** State mutations and navigation callbacks must be emitted on `Dispatchers.Main` / `viewModelScope`.
- **Recomposition Guards:**
  - Avoid object allocations (lambdas, collections, matrix calculations) directly inside the composable body; wrap with `remember` or `derivedStateOf`.
  - Use stable keys in `LazyRow` / `LazyColumn` items (`key = { it.id }`).

## 2. SurfaceView & Canvas Interop
- In Compose, embedding a `SurfaceView` inside `AndroidView` punches a hole through the view hierarchy.
- To prevent solid black backgrounds:
  - Configure `surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)`.
  - Call `surfaceView.setZOrderOnTop(true)`.
  - Explicitly clear buffers using `canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)`.
  - Ensure the parent composable provides an explicit background container (`Box(Modifier.background(Color.White))`).
