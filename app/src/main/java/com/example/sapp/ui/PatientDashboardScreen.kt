package com.example.sapp.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.example.sapp.data.model.AdherenceSummaryResponse
import com.example.sapp.data.model.UserOut
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    user: UserOut?,
    adherence: AdherenceSummaryResponse?,
    onNavigateToDevices: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToMedlogs: () -> Unit,
    onNavigateToConfirmMeds: () -> Unit,
    onLogout: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept back press
    BackHandler {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            scope.launch {
                snackbarHostState.showSnackbar("Press back again to exit")
            }
            scope.launch {
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SApp Health",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Profile */ }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToDevices,
                    icon = { Icon(Icons.Default.Devices, contentDescription = null) },
                    label = { Text("Devices") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToConfirmMeds,
                    icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    label = { Text("Confirm Meds") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMedlogs,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Logs") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            WelcomeBanner(user, currentDate)
            HealthSnapshot(adherence)
            ServicesList(
                onDevices = onNavigateToDevices,
                onSchedules = onNavigateToSchedules,
                onMedlogs = onNavigateToMedlogs
            )
        }
    }
}
