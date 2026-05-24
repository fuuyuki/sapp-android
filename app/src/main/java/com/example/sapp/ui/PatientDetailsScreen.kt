package com.example.sapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    onNavigateBack: () -> Unit,
    // ✅ Add these new callbacks
    onDeleteSchedule: (UUID) -> Unit,
    onEditSchedule: (ScheduleOut) -> Unit
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
                    ScheduleCard_Patient(
                        schedule = schedule,
                        onEdit = { onEditSchedule(schedule) },
                        onDelete = { onDeleteSchedule(schedule.id) }
                    )
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
    // 1. Parse the ISO date string (Same logic as DeviceListScreen)
    val formattedDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date: Date? = parser.parse(device.last_seen ?: "")

        val formatter = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
        date?.let { formatter.format(it) } ?: (device.last_seen ?: "Never")
    } catch (e: Exception) {
        try {
            val simpleParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = simpleParser.parse(device.last_seen ?: "")
            val formatter = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
            date?.let { formatter.format(it) } ?: device.last_seen
        } catch (e2: Exception) {
            device.last_seen ?: "Unknown"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Last seen: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "ID: ${device.chip_id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 2. Reuse the StatusBadge logic
            StatusBadgeSmall(device.status)
        }
    }
}

@Composable
fun StatusBadgeSmall(status: String) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "online" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "offline" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
    }

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScheduleCard_Patient(
    schedule: ScheduleOut,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Keeps everything aligned in a straight line
        ) {
            // ✅ Info Section (Takes up available space, pushing buttons to the right)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.pillname,
                    style = MaterialTheme.typography.titleMedium, // Matches Device name size
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ✅ Time Badge (Proportional to the Status Badge)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = try {
                            schedule.dose_time.substringAfter("T").substringBeforeLast(":")
                        } catch (e: Exception) {
                            schedule.dose_time
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ✅ Action Section (Grouped on the right)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp) // Standard icon button size
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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

