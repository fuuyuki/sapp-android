package com.example.sapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sapp.R
import com.example.sapp.data.model.AdherenceSummaryResponse
import com.example.sapp.data.model.DeviceOut
import com.example.sapp.data.model.MedlogOut
import com.example.sapp.data.model.ScheduleOut
import com.example.sapp.data.model.UserOut
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppTopBar
import com.example.sapp.ui.components.EmptyState
import com.example.sapp.ui.components.GradientHero
import com.example.sapp.ui.components.ScreenContainer
import com.example.sapp.ui.components.SectionHeader
import com.example.sapp.ui.components.StatusPill
import java.util.UUID

@Composable
fun PatientDetailsScreen(
    patient: UserOut,
    adherence: AdherenceSummaryResponse?,
    devices: List<DeviceOut>,
    schedules: List<ScheduleOut>,
    medlogs: List<MedlogOut>,
    onNavigateBack: () -> Unit,
    onDeleteSchedule: (UUID) -> Unit,
    onEditSchedule: (ScheduleOut) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = patient.name,
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        ScreenContainer(
            modifier = Modifier.padding(padding),
            maxWidth = 1040.dp
        ) {
            GradientHero(
                title = patient.name,
                subtitle = patient.email,
                icon = Icons.Default.AccountCircle
            )

            HealthSnapshot(adherence)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 760.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DevicesSection(devices)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SchedulesSection(
                                schedules = schedules,
                                onEditSchedule = onEditSchedule,
                                onDeleteSchedule = onDeleteSchedule
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DevicesSection(devices)
                        SchedulesSection(
                            schedules = schedules,
                            onEditSchedule = onEditSchedule,
                            onDeleteSchedule = onDeleteSchedule
                        )
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.medication_logs))
            if (medlogs.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.no_medlogs),
                    message = stringResource(R.string.no_medlogs_message)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    medlogs.forEach { log -> MedlogCard_Patient(log) }
                }
            }
        }
    }
}

@Composable
private fun DevicesSection(devices: List<DeviceOut>) {
    SectionHeader(title = stringResource(R.string.devices))
    if (devices.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Devices,
            title = stringResource(R.string.no_devices),
            message = stringResource(R.string.no_devices_message)
        )
    } else {
        devices.forEach { device -> DeviceCard_Patient(device) }
    }
}

@Composable
private fun SchedulesSection(
    schedules: List<ScheduleOut>,
    onEditSchedule: (ScheduleOut) -> Unit,
    onDeleteSchedule: (UUID) -> Unit
) {
    SectionHeader(title = stringResource(R.string.schedules))
    if (schedules.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Schedule,
            title = stringResource(R.string.no_schedules),
            message = stringResource(R.string.no_schedules_message)
        )
    } else {
        schedules.forEach { schedule ->
            ScheduleCard_Patient(
                schedule = schedule,
                onEdit = { onEditSchedule(schedule) },
                onDelete = { onDeleteSchedule(schedule.id) }
            )
        }
    }
}

@Composable
fun DeviceCard_Patient(device: DeviceOut) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.last_seen, formatLastSeen(device.last_seen)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ID: ${device.chip_id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusBadgeSmall(device.status)
        }
    }
}

@Composable
fun StatusBadgeSmall(status: String) {
    val (container, content) = when (status.lowercase()) {
        "online" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "offline" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    StatusPill(
        text = status.uppercase(),
        containerColor = container,
        contentColor = content
    )
}

@Composable
fun ScheduleCard_Patient(
    schedule: ScheduleOut,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val time = schedule.dose_time.substringAfter("T").substringBeforeLast(":")

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = time,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.pillname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stringResource(R.string.repeats)}: ${formatRepeatDays(schedule.repeat_days)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MedlogCard_Patient(log: MedlogOut) {
    val isTaken = log.status.equals("taken", ignoreCase = true)
    val statusColor = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val statusIcon = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Error

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = statusColor.copy(alpha = 0.12f), shape = CircleShape) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.pillname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.scheduled_for, formatIsoDateTime(log.scheduled_time)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadgeSmall(log.status)
        }
    }
}
