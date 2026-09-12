# The Transformation Journey: From Flutter to Native Android

Welcome to the team! As the Project Manager for this product, I want to walk you through our strategic pivot from our existing Flutter codebase to a purely Native Android (Kotlin) project. 

This document outlines our journey so far, why we made this hard decision, and how we need you, as our Senior Kotlin Developer, to architect and implement the new native version—starting from the UI up.

---

## 1. The Journey and The "Why"

We started our journey with Flutter to move fast and build a cross-platform MVP. We built a robust notes application (`notes_app_flutter`) that includes:
- Over 24 distinct screens (Home, Login, Multi-page Editor, PDF Import, Folders, Active Sessions).
- Complex custom widgets like Lasso tools, Selection bounds, TextBox widgets, and Formatting toolbars.
- State management utilizing typical Flutter patterns (Dart Providers) and deep custom painting for our document canvases.

**The Bottleneck:**
While Flutter allowed us to build the UI quickly, we hit a hard ceiling regarding our core value proposition: **writing and drawing experience**. 
To compete with top-tier apps like *Notewise* or *GoodNotes*, we need ultra-low latency ink, buttery-smooth zooming/scrolling on massive canvasses, and zero-compromise stylus support with predictive rendering. Flutter’s Impeller/Skia engine, while great for standard UI, introduces multi-frame latency for touch-to-screen pixel updates that we simply cannot overcome. We also need seamless integration with Google's ML Kit for shape/text recognition.

Therefore, we have decided to rewrite the core experience purely in Kotlin and Android Native.

---

## 2. Your Roadmap: Rebuilding the Native UI

As we migrate to `notes_app_native_android`, we want to adopt modern Android development best practices while avoiding the pitfalls we faced in our cross-platform attempt. Here is how we need you to tackle the UI implementation.

### A. The Declarative UI: Jetpack Compose
For 90% of the application interface, you will feel at home transitioning our Flutter concepts to **Jetpack Compose**. 
- **Mapping Concepts:** Flutter's `Widget` tree maps beautifully to `@Composable` functions. Our Flutter sidebars (`sidebar_profile_section.dart`), toolbars (`text_formatting_toolbar.dart`), and dialogs should all be rewritten purely in Jetpack Compose.
- **Theming:** We will use Compose Material 3. You'll need to translate our Flutter `ThemeData` into a robust Compose `ColorScheme` and `Typography` system.
- **Navigation:** Replace our Flutter Router with Jetpack Navigation Compose, managing our app's vast screen hierarchy (Auth flow -> Dashboard -> Editor -> Settings).

### B. The Core Canvas: Stepping Outside Compose
**This is the most critical part of your job.**
While Compose is fantastic for the Chrome (buttons, toolbars, menus), **you absolutely must NOT use Compose's native `Canvas` for the core document editor (`multi_page_editor_screen.dart`).** Compose's rendering pipeline still introduces too much overhead for high-performance, 120Hz stylus drawing.

Instead, you need to implement a hybrid approach:
1. **The Drawing Surface:** Build a highly optimized, custom `SurfaceView`.
2. **Rendering Pipeline:** Draw directly to the surface's hardware canvas on a dedicated background rendering thread. This bypasses the main UI thread entirely, ensuring ink flows instantly even if the UI thread is busy parsing menus.
3. **Jetpack Compose Integration:** Wrap your custom `SurfaceView` inside an `AndroidView` composable so it effortlessly sits underneath our Compose toolbars and undo/redo buttons.
4. **Low-Latency APIs:** Utilize Android's custom stylus APIs (`MotionEvent` historical batched points) and `HardwareRenderer`/`Ink API` for stroke prediction to bring touch-to-glass latency down to single-digit milliseconds.

### C. Advanced UI Interactions
In the Flutter app, we faked a lot of complex interactions. In Android Native, leverage the platform:
- **Lasso & Selection (`lasso_tool_painter.dart` -> Native):** Implement this via custom touch interceptors on your `SurfaceView`.
- **Drag & Drop (`draggable_document_card.dart`):** Use native Android Drag and Drop APIs combined with Compose's `Modifier.dragAndDropSource/Target`.
- **PDF Rendering (`pdf_import_demo_screen.dart`):** We struggled with Flutter PDF packages. Now, you should utilize Android's native `PdfRenderer` API to generate bitmap tiles efficiently for massive documents, feeding them directly into our canvas view.

---

## 3. Architecture & State Management

To support our new Compose UI, we need a rock-solid architecture:

- **MVI / MVVM:** We want a strict Unidirectional Data Flow pattern. Migrate our Flutter `Providers` and `Services` into Kotlin `ViewModel` classes.
- **Reactive State:** Expose UI state to Compose via `StateFlow` and handle one-off UI events (like showing a dialog or snackbar) via `SharedFlow` or Compose `Channel` execution. 
- **Dependency Injection:** We will use `Hilt` to manage dependencies seamlessly across our ViewModels, Room databases, and external ML services.

---

## 4. Next Steps for You

1. **Review the existing Flutter Code:** Check out `lib/screens/multi_page_editor_screen.dart` and `lib/widgets/lasso_tool_painter.dart` to understand the feature parity required.
2. **Scaffold the Compose Theme and Navigation:** Setup the basic scaffolding for `notes_app_native_android`.
3. **Prototype the `SurfaceView` Canvas:** Before building the rest of the app, build a blank screen with a custom `SurfaceView` and prove we can achieve the zero-latency stylus drawing performance we are after. 
4. **Integrate ML Kit:** Prototype shape detection natively to replace our old logic.

I’m excited to see how you elevate this application’s performance and feel. Let me know if you need to review the Figma files or have API questions on the backend. 

Let's build something phenomenal.
