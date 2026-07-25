package com.echoease.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val auth = remember { if (isPreview) null else FirebaseAuth.getInstance() }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "EchoEase",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Peaceful Living, Peer-to-Peer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                ) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "Please enter both email and password."
                        return@Button
                    }
                    val currentAuth = auth
                    if (currentAuth == null) {
                        if (isPreview) onAuthenticated()
                        return@Button
                    }
                    isLoading = true
                    error = null
                    scope.launch {
                        if (isSignUp) {
                            currentAuth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onAuthenticated()
                                    } else {
                                        error = task.exception?.localizedMessage ?: "Sign up failed."
                                    }
                                }
                        } else {
                            currentAuth.signInWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onAuthenticated()
                                    } else {
                                        error = task.exception?.localizedMessage ?: "Login failed. Check your password."
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.large
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isSignUp) "Create Account" else "Enter EchoEase", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { isSignUp = !isSignUp },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(
                    if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    val currentAuth = auth
                    if (currentAuth == null) {
                        if (isPreview) onAuthenticated()
                        return@TextButton
                    }
                    isLoading = true
                    error = null
                    currentAuth.signInAnonymously().addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            onAuthenticated()
                        } else {
                            error = "Anonymous login failed: ${task.exception?.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Continue as Guest", color = MaterialTheme.colorScheme.outline)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Tip: Ensure 'Email/Password' and 'Anonymous' providers are enabled in your Firebase Console.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    MaterialTheme {
        AuthScreen(onAuthenticated = {})
    }
}
