package com.example.sapp.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.sapp.R
import com.example.sapp.AppRepository
import com.example.sapp.data.model.UserOut
import com.example.sapp.ui.components.LanguageSelector
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*


class CaretakerDashboardViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _patients = MutableStateFlow<List<UserOut>>(emptyList())
    val patients: StateFlow<List<UserOut>> = _patients.asStateFlow()

    fun loadPatients(caretakerId: UUID) {
        viewModelScope.launch {
            try {
                val result = repository.getPatientsByCaretaker(caretakerId)
                _patients.value = result
            } catch (e: Exception) {
                // handle error (log, snackbar, etc.)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaretakerDashboardScreen(
    user: UserOut?,
    caretakerId: UUID,
    mainViewModel: MainViewModel,
    viewModel: CaretakerDashboardViewModel,
    onNavigateToPatientDetails: (UserOut) -> Unit,
    onNavigateToAddMeds: (UUID) -> Unit, // new callback
    onLogout: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    val patients by viewModel.patients.collectAsState()
    var showPatientSelector by remember { mutableStateOf(false) }
    val currentLang by mainViewModel.currentLanguage.collectAsState()

    // Load patients when screen starts
    LaunchedEffect(caretakerId) {
        viewModel.loadPatients(caretakerId)
    }

    Scaffold(
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
                    // ✅ Add the Language Selector
                    LanguageSelector(
                        currentLanguage = currentLang,
                        onLanguageChange = { lang -> mainViewModel.changeLanguage(lang) }
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPatients(caretakerId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Dashboard stays here */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.dashboard)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showPatientSelector = true },
                    icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    label = { Text(stringResource(R.string.add_medication)) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            WelcomeBanner(user, currentDate)

            Text(stringResource(R.string.your_patients), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(patients) { patient ->
                    PatientCard(patient, onClick = { onNavigateToPatientDetails(patient) })
                }
            }
        }

        // ✅ Patient selector dialog
        if (showPatientSelector) {
            AlertDialog(
                onDismissRequest = { showPatientSelector = false },
                title = { Text("Select Patient") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        patients.forEach { patient ->
                            TextButton(onClick = {
                                showPatientSelector = false
                                onNavigateToAddMeds(patient.id)
                            }) {
                                Text(patient.name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPatientSelector = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Composable
fun PatientCard(patient: UserOut, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(patient.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(patient.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

