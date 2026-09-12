package com.mal5odha.app.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Editor : Screen("editor/{documentId}") {
        fun createRoute(documentId: String) = "editor/$documentId"
    }
    object Documents : Screen("documents")
    object FolderContents : Screen("folder/{folderId}") {
        fun createRoute(folderId: String) = "folder/$folderId"
    }
    object RecycleBin : Screen("recycle_bin")
    object PdfImport : Screen("pdf_import")
    object Scanner : Screen("scanner")
    
    // Auth Flow
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object TwoFactorSetup : Screen("two_factor")
    
    // User Flow
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
}
