package com.example.sapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.messaging.FirebaseMessaging
import com.example.sapp.data.model.ScheduleOut
import com.example.sapp.data.network.RetrofitClient
import com.example.sapp.data.network.dataStore
import com.example.sapp.data.repository.AppRepository
import com.example.sapp.ui.*
import com.example.sapp.ui.LoginScreen
import com.example.sapp.ui.RegisterScreen
import com.example.sapp.ui.DeviceListScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sapp.ui.theme.MedicalAppTheme


class MainActivity : ComponentActivity() {
    private val tokenKey = stringPreferencesKey("jwt_token")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiService = RetrofitClient.getApiService(this)
        val repository = AppRepository(apiService)
        val viewModel = MainViewModel(repository)

        setContent {
            MedicalAppTheme{
                val isLoggedIn by viewModel.loginSuccess.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val adherenceSummary by viewModel.adherenceSummary.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var currentScreen by remember { mutableStateOf("login") }
                var currentSubScreen by remember { mutableStateOf("dashboard") }
                var scheduleToEdit by remember { mutableStateOf<ScheduleOut?>(null) }

                // Permission handling for Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(
                            context,
                            "Notification permission denied. Alerts will not be shown.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Register FCM token once user is logged in
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                Log.d("FCM", "FCM Device Token: $token")
                                viewModel.registerFcmToken(token)
                            }
                        }
                    }
                }

                LaunchedEffect(errorMessage) {
                    errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        viewModel.errorMessage.value = null
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val snackbarHostState = remember { SnackbarHostState() }

                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "dashboard" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLogin = { email, pass ->
                                    viewModel.login(email, pass) { token ->
                                        scope.launch { dataStore.edit { it[tokenKey] = token } }
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") },
                                snackbarHostState = snackbarHostState
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegister = { name, email, pass, role ->
                                    viewModel.register(name, email, pass, role) {
                                        navController.navigate("login") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Registration successful!")
                                        }
                                    }
                                },
                                onNavigateToLogin = { navController.navigate("login") },
                                snackbarHostState = snackbarHostState
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                user = userProfile,
                                adherence = adherenceSummary,
                                onNavigateToDevices = { navController.navigate("devices") },
                                onNavigateToSchedules = { navController.navigate("schedules") },
                                onNavigateToMedlogs = { navController.navigate("medlogs") },
                                onNavigateToAddMeds = {
                                    scheduleToEdit = null
                                    navController.navigate("add_meds")
                                },
                                onLogout = {
                                    scope.launch {
                                        dataStore.edit { it.remove(tokenKey) }
                                        viewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("devices") {
                            val devicesList by viewModel.devices.collectAsState()
                            DeviceListScreen(
                                devices = devicesList,
                                onDeviceClick = { /* handle device click */ }
                            )
                        }

                        composable("schedules") {
                            val schedules by viewModel.schedules.collectAsState()
                            ScheduleScreen(
                                schedules = schedules,
                                onBack = { navController.popBackStack() },
                                onRefresh = { viewModel.refreshDashboard() },
                                onEdit = { schedule ->
                                    scheduleToEdit = schedule
                                    navController.navigate("add_meds")
                                },
                                onDelete = { scheduleId -> viewModel.deleteSchedule(scheduleId) }
                            )
                        }

                        composable("medlogs") {
                            val logs by viewModel.medlogs.collectAsState()
                            MedicationLogsScreen(
                                logs = logs,
                                onBack = { navController.popBackStack() },
                                onRefresh = { viewModel.refreshDashboard() }
                            )
                        }

                        composable("add_meds") {
                            AddMedicationScreen(
                                initialSchedule = scheduleToEdit,
                                onBack = { navController.popBackStack() },
                                onSave = { name, time, repeat ->
                                    if (scheduleToEdit == null) {
                                        viewModel.createSchedule(name, time, repeat) {
                                            navController.popBackStack()
                                            viewModel.refreshDashboard()
                                        }
                                    } else {
                                        viewModel.updateSchedule(scheduleToEdit!!.id, name, time, repeat) {
                                            navController.popBackStack()
                                            viewModel.refreshDashboard()
                                        }
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

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun StatusBadge(status: String) {
    val color = if (status.lowercase() == "online") Color(0xFF4CAF50) else Color.Gray
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
