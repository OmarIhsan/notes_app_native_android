---
name: "android-design-guidelines"
description: "Material Design 3 guidelines for stylus touch targets, unified toolbar design, responsive tablet layouts, and edge-to-edge window insets."
---

# Android Design Guidelines & Stylus Canvas UI

## 1. Stylus & Touch Target Sizing
- **Minimum Interactive Touch Targets:** $48 \times 48\,\text{dp}$ for finger touch and $40 \times 40\,\text{dp}$ for fine stylus tip targets.
- **Top Toolbar Ergonomics:**
  - Floating pill or unified top bar with elevation (`shadowElevation = 4.dp`, `tonalElevation = 2.dp`).
  - Clear visual affordances for active tool selection (accent outline / tint).
  - Prominent real-time page counter badge (`"${currentPage + 1} / $totalPages"`).

## 2. Edge-to-Edge & Window Insets
- Accommodate system status bars and display cutouts by padding toolbars with `WindowInsets.safeDrawing.asPaddingValues()`.
- Ensure drawing canvases extend full screen underneath transient toolbars for immersive inking.
