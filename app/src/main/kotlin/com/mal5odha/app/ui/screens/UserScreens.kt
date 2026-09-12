package com.mal5odha.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mal5odha.app.ui.theme.PrimaryCyanBlue
import com.mal5odha.app.ui.theme.SecondaryGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onNavigateToAuth: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val session by viewModel.currentSession.collectAsState()

    val formattedDate = remember(session?.createdAt) {
        session?.createdAt?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it))
        } ?: "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Credentials") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(96.dp),
                tint = if (session?.isGuest == true) MaterialTheme.colorScheme.tertiary else SecondaryGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (session?.isGuest == true) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Guest Session (Local Only)",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = session?.displayName ?: "Guest Student",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Guest Credentials",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryCyanBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CredentialRow(label = "Account Type", value = "Guest Session (Local Only)")
                        CredentialRow(label = "Guest ID", value = session?.userId ?: "Unknown")
                        CredentialRow(label = "Session Created", value = formattedDate)
                        CredentialRow(label = "Storage Scheme", value = "Hardware Encrypted Local")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Cloud Sync & Multi-Device Backup",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sign in or Create Account to sync across devices and access your notes anywhere.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.logout()
                                onNavigateToAuth()
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue)
                        ) {
                            Text("Sign in or Create Account")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.logout()
                        onLogoutSuccess()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("End Guest Session")
                }
            } else {
                val currentSession = session
                val displayName = currentSession?.displayName ?: "Student"
                val email = currentSession?.email ?: "user@example.com"
                val userId = currentSession?.userId ?: "usr_unknown"
                val rawToken = currentSession?.token
                val maskedToken = rawToken?.let {
                    if (it.length > 12) "${it.take(6)}...${it.takeLast(4)}" else it
                } ?: "None"

                Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Pro / Verified Account",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Active Session Credentials",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryCyanBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CredentialRow(label = "Display Name", value = displayName)
                        CredentialRow(label = "Email Address", value = email)
                        CredentialRow(label = "User ID", value = userId)
                        CredentialRow(label = "Auth Token", value = maskedToken)
                        CredentialRow(label = "Session Created", value = formattedDate)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.logout()
                        onLogoutSuccess()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Edit Profile coming soon.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAuth: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val session by viewModel.currentSession.collectAsState()

    val formattedDate = remember(session?.createdAt) {
        session?.createdAt?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Account & Session State
            Text(
                "Account & Session",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryCyanBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = if (session?.isGuest == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (session?.isGuest == true) "Guest Session" else (session?.displayName ?: "Active User"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (session?.isGuest == true) "Guest ID: ${session?.userId}" else (session?.email ?: "Signed in"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (session?.isGuest == true)
                            "Account Type: Guest Session (Local Only)\nCreated: $formattedDate\nSign in or Create Account to sync across devices."
                        else
                            "Account Type: Authenticated User\nID: ${session?.userId}\nCreated: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (session?.isGuest == true) {
                            Button(
                                onClick = {
                                    viewModel.logout()
                                    onNavigateToAuth()
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue)
                            ) {
                                Text("Sign In / Upgrade", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.logout()
                                    onLogoutSuccess()
                                },
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("Exit Guest", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    viewModel.logout()
                                    onLogoutSuccess()
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Log Out", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp))

            // Appearance Section
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryCyanBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Theme Mode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Choose whether the app follows system dark mode settings or forces a specific appearance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeModeOptionCard(
                    modifier = Modifier.weight(1f),
                    title = "System Default",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = themeMode == com.mal5odha.core.data.preferences.ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(com.mal5odha.core.data.preferences.ThemeMode.SYSTEM) }
                )
                ThemeModeOptionCard(
                    modifier = Modifier.weight(1f),
                    title = "Light Mode",
                    icon = Icons.Default.LightMode,
                    isSelected = themeMode == com.mal5odha.core.data.preferences.ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(com.mal5odha.core.data.preferences.ThemeMode.LIGHT) }
                )
                ThemeModeOptionCard(
                    modifier = Modifier.weight(1f),
                    title = "Dark Mode",
                    icon = Icons.Default.DarkMode,
                    isSelected = themeMode == com.mal5odha.core.data.preferences.ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(com.mal5odha.core.data.preferences.ThemeMode.DARK) }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp))
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryCyanBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Malhodha Academic Note-Taking",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
