---
name: "pdf"
description: "Guidelines and architecture for native Android PDF document handling, high-DPI rasterization, coordinate space transformations (Screen <-> Page Matrix), SAF stream caching, and lifecycle management."
---

# Android PDF Engine & Document Viewport Skill

## 1. Core Architecture Principles
- **SAF Stream Handling:** Always copy remote/content-provider `Uri` streams atomically into internal `cacheDir` and verify `file.length() > 0` before opening seekable `ParcelFileDescriptor`.
- **Thread Safety:** `PdfRenderer` is not thread-safe. All page open, render, and close calls must be guarded by a synchronized lock or serialized on a single-threaded dispatcher.
- **Rendering & Color Compositing:**
  - Never render PDF pages directly into uninitialized/transparent bitmaps. Always pre-fill destination bitmaps with solid white (`eraseColor(Color.WHITE)`).
  - Calculate destination bitmap dimensions dynamically using `page.width` and `page.height` multiplied by the device's display density factor ($1.5\times .. 3.0\times$) to prevent distortion across arbitrary aspect ratios.
- **Viewport Matrix Mapping:**
  - Maintain bidirectional matrix transformations:
    - Screen-to-Page: $M^{-1}$ maps stylus touch points into normalized PDF coordinate space for vector stroke persistence.
    - Page-to-Screen: $M$ maps document vectors back to screen pixels during zoom/pan operations.
