package com.example.sapp.ui

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
import kotlinx.coroutines.launch
import com.example.sapp.R
import com.example.sapp.ui.state.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateToLogin: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("patient") } // default role

    val scope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsState()
    val regMsg = stringResource(R.string.registration_successful)

    // React to auth state changes (RegistrationState)
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.RegistrationSuccess -> {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(regMsg)
                    if (result == SnackbarResult.Dismissed || result == SnackbarResult.ActionPerformed) {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ Logo image
            Image(
                painter = painterResource(id = R.drawable.pills_375879),
                contentDescription = "App Logo",
                modifier = Modifier.size(96.dp)
            )

            Text(stringResource(R.string.create_account), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.please_fill_in_the_details_to_register), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth()
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

            // Role dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = role,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.role)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("patient") },
                        onClick = { role = "patient"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("caretaker") },
                        onClick = { role = "caretaker"; expanded = false }
                    )
                }
            }

            val fieldMsg = stringResource(R.string.please_fill_in_all_fields)
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(fieldMsg)
                        }
                    } else {
                        viewModel.register(name, email, password, role)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sign_up))
            }

            TextButton(onClick = onNavigateToLogin) {
                Text(stringResource(R.string.already_have_an_account))
            }

            if (authState is AuthState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

