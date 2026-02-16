package com.example.sapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )

    val formattedTime = remember(timePickerState.hour, timePickerState.minute) {
        val h = timePickerState.hour.toString().padStart(2, '0')
        val m = timePickerState.minute.toString().padStart(2, '0')
        "$h:$m"
    }

    // Days of week selector - Improved labels
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
            TopAppBar(
                title = { Text(if (initialSchedule == null) "Add Medication" else "Update Medication", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
            // Pill Name Input
            OutlinedTextField(
                value = pillName,
                onValueChange = { pillName = it },
                label = { Text("Pill Name") },
                placeholder = { Text("e.g. Paracetamol") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            // Time Selection Trigger
            OutlinedTextField(
                value = formattedTime,
                onValueChange = {},
                label = { Text("Dose Time") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
            )

            // Days Selector Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Repeat on Days",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Using Weight(1f) ensures all 7 items fit perfectly regardless of screen width
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEachIndexed { index, day ->
                        val isSelected = selectedDays.contains(index)

                        // Custom Round Day Toggle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f) // Makes it a perfect circle
                                .padding(2.dp)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null // Remove ripple for cleaner look
                                ) {
                                    if (isSelected) selectedDays.remove(index)
                                    else selectedDays.add(index)
                                }
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.take(1), // Show only first letter (S, M, T, W...)
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = pillName.isNotBlank() && selectedDays.isNotEmpty(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (initialSchedule == null) "Save Medication" else "Update Medication", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Time Picker Dialog Logic
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Time",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    )
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
