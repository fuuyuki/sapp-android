package com.example.sapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sapp.R
import com.example.sapp.data.model.ScheduleOut
import com.example.sapp.ui.components.AppCard
import com.example.sapp.ui.components.AppTopBar
import com.example.sapp.ui.components.ScreenContainer
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMedicationScreen(
    initialSchedule: ScheduleOut? = null,
    onBack: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var pillName by remember { mutableStateOf(initialSchedule?.pillname ?: "") }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val initialHour = initialSchedule?.dose_time
        ?.substringAfter("T")
        ?.substringBefore(":")
        ?.toIntOrNull()
        ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialSchedule?.dose_time
        ?.substringAfter(":")
        ?.substringBefore(":")
        ?.toIntOrNull()
        ?: calendar.get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(initialHour, initialMinute)
    val formattedTime = remember(timePickerState.hour, timePickerState.minute) {
        "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
    }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            initialSchedule?.repeat_days?.let { repeatDays ->
                daysOfWeek.indices.forEach { index ->
                    if ((repeatDays shr index) and 1 == 1) add(index)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = if (initialSchedule == null) {
                    stringResource(R.string.add_medication)
                } else {
                    stringResource(R.string.edit)
                },
                onBack = onBack
            )
        }
    ) { padding ->
        ScreenContainer(
            modifier = Modifier.padding(padding),
            maxWidth = 680.dp
        ) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    OutlinedTextField(
                        value = pillName,
                        onValueChange = { pillName = it },
                        label = { Text(stringResource(R.string.medication_name)) },
                        placeholder = { Text("e.g. Paracetamol") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formattedTime,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.dose_time)) },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        shape = MaterialTheme.shapes.medium,
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                            }
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.repeat_days),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            daysOfWeek.forEachIndexed { index, day ->
                                val selected = selectedDays.contains(index)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) {
                                            selectedDays.remove(index)
                                        } else {
                                            selectedDays.add(index)
                                        }
                                    },
                                    label = { Text(day) },
                                    shape = MaterialTheme.shapes.small,
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val doseTime = "${formattedTime}:00.000Z"
                            var repeatDaysDecimal = 0
                            selectedDays.forEach { repeatDaysDecimal += (1 shl it) }
                            onSave(pillName, doseTime, repeatDaysDecimal)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = pillName.isNotBlank() && selectedDays.isNotEmpty()
                    ) {
                        Text(
                            text = if (initialSchedule == null) {
                                stringResource(R.string.save)
                            } else {
                                stringResource(R.string.update)
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.select_dose_time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}
