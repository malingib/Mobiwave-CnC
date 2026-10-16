package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.SupabaseAuthRepository
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    authRepository: SupabaseAuthRepository,
    onSignedIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("MobiWave-CnC", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Sign in with your organisation account.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Work email") },
            singleLine = true,
            enabled = !busy
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !busy
        )
        Spacer(Modifier.height(16.dp))
        if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        authRepository.signIn(email, password)
                        onSignedIn()
                    } catch (e: Exception) {
                        error = e.message ?: "Sign-in failed. Please try again."
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && email.isNotBlank() && password.isNotBlank()
        ) {
            if (busy) CircularProgressIndicator() else Text("Sign in")
        }
        Spacer(Modifier.height(12.dp))
        if (!authRepository.configured()) {
            Text(
                "App configuration is incomplete. Configure the Supabase URL and publishable key in the build secrets before signing in.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "Access is controlled by your organisation's Supabase account and assigned permissions.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = { password = ""; email = ""; error = null }, enabled = !busy) {
            Text("Clear fields")
        }
    }
}
