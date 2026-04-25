package com.example.sapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sapp.data.model.ScheduleOut
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    initialSchedule: ScheduleOut? = null,
    onBack: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var pillName by remember { mutableStateOf(initialSchedule?.pillname ?: "") }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val initialHour = initialSchedule?.dose_time?.substringAfter("T")?.substringBefore(":")?.toIntOrNull()
        ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialSchedule?.dose_time?.substringAfter(":")?.substringBefore(":")?.toIntOrNull()
        ?: calendar.get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(initialHour, initialMinute)

    val formattedTime = remember(timePickerState.hour, timePickerState.minute) {
        "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
    }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            initialSchedule?.repeat_days?.let { repeatDays ->
                daysOfWeek.indices.forEach { i ->
                    if ((repeatDays shr i) and 1 == 1) add(i)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (initialSchedule == null) "Add Medication" else "Edit Medication",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = pillName,
                onValueChange = { pillName = it },
                label = { Text("Medication Name") },
                placeholder = { Text("e.g. Paracetamol") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Time Picker Trigger
            OutlinedTextField(
                value = formattedTime,
                onValueChange = {},
                label = { Text("Dose Time") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true },
                enabled = false,
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Days Selector
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Repeat on Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEachIndexed { index, day ->
                        val isSelected = selectedDays.contains(index)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable {
                                    if (isSelected) selectedDays.remove(index) else selectedDays.add(index)
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.take(1),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val doseTime = "${formattedTime}:00.000Z"
                    var repeatDaysDecimal = 0
                    selectedDays.forEach { repeatDaysDecimal += (1 shl it) }
                    onSave(pillName, doseTime, repeatDaysDecimal)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = pillName.isNotBlank() && selectedDays.isNotEmpty()
            ) {
                Text(
                    if (initialSchedule == null) "Save Medication" else "Update Medication",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select Dose Time", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = { showTimePicker = false }) { Text("OK") }
                    }
                }
            }
        }
    }
}

