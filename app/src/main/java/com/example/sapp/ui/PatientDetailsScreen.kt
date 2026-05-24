package com.example.sapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sapp.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailsScreen(
    patient: UserOut,
    adherence: AdherenceSummaryResponse?,
    devices: List<DeviceOut>,
    schedules: List<ScheduleOut>,
    medlogs: List<MedlogOut>,
    onNavigateBack: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "${patient.name}'s Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ✅ Banner
            WelcomeBanner(patient, currentDate)

            // ✅ Adherence snapshot
            HealthSnapshot(adherence)

            // ✅ Devices list
            Text("Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (devices.isEmpty()) {
                Text("No devices assigned", style = MaterialTheme.typography.bodyMedium)
            } else {
                devices.forEach { device ->
                    DeviceCard_Patient(device)
                }
            }

            // ✅ Schedules list
            Text("Schedules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (schedules.isEmpty()) {
                Text("No schedules found", style = MaterialTheme.typography.bodyMedium)
            } else {
                schedules.forEach { schedule ->
                    ScheduleCard_Patient(schedule)
                }
            }

            // ✅ Medlogs list
            Text("Medication Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (medlogs.isEmpty()) {
                Text("No medication logs available", style = MaterialTheme.typography.bodyMedium)
            } else {
                medlogs.forEach { log ->
                    MedlogCard_Patient(log)
                }
            }
        }
    }
}

@Composable
fun DeviceCard_Patient(device: DeviceOut) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Chip ID: ${device.chip_id}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ScheduleCard_Patient(schedule: ScheduleOut) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Medication: ${schedule.pillname}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Time: ${schedule.dose_time}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MedlogCard_Patient(log: MedlogOut) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Medication: ${log.pillname}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Status: ${log.status}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

