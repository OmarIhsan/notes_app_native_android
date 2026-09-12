---
name: "mobile-design"
description: "Mobile UX guidelines for touch gestures, palm rejection, contextual floating toolbars, and responsive tablet split views."
---

# Mobile & Tablet Design Guidelines

## 1. Ergonomics & Touch Gestures
- **Stylus vs Touch Differentiation:** Distinguish `MotionEvent.TOOL_TYPE_STYLUS` for drawing/inking from `MotionEvent.TOOL_TYPE_FINGER` for scrolling, zooming, and pan gestures.
- **Palm Rejection:** Suppress touch events with large touch major/minor diameters or classified as `FLAG_CANCELED` when a stylus is in active proximity.
- **Contextual Popups & Floating Menus:** Display long-press stroke menus and color selectors adjacent to the touch point without obscuring active viewport content.
