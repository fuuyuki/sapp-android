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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.sapp.worker.NotificationWorker
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.sapp.data.model.DeviceOut

class MainActivity : ComponentActivity() {
    private val tokenKey = stringPreferencesKey("jwt_token")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiService = RetrofitClient.getApiService(this)
        val repository = AppRepository(apiService)
        val viewModel = MainViewModel(repository)

        setContent {
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
                    Toast.makeText(context, "Notification permission denied. Alerts will not be shown.", Toast.LENGTH_LONG).show()
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

            // Start periodic work for notifications when logged in
            LaunchedEffect(isLoggedIn, userProfile) {
                if (isLoggedIn && userProfile != null) {
                    NotificationWorker.startPeriodicWork(context, userProfile!!.id)
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when {
                    !isLoggedIn -> {
                        if (currentScreen == "login") {
                            LoginScreen(
                                onLogin = { email, pass ->
                                    viewModel.login(email, pass) { token ->
                                        dataStore.edit { it[tokenKey] = token }
                                    }
                                },
                                onNavigateToRegister = { currentScreen = "register" }
                            )
                        } else {
                            RegisterScreen(
                                onRegister = { name, email, pass, role ->
                                    viewModel.register(name, email, pass, role) {
                                        currentScreen = "login"
                                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onNavigateToLogin = { currentScreen = "login" }
                            )
                        }
                    }
                    else -> {
                        when (currentSubScreen) {
                            "dashboard" -> DashboardScreen(
                                user = userProfile,
                                adherence = adherenceSummary,
                                onNavigateToDevices = {
                                    currentSubScreen = "devices"
                                    viewModel.refreshDashboard()
                                },
                                onNavigateToSchedules = {
                                    currentSubScreen = "schedules"
                                    viewModel.refreshDashboard()
                                },
                                onNavigateToMedlogs = {
                                    currentSubScreen = "medlogs"
                                    viewModel.refreshDashboard()
                                },
                                onNavigateToAddMeds = {
                                    scheduleToEdit = null
                                    currentSubScreen = "add_meds"
                                },
                                onLogout = {
                                    scope.launch {
                                        dataStore.edit { it.remove(tokenKey) }
                                        viewModel.logout()
                                        currentSubScreen = "dashboard"
                                    }
                                }
                            )
                            "devices" -> {
                                val devicesList by viewModel.devices.collectAsState()
                                DeviceListScreen(
                                    devices = devicesList,
                                    onBack = { currentSubScreen = "dashboard" },
                                    onRefresh = { viewModel.refreshDashboard() }
                                )
                            }
                            "schedules" -> {
                                val schedules by viewModel.schedules.collectAsState()
                                ScheduleScreen(
                                    schedules = schedules,
                                    onBack = { currentSubScreen = "dashboard" },
                                    onRefresh = { viewModel.refreshDashboard() },
                                    onEdit = { schedule ->
                                        scheduleToEdit = schedule
                                        currentSubScreen = "add_meds"
                                    },
                                    onDelete = { scheduleId -> viewModel.deleteSchedule(scheduleId) }
                                )
                            }
                            "medlogs" -> {
                                val logs by viewModel.medlogs.collectAsState()
                                MedicationLogsScreen(
                                    logs = logs,
                                    onBack = { currentSubScreen = "dashboard" },
                                    onRefresh = { viewModel.refreshDashboard() }
                                )
                            }
                            "add_meds" -> {
                                AddMedicationScreen(
                                    initialSchedule = scheduleToEdit,
                                    onBack = { currentSubScreen = "dashboard" },
                                    onSave = { name, time, repeat ->
                                        if (scheduleToEdit == null) {
                                            viewModel.createSchedule(name, time, repeat) {
                                                currentSubScreen = "dashboard"
                                                viewModel.refreshDashboard()
                                            }
                                        } else {
                                            viewModel.updateSchedule(scheduleToEdit!!.id, name, time, repeat) {
                                                currentSubScreen = "dashboard"
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
}

@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("$title Screen coming soon...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegister: (String, String, String, String) -> Unit, onNavigateToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val roles = listOf("patient", "caretaker")
    var expanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(roles[0]) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        TextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        // Role Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedRole.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            selectedRole = role
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(onClick = { onRegister(name, email, password, selectedRole) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Register")
        }
        TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Login")
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                }
            }
        )

        Button(onClick = { onLogin(email, password) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Login")
        }
        TextButton(onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? Register")
        }
    }
}

@Composable
fun DeviceListScreen(devices: List<DeviceOut>, onBack: () -> Unit, onRefresh: () -> Unit) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Your Devices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No devices found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices) { device ->
                        DeviceCard(device = device)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: DeviceOut) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Chip ID: ${device.chip_id}", style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(status = device.status)
        }
    }
}

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
