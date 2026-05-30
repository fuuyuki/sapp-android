package com.example.sapp

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.sapp.data.model.ScheduleOut
import com.example.sapp.data.network.RetrofitClient
import com.example.sapp.AppRepository
import com.example.sapp.ui.*
import com.example.sapp.ui.LoginScreen
import com.example.sapp.ui.RegisterScreen
import com.example.sapp.ui.DeviceListScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sapp.ui.state.AuthState
import com.example.sapp.ui.theme.MedicalAppTheme
import java.util.*


//class MainActivity : ComponentActivity() {
class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiService = RetrofitClient.getApiService(this)
        val repository = AppRepository(apiService)
        val viewModel = MainViewModel(repository, this)
//        val viewModel = androidx.lifecycle.ViewModelProvider(this, MainViewModelFactory(repository, this))[MainViewModel.class.java]

        setContent {
            MedicalAppTheme{
                val authState by viewModel.authState.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val selectedPatient by viewModel.selectedPatient.collectAsState() // Single patient
                val patientList by viewModel.patients.collectAsState() // The list of all patients
                val adherenceSummary by viewModel.adherenceSummary.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val userRole by viewModel.userRole.collectAsState()
                val userId by viewModel.userId.collectAsState()
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                var scheduleToEdit by remember { mutableStateOf<ScheduleOut?>(null) }

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                var showLogoutDialog by remember { mutableStateOf(false) }

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
                    val startScreen = intent.getStringExtra("OPEN_SCREEN")

                    if (startScreen == "medlogs") {
                        navController.navigate("medlogs")
                    }
                }

                if (showLogoutDialog) {
                    LogoutConfirmationDialog(
                        onConfirm = {
                            showLogoutDialog = false
                            // Actual logout logic here
                            viewModel.logout()
                            // Navigate back to login screen
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        },
                        onDismiss = { showLogoutDialog = false }
                    )
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
                    NavHost(
                        navController = navController,
                        startDestination = if (authState is AuthState.Success) "dashboard" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                viewModel = viewModel,
                                navController = navController,
                                onNavigateToRegister = { navController.navigate("register") },
                                snackbarHostState = snackbarHostState
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                viewModel = viewModel,
                                navController = navController,
                                onNavigateToLogin = { navController.navigate("login") },
                                snackbarHostState = snackbarHostState
                            )
                        }

                        composable("dashboard") {
                            // Determine which dashboard to show based on role
                            when (userRole) {
                                "caretaker" -> {
                                    // Initialize the Caretaker ViewModel
                                    val caretakerViewModel = remember { CaretakerDashboardViewModel(repository) }

                                    CaretakerDashboardScreen(
                                        user = userProfile,
                                        caretakerId = userId ?: UUID.randomUUID(),
                                        mainViewModel = viewModel,
                                        viewModel = caretakerViewModel,
                                        onNavigateToPatientDetails = { patient ->
                                            navController.navigate("patient_details/${patient.id}")
                                        },
                                        onNavigateToAddMeds = { patientId ->
                                            navController.navigate("add_meds/$patientId")
                                        },
                                        onLogout = { showLogoutDialog = true }
                                    )
                                }
                                else -> {
                                    // Default to Patient Dashboard
                                    PatientDashboardScreen(
                                        user = userProfile,
                                        adherence = adherenceSummary,
                                        viewModel = viewModel,
                                        onNavigateToDevices = { navController.navigate("devices") },
                                        onNavigateToSchedules = { navController.navigate("schedules") },
                                        onNavigateToMedlogs = { navController.navigate("medlogs") },
                                        onNavigateToConfirmMeds = { navController.navigate("confirm_meds") },
                                        onLogout = { showLogoutDialog = true }
                                    )
                                }
                            }
                        }

                        composable(
                            route = "patient_details/{patientId}",
                            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable

                            // Use your ViewModel to load patient data
                            LaunchedEffect(patientId) {
                                viewModel.loadPatientData(UUID.fromString(patientId))
                            }

                            val medlogsList by viewModel.medlogs.collectAsState()

                            // Fixed: Use selectedPatient (UserOut?) instead of the list
                            selectedPatient?.let { singlePatient ->
                                PatientDetailsScreen(
                                    patient = singlePatient, // Fixed: now passing UserOut
                                    adherence = adherenceSummary,
                                    devices = viewModel.devices.collectAsState().value,
                                    schedules = viewModel.schedules.collectAsState().value,
                                    medlogs = medlogsList,
                                    onNavigateBack = { navController.popBackStack() },
                                    onDeleteSchedule = { scheduleId ->
                                        viewModel.deleteSchedule(scheduleId)
                                    },
                                    onEditSchedule = { schedule ->
                                        // Logic to open edit screen (AddMedicationScreen with initialSchedule)
                                        scheduleToEdit = schedule
                                        navController.navigate("add_meds/${singlePatient.id}")
                                    }
                                )
                            } ?: Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        // Patient Selector
                        composable("patient_selector") {
                            val patients by viewModel.patients.collectAsState()

                            LazyColumn {
                                items(patients) { patient ->
                                    PatientCard(patient, onClick = {
                                        navController.navigate("add_meds/${patient.id}")
                                    })
                                }
                            }
                        }
                        // Add Meds to selected patient
                        // In MainActivity.kt inside NavHost

                        composable(
                            route = "add_meds/{patientId}",
                            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val patientIdString = backStackEntry.arguments?.getString("patientId") ?: return@composable
                            val patientId = UUID.fromString(patientIdString)

                            // Observe the devices list (which should be loaded by loadPatientData or a specific call)
                            val devices by viewModel.devices.collectAsState()

                            // Trigger loading devices if the list is empty
                            LaunchedEffect(patientId) {
                                viewModel.loadDevices(patientId, "patient")
                            }

                            AddMedicationScreen(
                                initialSchedule = scheduleToEdit,
                                onBack = { navController.popBackStack() },
                                onSave = { name, time, repeat ->
                                    // Use the first device ID found for this patient
                                    val validDeviceId = devices.firstOrNull()?.chip_id

                                    if (validDeviceId == null) {
                                        Toast.makeText(context, "This patient has no registered devices.", Toast.LENGTH_LONG).show()
                                        return@AddMedicationScreen
                                    }

                                    if (scheduleToEdit == null) {
                                        viewModel.createScheduleForPatient(patientId, name, time, repeat, validDeviceId) {
                                            navController.popBackStack()
                                            viewModel.loadPatientData(patientId)
                                        }
                                    } else {
                                        viewModel.updateSchedule(scheduleToEdit!!.id, name, time, repeat) {
                                            navController.popBackStack()
                                            viewModel.loadPatientData(patientId)
                                        }
                                    }
                                }
                            )
                        }


                        composable("devices") {
                            val devicesList by viewModel.devices.collectAsState()
                            DeviceListScreen(
                                devices = devicesList,
                                onDeviceClick = {
                                },
                                onBack = { navController.popBackStack() },
                                onRefresh = { viewModel.refreshDashboard() }
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

                        composable("confirm_meds") {
                            val context = LocalContext.current

                            // Permission state handler
                            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.RequestPermission()
                            ) { isGranted ->
                                if (!isGranted) {
                                    Toast.makeText(context, "Camera permission is required to verify medication", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }

                            // Request permission as soon as we enter this route
                            LaunchedEffect(Unit) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }

                            ConfirmMedicationScreen(
                                onBack = { navController.popBackStack() },
                                onConfirm = {
                                    // Logic for when the user clicks the shutter button
                                    navController.popBackStack()
                                    Toast.makeText(context, "Medication Verified!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                    }
                }
            }
        }
    }
}

class MainViewModelFactory(private val repo: AppRepository, private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repo, context) as T
    }
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Logout Confirmation") },
        text = { Text("Are you sure you want to logout?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Login")
            }
        }
    )
}

