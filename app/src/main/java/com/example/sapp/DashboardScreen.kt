package com.example.sapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sapp.data.model.AdherenceSummaryResponse
import com.example.sapp.data.model.UserOut
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: UserOut?,
    adherence: AdherenceSummaryResponse?,
    onNavigateToDevices: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToMedlogs: () -> Unit,
    onNavigateToAddMeds: () -> Unit,
    onLogout: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SApp Health", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Navigate to Profile Screen */ }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Adherence Section
            AdherenceOverview(adherence)

            Text(
                text = "Services",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val dashboardItems = listOf(
                DashboardItem("My Devices", Icons.Default.Devices, MaterialTheme.colorScheme.primary, onNavigateToDevices),
                DashboardItem("Schedules", Icons.Default.Schedule, MaterialTheme.colorScheme.tertiary, onNavigateToSchedules),
                DashboardItem("Medlogs", Icons.Default.History, MaterialTheme.colorScheme.secondary, onNavigateToMedlogs),
                DashboardItem("Add Meds", Icons.Default.AddCircle, MaterialTheme.colorScheme.errorContainer, onNavigateToAddMeds)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                items(dashboardItems) { item ->
                    DashboardCard(item)
                }
            }
        }
    }
}

@Composable
fun AdherenceOverview(adherence: AdherenceSummaryResponse?) {
    val weeklyAvg = adherence?.weekly_adherence?.toFloat() ?: 0f
    val adherenceColor = when {
        weeklyAvg >= 0.8f -> MaterialTheme.colorScheme.primary
        weeklyAvg >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                CircularProgressIndicator(
                    progress = weeklyAvg,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = adherenceColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(weeklyAvg * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Weekly", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.width(24.dp))

            // Info Column
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(Icons.Default.Whatshot, "Streak", "${adherence?.adherence_streak ?: 0} Days")
                val nextDose = adherence?.next_dose?.next_dose ?: "--:--"
                InfoRow(Icons.Default.NotificationImportant, "Next Dose", nextDose)
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun DashboardCard(item: DashboardItem) {
    Surface(
        onClick = item.onClick,
        modifier = Modifier.height(110.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = item.color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(item.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}
