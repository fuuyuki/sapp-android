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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
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
import com.example.sapp.data.model.DeviceOut
import com.example.sapp.ui.components.AdaptiveCardList
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppTopBar
import com.example.sapp.ui.components.EmptyState
import com.example.sapp.ui.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class Device(
    val name: String,
    val status: String,
    val lastSeen: String
)

@Composable
fun DeviceListScreen(
    devices: List<DeviceOut>,
    onDeviceClick: (DeviceOut) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.devices),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { padding ->
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Devices,
                    title = stringResource(R.string.no_devices),
                    message = stringResource(R.string.no_devices_message)
                )
            }
        } else {
            AdaptiveCardList(
                items = devices,
                modifier = Modifier.padding(padding),
                minCardWidth = 320.dp
            ) { device ->
                DeviceCard(device = device, onClick = { onDeviceClick(device) })
            }
        }
    }
}

@Composable
fun DeviceCard(device: DeviceOut, onClick: () -> Unit) {
    val formattedDate = formatLastSeen(device.last_seen)

    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.last_seen, formattedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${device.chip_id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(device.status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
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

internal fun formatLastSeen(raw: String?): String {
    if (raw.isNullOrBlank()) return "Never"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date: Date? = parser.parse(raw)
        val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        date?.let { formatter.format(it) } ?: raw
    } catch (_: Exception) {
        try {
            val simpleParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = simpleParser.parse(raw)
            val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            date?.let { formatter.format(it) } ?: raw
        } catch (_: Exception) {
            raw
        }
    }
}
