package com.example.sapp.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sapp.R
import com.example.sapp.data.model.AdherenceSummaryResponse
import com.example.sapp.data.model.UserOut
import com.example.sapp.ui.components.AppWindowSize
import com.example.sapp.ui.components.DashboardTopBar
import com.example.sapp.ui.components.rememberAppWindowSize
import com.example.sapp.ui.components.LanguageSelector
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PatientDashboardScreen(
    user: UserOut?,
    adherence: AdherenceSummaryResponse?,
    viewModel: MainViewModel,
    onNavigateToDevices: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToMedlogs: () -> Unit,
    onNavigateToConfirmMeds: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onLogout: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentLang by viewModel.currentLanguage.collectAsState()
    val windowSize = rememberAppWindowSize()
    val wide = windowSize == AppWindowSize.Expanded

    BackHandler {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            scope.launch {
                snackbarHostState.showSnackbar("Tekan sekali lagi untuk keluar")
            }
            scope.launch {
                kotlinx.coroutines.delay(2000)
                backPressedOnce = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                navigationIcon = {
                    LanguageSelector(
                        currentLanguage = currentLang,
                        onLanguageChange = { lang -> viewModel.changeLanguage(lang) }
                    )
                },
                actions = {
                    IconButton(onClick = onCheckForUpdates) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = stringResource(R.string.update_check_for_updates)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.logout)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!wide) {
                PatientNavigationBar(
                    onNavigateToDevices = onNavigateToDevices,
                    onNavigateToConfirmMeds = onNavigateToConfirmMeds,
                    onNavigateToMedlogs = onNavigateToMedlogs
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (wide) {
                PatientNavigationRail(
                    onNavigateToDevices = onNavigateToDevices,
                    onNavigateToConfirmMeds = onNavigateToConfirmMeds,
                    onNavigateToMedlogs = onNavigateToMedlogs
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    WelcomeBanner(user, currentDate)

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth >= 760.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.weight(1.05f)) {
                                    HealthSnapshot(adherence)
                                }
                                Box(modifier = Modifier.weight(0.95f)) {
                                    ServicesList(
                                        onNavigateToDevices,
                                        onNavigateToSchedules,
                                        onNavigateToMedlogs
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                HealthSnapshot(adherence)
                                ServicesList(
                                    onNavigateToDevices,
                                    onNavigateToSchedules,
                                    onNavigateToMedlogs
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
private fun PatientNavigationBar(
    onNavigateToDevices: () -> Unit,
    onNavigateToConfirmMeds: () -> Unit,
    onNavigateToMedlogs: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToDevices,
            icon = { Icon(Icons.Default.Devices, contentDescription = null) },
            label = { Text(stringResource(R.string.devices)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToConfirmMeds,
            icon = { Icon(Icons.Default.Medication, contentDescription = null) },
            label = { Text(stringResource(R.string.confirm_meds)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToMedlogs,
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text(stringResource(R.string.medication_logs)) }
        )
    }
}

@Composable
private fun PatientNavigationRail(
    onNavigateToDevices: () -> Unit,
    onNavigateToConfirmMeds: () -> Unit,
    onNavigateToMedlogs: () -> Unit
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(12.dp))
        Image(
            painter = painterResource(id = R.drawable.pills_375879),
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(18.dp))
        NavigationRailItem(
            selected = false,
            onClick = onNavigateToDevices,
            icon = { Icon(Icons.Default.Devices, contentDescription = null) },
            label = { Text(stringResource(R.string.devices)) }
        )
        NavigationRailItem(
            selected = false,
            onClick = onNavigateToConfirmMeds,
            icon = { Icon(Icons.Default.Medication, contentDescription = null) },
            label = { Text(stringResource(R.string.confirm_meds)) }
        )
        NavigationRailItem(
            selected = false,
            onClick = onNavigateToMedlogs,
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text(stringResource(R.string.medication_logs)) }
        )
    }
}
