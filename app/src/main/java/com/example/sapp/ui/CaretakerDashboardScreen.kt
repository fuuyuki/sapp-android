package com.example.sapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sapp.AppRepository
import com.example.sapp.R
import com.example.sapp.data.model.UserOut
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppWindowSize
import com.example.sapp.ui.components.DashboardTopBar
import com.example.sapp.ui.components.EmptyState
import com.example.sapp.ui.components.LanguageSelector
import com.example.sapp.ui.components.SectionHeader
import com.example.sapp.ui.components.rememberAppWindowSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CaretakerDashboardViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _patients = MutableStateFlow<List<UserOut>>(emptyList())
    val patients: StateFlow<List<UserOut>> = _patients.asStateFlow()

    fun loadPatients(caretakerId: UUID) {
        viewModelScope.launch {
            try {
                _patients.value = repository.getPatientsByCaretaker(caretakerId)
            } catch (_: Exception) {
                // Keep the previous list; the empty state explains the situation.
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
    onNavigateToAddMeds: (UUID) -> Unit,
    onCheckForUpdates: () -> Unit,
    onLogout: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    val patients by viewModel.patients.collectAsState()
    var showPatientSelector by remember { mutableStateOf(false) }
    val currentLang by mainViewModel.currentLanguage.collectAsState()
    val windowSize = rememberAppWindowSize()
    val wide = windowSize == AppWindowSize.Expanded

    LaunchedEffect(caretakerId) {
        viewModel.loadPatients(caretakerId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                navigationIcon = {
                    LanguageSelector(
                        currentLanguage = currentLang,
                        onLanguageChange = { lang -> mainViewModel.changeLanguage(lang) }
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPatients(caretakerId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
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
                CaretakerNavigationBar(
                    onHome = {},
                    onAddMeds = { showPatientSelector = true }
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
                CaretakerNavigationRail(
                    onHome = {},
                    onAddMeds = { showPatientSelector = true }
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
                    SectionHeader(
                        title = stringResource(R.string.your_patients),
                        subtitle = stringResource(R.string.patients_count, patients.size)
                    )
                    if (patients.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.People,
                            title = stringResource(R.string.no_patients),
                            message = stringResource(R.string.no_patients_message)
                        )
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val columns = if (maxWidth >= 720.dp) 2 else 1
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                patients.chunked(columns).forEach { rowPatients ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowPatients.forEach { patient ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                PatientCard(
                                                    patient = patient,
                                                    onClick = {
                                                        onNavigateToPatientDetails(patient)
                                                    }
                                                )
                                            }
                                        }
                                        repeat(columns - rowPatients.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPatientSelector) {
            AlertDialog(
                onDismissRequest = { showPatientSelector = false },
                title = { Text(stringResource(R.string.select_patient)) },
                text = {
                    if (patients.isEmpty()) {
                        Text(stringResource(R.string.no_patients))
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(patients) { patient ->
                                ListItem(
                                    headlineContent = { Text(patient.name) },
                                    supportingContent = { Text(patient.email) },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.People,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        showPatientSelector = false
                                        onNavigateToAddMeds(patient.id)
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPatientSelector = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun CaretakerNavigationBar(
    onHome: () -> Unit,
    onAddMeds: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onAddMeds,
            icon = { Icon(Icons.Default.Medication, contentDescription = null) },
            label = { Text(stringResource(R.string.add_medication)) }
        )
    }
}

@Composable
private fun CaretakerNavigationRail(
    onHome: () -> Unit,
    onAddMeds: () -> Unit
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
            selected = true,
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.dashboard)) }
        )
        NavigationRailItem(
            selected = false,
            onClick = onAddMeds,
            icon = { Icon(Icons.Default.Medication, contentDescription = null) },
            label = { Text(stringResource(R.string.add_medication)) }
        )
    }
}

@Composable
fun PatientCard(patient: UserOut, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Text(
                    text = initialsOf(patient.name),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = patient.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
