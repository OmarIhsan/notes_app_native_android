package com.mal5odha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mal5odha.app.navigation.Screen
import com.mal5odha.app.ui.screens.AuthViewModel
import com.mal5odha.app.ui.screens.DashboardScreen
import com.mal5odha.app.ui.screens.MultiPageEditorScreen
import com.mal5odha.app.ui.theme.Mal5odhaTheme
import com.mal5odha.core.data.services.ErrorHandlingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var errorHandlingService: ErrorHandlingService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: com.mal5odha.app.ui.screens.SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()

            Mal5odhaTheme(themeMode = themeMode) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                    val startDestination =
                            if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route

                    com.mal5odha.app.ui.components.GlobalRecoveryDialog(
                            errorHandlingService = errorHandlingService
                    )

                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                    onNavigateToEditor = { docId ->
                                        navController.navigate(Screen.Editor.createRoute(docId))
                                    },
                                    onNavigateToScanner = {
                                        navController.navigate(Screen.Scanner.route)
                                    },
                                    onNavigateToFolder = { folderId ->
                                        navController.navigate(
                                                Screen.FolderContents.createRoute(folderId)
                                        )
                                    },
                                    onNavigateTo = { route ->
                                        navController.navigate(route)
                                    }
                            )
                        }
                        composable(Screen.Editor.route) { backStackEntry ->
                            val documentId = backStackEntry.arguments?.getString("documentId")
                            MultiPageEditorScreen(
                                documentId = documentId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Documents.route) {
                            com.mal5odha.app.ui.screens.DocumentsScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToEditor = { docId ->
                                        navController.navigate(Screen.Editor.createRoute(docId))
                                    },
                                    onNavigateToFolder = { folderId ->
                                        navController.navigate(Screen.FolderContents.createRoute(folderId))
                                    },
                                    onNavigateTo = { route ->
                                        navController.navigate(route)
                                    }
                            )
                        }
                        composable(Screen.FolderContents.route) { backStackEntry ->
                            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                            com.mal5odha.app.ui.screens.FolderContentsScreen(
                                    folderId = folderId,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToDocument = { docId ->
                                        navController.navigate(Screen.Editor.createRoute(docId))
                                    }
                            )
                        }
                        composable(Screen.RecycleBin.route) {
                            com.mal5odha.app.ui.screens.RecycleBinScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.PdfImport.route) {
                            com.mal5odha.app.ui.screens.PdfImportScreen(
                                    onNavigateToEditor = { documentId ->
                                        navController.navigate(
                                                Screen.Editor.createRoute(documentId)
                                        )
                                    }
                            )
                        }
                        composable(Screen.Scanner.route) {
                            com.mal5odha.app.ui.screens.ScannerScreen(onScanSuccess = { docId ->
                                navController.navigate(Screen.Editor.createRoute(docId)) {
                                    popUpTo(Screen.Scanner.route) { inclusive = true }
                                }
                            })
                        }

                        // Auth Flow
                        composable(Screen.Login.route) {
                            com.mal5odha.app.ui.screens.LoginScreen(
                                    onNavigateToRegister = {
                                        navController.navigate(Screen.Register.route)
                                    },
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                            )
                        }
                        composable(Screen.Register.route) {
                            com.mal5odha.app.ui.screens.RegisterScreen(
                                    onNavigateToLogin = {
                                        navController.navigate(Screen.Login.route)
                                    },
                                    onRegisterSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                            )
                        }
                        composable(Screen.ForgotPassword.route) {
                            com.mal5odha.app.ui.screens.ForgotPasswordScreen(
                                    onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.TwoFactorSetup.route) {
                            com.mal5odha.app.ui.screens.TwoFactorSetupScreen(
                                    onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // User Flow
                        composable(Screen.Profile.route) {
                            com.mal5odha.app.ui.screens.ProfileScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToAuth = {
                                        navController.navigate(Screen.Login.route)
                                    },
                                    onLogoutSuccess = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                            )
                        }
                        composable(Screen.EditProfile.route) {
                            com.mal5odha.app.ui.screens.EditProfileScreen()
                        }
                        composable(Screen.Settings.route) {
                            com.mal5odha.app.ui.screens.SettingsScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToAuth = {
                                        navController.navigate(Screen.Login.route)
                                    },
                                    onLogoutSuccess = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
