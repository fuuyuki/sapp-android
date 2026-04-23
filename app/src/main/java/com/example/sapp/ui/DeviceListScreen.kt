package com.example.sapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sapp.data.model.DeviceOut

data class Device(
    val name: String,
    val status: String,
    val lastSeen: String
)

@Composable
fun DeviceListScreen(
    devices: List<DeviceOut>,
    onDeviceClick: (Device) -> Unit
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(devices) { device ->
                DeviceCard(device, onClick = { onDeviceClick(device) })
            }
        }
    }
}

private fun LazyItemScope.onDeviceClick(p1: DeviceOut) {}

@Composable
fun DeviceCard(device: DeviceOut, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text("Last seen: ${device.last_seen}", style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(device.status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "online" -> MaterialTheme.colorScheme.primary
        "offline" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    AssistChip(
        onClick = {},
        label = { Text(status) },
        leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(containerColor = color)
    )
}
