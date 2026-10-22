package com.example.sapp.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sapp.R
import com.example.sapp.data.model.AdherenceSummaryResponse
import com.example.sapp.data.model.UserOut
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppTopBar
import com.example.sapp.ui.components.GradientHero
import com.example.sapp.ui.components.ScreenContainer
import com.example.sapp.ui.components.SectionHeader
import com.example.sapp.ui.components.ServiceTile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            scope.launch { snackbarHostState.showSnackbar("Press back again to exit") }
            scope.launch {
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "SApp Health",
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToDevices,
                    icon = { Icon(Icons.Default.Devices, contentDescription = null) },
                    label = { Text(stringResource(R.string.devices)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAddMeds,
                    icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    label = { Text(stringResource(R.string.add_medication)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMedlogs,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.medication_logs)) }
                )
            }
        }
    ) { padding ->
        ScreenContainer(
            modifier = Modifier.padding(padding),
            maxWidth = 1040.dp
        ) {
            WelcomeBanner(user, currentDate)
            HealthSnapshot(adherence)
            ServicesList(onNavigateToDevices, onNavigateToSchedules, onNavigateToMedlogs)
        }
    }
}

@Composable
fun WelcomeBanner(user: UserOut?, currentDate: String) {
    GradientHero(
        title = stringResource(R.string.welcome_user, user?.name ?: "???"),
        subtitle = currentDate,
        icon = Icons.Default.AccountCircle
    )
}

@Composable
fun HealthSnapshot(adherence: AdherenceSummaryResponse?) {
    val weeklyAvg = (adherence?.weekly_adherence?.toFloat() ?: 0f).coerceIn(0f, 1f)
    val adherenceColor = when {
        weeklyAvg >= 0.8f -> MaterialTheme.colorScheme.primary
        weeklyAvg >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    AppCard {
        SectionHeader(
            title = stringResource(R.string.health_snapshot),
            subtitle = stringResource(R.string.adherence_this_week)
        )
        Spacer(Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { weeklyAvg },
                    modifier = Modifier.size(104.dp),
                    strokeWidth = 10.dp,
                    color = adherenceColor,
                    trackColor = adherenceColor.copy(alpha = 0.14f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(weeklyAvg * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = adherenceColor
                    )
                    Text(
                        text = stringResource(R.string.adherence),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HealthMetric(
                    icon = Icons.Default.Whatshot,
                    label = stringResource(R.string.streak_days, adherence?.adherence_streak ?: 0),
                    tint = MaterialTheme.colorScheme.primary
                )
                HealthMetric(
                    icon = Icons.Default.Medication,
                    label = "${stringResource(R.string.next_dose)}: ${adherence?.next_dose?.next_dose ?: "--:--"}",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun HealthMetric(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = tint.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.padding(8.dp).size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ServicesList(onDevices: () -> Unit, onSchedules: () -> Unit, onMedlogs: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = stringResource(R.string.quick_actions))
        ServiceTile(
            title = stringResource(R.string.devices),
            subtitle = stringResource(R.string.devices_subtitle),
            icon = Icons.Default.Devices,
            onClick = onDevices
        )
        ServiceTile(
            title = stringResource(R.string.schedules),
            subtitle = stringResource(R.string.schedules_subtitle),
            icon = Icons.Default.Schedule,
            onClick = onSchedules
        )
        ServiceTile(
            title = stringResource(R.string.medication_logs),
            subtitle = stringResource(R.string.medlogs_subtitle),
            icon = Icons.Default.History,
            onClick = onMedlogs
        )
    }
}

@Composable
fun ServiceCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ServiceTile(title = title, subtitle = subtitle, icon = icon, onClick = onClick)
}
