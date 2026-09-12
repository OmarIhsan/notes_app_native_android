Project Tree
less than a minute ago

Review
Submit comment
Add a message...
Submit
Select text in the artifact to add a comment
🏢 Project Functional Architecture: Mal5odha
This document explains the Mal5odha Notes App's structure, focusing on its functional components, user-facing screens, and core background "engine" (services).

📱 1. User Interface & Screens
The visual layer that users interact with. Each screen specializes in a specific task.

🏠 Core Navigation
Component	Functionality
home_screen	Main Dashboard. The primary hub for browsing folders, notebooks, and scanned files. Features a split-view for quick navigation.
documents_screen	Advanced File Browser. Detailed grid or list view with cloud sync controls and sorting features.
folder_contents_screen	Provides a focused view of all items contained within a specific folder.
recycle_bin_screen	Handles data recovery and temporary deletion management.
✏️ Annotation & Editing Engine
Component	Functionality
pdf_import_demo_screen	Master Notebook/PDF Editor. The primary interactive canvas where users draw, add text, and annotate pages.
multi_page_editor_screen	Alternative specialized canvas optimized for multi-page notebook editing.
scanner_screen	An intelligent camera interface that detects document edges and captures physical paper as digital notes.
👤 Profile & Authentication
Component	Functionality
login_screen, register_screen, forgot_password_screen	Manage the primary user entry and onboarding flow.
two_factor_setup_screen	Secure configuration for multi-step account protection.
profile_screen, edit_profile_screen	Manage user data, storage quotas, subscription plans, and avatar settings.
settings_screen	Central menu for app defaults (e.g., drawing ink quality, auto-save settings, and backup frequency).
🚀 2. Core Functional Features
These blocks of logic provide the "power" to the app.

📚 Notebook Management
Code / Module	Capability
unified_document, document_page	Allow treating every file (Note or PDF) as a multi-page notebook with editable layers.
page_manager_service	Enables users to add, duplicate, reorder, or delete pages on the fly.
batch_operations_service	Powers the "Batch Select" system to move, copy, or delete hundreds of files at once.
✍️ Intelligent Drawing Engine
Code / Module	Capability
shape_detection_service	Digitally "fixes" hand-drawn circles, squares, and lines into perfect geometric shapes.
stroke_selection_service	Implements the Lasso Tool, enabling users to select and move ink strokes as objects.
undo_redo_manager	Tracks every stroke as a command, making every single action reversible.
☁️ Cloud & Synchronization
Code / Module	Capability
sync_service	Automatically uploads work to the cloud when the user has internet access.
document_api_service, auth_service	Handles secure communication with the backend for cloud storage and user verification.
🖼️ Document Intelligence
Code / Module	Capability
document_processor, document_scanner_service	Use computer vision to boost contrast, crop paper edges, and extract text (OCR).
pdf_background_service, pdf_export_service	Handle high-quality conversion between standard PDFs and our editable notebook format.
🎨 3. Design System & Themes
The visual "DNA" that keeps the app feeling premium and consistent.

Component	Role
app_theme	Global Stylesheet. Defines the typography hierarchy, brand colors, and standard component visuals (buttons, inputs).
AppColors
Specific "Light Mode" palette: Primary Cyan Blue, Action Dark Blue, and clean White backgrounds.
AppTextStyles
Structured fonts for H1 titles, H2 headers, and standard Body text.
dialog_animations	Ensures all popups and transitions use smooth, modern animations.
⚙️ 4. Services & Architecture (The Engine)
Hidden logic that ensures the app is fast, safe, and reliable.

💾 Data Lifecycle
unified_document_store: Persistent local database that saves work instantly as you draw (offline-first).
app_preferences_service: Remembers your specific app settings across sessions.
error_handling_service: Protects work during crashes and handles "Crash Recovery" sessions.
data_migration_service: Safely upgrades user data formats as the app evolves.
🛠️ UI Components (Widgets)
selectable_document_card, draggable_document_card: Reusable cards that react to long-press and drag-and-drop.
selection_action_bar: A contextual menu that appears when files are selected.
text_formatting_toolbar, undo_redo_toolbar: Dedicated toolbars for fonts, colors, and canvas control.
page_thumbnail_strip: The horizontal strip for navigating between notebook pages.
📦 File-by-File Technical Index
A detailed mapping of every specific code file to its primary purpose.

Path / File	For What?
main
Initializes all background engines (logger, preferences, error handling) and launches the app.
api_config	Defines the secure URLs for the app's backend servers.
unified_document.dart
The Core Data Model for every file you create/import.
document_page.dart
The Page-Level Model storing ink strokes and text for a single page.
drawing_command.dart
The logic for the Undo/Redo system.
shared_content_provider	Manages data for community-shared folders.
selection_provider	Manages the Multi-Select state of the file browser.
haptic_feedback_service	Controls the vibration feedback when you use tools.
thumbnail_generator_service	Creates the small preview images you see in the home grid.
lasso_tool_painter, selection_bounds_painter	Handle the visual drawing of the selection box and resize handles.
text_box_widget	Represents a floating, editable, and draggable text block on your notes.
export_options_dialog	The wizard for picking PDF/Image quality before sharing a document.
recovery_dialog	A safety feature that asks you to recover work if the app was closed unexpectedly.
secure_storage_service	Safely stores your encrypted login tokens on the device.
