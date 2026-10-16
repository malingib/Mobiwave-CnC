package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AuthSession
import com.example.data.SupabaseAuthRepository
import com.example.ui.MobiWaveViewModel
import com.example.ui.screens.MainContainer
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.MobiWaveTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobiWaveTheme {
                val context = LocalContext.current
                val auth = remember(context) { SupabaseAuthRepository(context.applicationContext) }
                val scope = rememberCoroutineScope()
                val viewModel: MobiWaveViewModel = viewModel()
                var session by remember { mutableStateOf<AuthSession?>(null) }
                var checkingSession by remember { mutableStateOf(true) }

                LaunchedEffect(auth) {
                    session = auth.restoreSession()
                    checkingSession = false
                }
                LaunchedEffect(session?.accessToken, session?.userId) {
                    viewModel.setAuthenticatedSession(session?.accessToken, session?.userId)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        checkingSession -> Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text("Checking secure session", style = MaterialTheme.typography.bodyMedium)
                        }
                        session == null -> SignInScreen(authRepository = auth) {
                            scope.launch { session = auth.restoreSession() }
                        }
                        else -> MainContainer(
                            viewModel = viewModel,
                            signedInEmail = session?.email,
                            accessToken = session?.accessToken,
                            onSignOut = {
                                scope.launch {
                                    auth.signOut()
                                    session = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
