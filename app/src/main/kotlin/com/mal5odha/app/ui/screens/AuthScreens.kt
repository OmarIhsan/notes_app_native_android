package com.mal5odha.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mal5odha.app.ui.theme.PrimaryCyanBlue

@Composable
fun LoginScreen(
        onNavigateToRegister: () -> Unit,
        onLoginSuccess: () -> Unit,
        viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            onLoginSuccess()
        }
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                "Welcome to Mal5odha",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryCyanBlue
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    viewModel.clearError()
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.error != null) {
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading)
                    CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                    )
            else Text("Log In")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) { Text("Don't have an account? Register") }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.continueAsGuest() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !uiState.isLoading
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue as Guest")
        }
    }
}

@Composable
fun RegisterScreen(
        onNavigateToLogin: () -> Unit,
        onRegisterSuccess: () -> Unit,
        viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            onRegisterSuccess()
        }
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                "Create an Account",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryCyanBlue
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    viewModel.clearError()
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    viewModel.clearError()
                },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.error != null) {
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
                onClick = { viewModel.register(username, password, confirmPassword) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading)
                    CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                    )
            else Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Log In") }
    }
}

@Composable
fun ForgotPasswordScreen(onNavigateBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1: Input, 2: Success

    Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (step == 1) {
            Text(
                    "Reset Password",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryCyanBlue
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Enter your email address to receive a recovery link.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                    onClick = { if (email.contains("@")) step = 2 },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                    enabled = email.isNotEmpty()
            ) {
                Text("Send Recovery Link")
            }
        } else {
            Text(
                "Link Sent!",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryCyanBlue
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "If an account exists for $email, you will receive a password reset link shortly.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        TextButton(onClick = onNavigateBack) { Text("Back to Login") }
    }
}

@Composable
fun TwoFactorSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: TwoFactorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var verificationCode by remember { mutableStateOf("") }

    Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                "Two-Factor Setup",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryCyanBlue
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (uiState.step) {
            1 -> {
                Text(
                    "Enhance your security by enabling two-factor authentication. You will need a standard authenticator app (like Google Authenticator).",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.nextStep() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue)
                ) { Text("Get Started") }
            }
            2 -> {
                Text("Scan the QR code below or enter the secret key manually.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                // Mock QR Code placeholder
                Box(modifier = Modifier.size(150.dp).padding(8.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text("QR CODE", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Secret Key: ${uiState.secretKey ?: "Loading..."}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.nextStep() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                    enabled = uiState.secretKey != null
                ) { Text("I've Saved My Key") }
            }
            3 -> {
                Text("Enter the 6-digit code from your authenticator app to verify.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { if (it.length <= 6) verificationCode = it },
                    label = { Text("6-Digit Code") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (uiState.verificationError != null) {
                    Text(uiState.verificationError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.verifyCode(verificationCode) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue),
                    enabled = verificationCode.length == 6 && !uiState.isVerifying
                ) {
                    if (uiState.isVerifying) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Verify & Enable")
                }
            }
            4 -> {
                Text("Success!", style = MaterialTheme.typography.headlineSmall, color = PrimaryCyanBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Two-factor authentication is now enabled for your account.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyanBlue)
                ) { Text("Finish") }
            }
        }

        if (uiState.step < 4) {
            TextButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp)) { Text("Cancel") }
        }
    }
}
