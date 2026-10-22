package com.example.sapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import com.example.sapp.data.model.ScheduleOut
import com.example.sapp.ui.components.AdaptiveCardList
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppTopBar
import com.example.sapp.ui.components.EmptyState
import java.util.UUID

@Composable
fun ScheduleScreen(
    schedules: List<ScheduleOut>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: (ScheduleOut) -> Unit,
    onDelete: (UUID) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.schedules),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { padding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.no_schedules),
                    message = stringResource(R.string.no_schedules_message)
                )
            }
        } else {
            AdaptiveCardList(
                items = schedules,
                modifier = Modifier.padding(padding),
                minCardWidth = 340.dp
            ) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onEdit = { onEdit(schedule) },
                    onDelete = { onDelete(schedule.id) }
                )
            }
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: ScheduleOut,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val time = schedule.dose_time.substringAfter("T").substringBeforeLast(":")

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

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

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun formatRepeatDays(repeatDays: Int): String {
    if (repeatDays == 127) return "Setiap hari"
    if (repeatDays == 0) return "Does not repeat"

    val days = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    val selectedDays = mutableListOf<String>()
    days.forEachIndexed { index, day ->
        if ((repeatDays shr index) and 1 == 1) selectedDays.add(day)
    }
    return selectedDays.joinToString(", ")
}
