package com.example.sapp.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import com.example.sapp.R
import com.example.sapp.ui.state.AuthState

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateToRegister: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val authState by viewModel.authState.collectAsState()

    // React to auth state changes (LoginState)
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.LoginSuccess -> {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        viewModel.registerFcmToken(token)
                    } else {
                        viewModel.setAuthError("Failed to get FCM token")
                    }
                }
            }
            AuthState.SessionReady -> {
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
            }
            else -> Unit
        }
    }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (authState) {
            is AuthState.Loading -> {
                // Fullscreen loading for startup/session check
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.session_check), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pills_375879),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(96.dp)
                    )

                    Text(
                        stringResource(R.string.welcome_back),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.login_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email_address)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    val fieldMsg = stringResource(R.string.please_fill_in_all_fields)
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(fieldMsg)
                                }
                            } else {
                                viewModel.login(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.sign_in))
                    }

                    TextButton(onClick = onNavigateToRegister) {
                        Text(stringResource(R.string.don_t_have_an_account))
                    }

                    if (authState is AuthState.LoggingIn) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

