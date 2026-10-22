package com.example.sapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import com.example.sapp.R
import com.example.sapp.data.model.MedlogOut
import com.example.sapp.ui.components.AdaptiveCardList
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppTopBar
import com.example.sapp.ui.components.EmptyState
import com.example.sapp.ui.components.StatusPill
import com.example.sapp.verification.MedlogMatcher
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Locale

@Composable
fun MedicationLogsScreen(
    logs: List<MedlogOut>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onVerifyLog: ((MedlogOut) -> Unit)? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.medication_logs),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.no_medlogs),
                    message = stringResource(R.string.no_medlogs_message)
                )
            }
        } else {
            val sortedLogs = logs.sortedByDescending { it.scheduled_time }
            AdaptiveCardList(
                items = sortedLogs,
                modifier = Modifier.padding(padding),
                minCardWidth = 340.dp
            ) { log ->
                val canVerify = onVerifyLog != null &&
                        log.evidence_key.isNullOrEmpty() &&
                        log.status.equals("taken", ignoreCase = true) &&
                        MedlogMatcher.isWithinWindow(log.scheduled_time, OffsetDateTime.now())
                val verifyAction: (() -> Unit)? = if (canVerify) {
                    onVerifyLog?.let { callback -> { callback(log) } }
                } else null
                MedlogCard(log = log, onVerify = verifyAction)
            }
        }
    }
}

@Composable
fun MedlogCard(log: MedlogOut, onVerify: (() -> Unit)? = null) {
    val isTaken = log.status.equals("taken", ignoreCase = true)
    val statusColor = if (isTaken) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val statusIcon = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Error

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = log.status,
                    tint = statusColor,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.pillname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatIsoDateTime(log.scheduled_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusPill(
                text = log.status.uppercase(),
                containerColor = statusColor.copy(alpha = 0.12f),
                contentColor = statusColor
            )
        }

        if (!log.evidence_key.isNullOrEmpty()) {
            Spacer(Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                val percent = log.verification_confidence
                    ?.let { " · ${(it.coerceIn(0.0, 1.0) * 100).toInt()}%" }
                    .orEmpty()
                Text(
                    text = stringResource(R.string.mouth_verified_label) + percent,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isTaken && onVerify != null) {
            Spacer(Modifier.size(10.dp))
            FilledTonalButton(
                onClick = onVerify,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.verify_with_camera))
            }
        }
    }
}

fun formatIsoDateTime(isoString: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(isoString)
        val displayFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
        if (date != null) displayFormat.format(date) else isoString
    } catch (_: Exception) {
        isoString
    }
}
