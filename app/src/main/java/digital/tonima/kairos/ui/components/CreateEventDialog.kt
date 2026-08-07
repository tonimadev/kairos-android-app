package digital.tonima.kairos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.core.model.DeviceCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import digital.tonima.kairos.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    onDismiss: () -> Unit,
    onCreate: (
        calendarId: Long,
        title: String,
        description: String?,
        location: String?,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean,
    ) -> Unit,
    availableCalendars: List<DeviceCalendar>,
    initialDate: LocalDate = LocalDate.now(),
    voiceEventData: digital.tonima.core.viewmodel.VoiceEventData? = null,
) {
    var title by remember { mutableStateOf(voiceEventData?.title ?: "") }
    var description by remember { mutableStateOf(voiceEventData?.description ?: "") }
    var location by remember { mutableStateOf(voiceEventData?.location ?: "") }

    val initialStartDateTime =
        voiceEventData?.startTime?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } ?: LocalDateTime.of(initialDate, LocalTime.now().plusHours(1).withMinute(0))

    val initialEndDateTime =
        voiceEventData?.endTime?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } ?: initialStartDateTime.plusHours(1)

    var startDate by remember { mutableStateOf(initialStartDateTime.toLocalDate()) }
    var startTime by remember { mutableStateOf(initialStartDateTime.toLocalTime()) }
    var endDate by remember { mutableStateOf(initialEndDateTime.toLocalDate()) }
    var endTime by remember { mutableStateOf(initialEndDateTime.toLocalTime()) }
    var isAllDay by remember { mutableStateOf(voiceEventData?.isAllDay ?: false) }
    var selectedCalendar by remember { mutableStateOf(availableCalendars.firstOrNull()) }
    var calendarExpanded by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(CoreR.string.create_event)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(CoreR.string.event_title)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Calendar Selector
                Box {
                    OutlinedTextField(
                        value = selectedCalendar?.displayName ?: "",
                        onValueChange = {},
                        label = { Text(stringResource(CoreR.string.calendar)) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                painter = painterResource(digital.tonima.kairos.R.drawable.ic_expand_more),
                                contentDescription = stringResource(CoreR.string.cd_expand_settings),
                                Modifier.clickable { calendarExpanded = true },
                            )
                        },
                    )
                    DropdownMenu(
                        expanded = calendarExpanded,
                        onDismissRequest = { calendarExpanded = false },
                    ) {
                        availableCalendars.forEach { calendar ->
                            DropdownMenuItem(
                                text = { Text(calendar.displayName) },
                                onClick = {
                                    selectedCalendar = calendar
                                    calendarExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAllDay, onCheckedChange = { isAllDay = it })
                    Text(stringResource(CoreR.string.all_day))
                }

                // Start Date & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val modifier = Modifier.weight(1f).clickable { showStartDatePicker = true }
                    Box(modifier = modifier) {
                        OutlinedTextField(
                            value = startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            onValueChange = {},
                            label = { Text(stringResource(CoreR.string.start_date)) },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        )
                    }
                    if (!isAllDay) {
                        val timeModifier = Modifier.weight(1f).clickable { showStartTimePicker = true }
                        Box(modifier = timeModifier) {
                            OutlinedTextField(
                                value = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                onValueChange = {},
                                label = { Text(stringResource(CoreR.string.start_time)) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                            )
                        }
                    }
                }

                // End Date & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val dateModifier = Modifier.weight(1f).clickable { showEndDatePicker = true }
                    Box(modifier = dateModifier) {
                        OutlinedTextField(
                            value = endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            onValueChange = {},
                            label = { Text(stringResource(CoreR.string.end_date)) },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        )
                    }
                    if (!isAllDay) {
                        val timeModifier = Modifier.weight(1f).clickable { showEndTimePicker = true }
                        Box(modifier = timeModifier) {
                            OutlinedTextField(
                                value = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                onValueChange = {},
                                label = { Text(stringResource(CoreR.string.end_time)) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(CoreR.string.description)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(CoreR.string.location)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startMillis: Long
                    val endMillis: Long
                    if (isAllDay) {
                        startMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                        endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    } else {
                        startMillis =
                            LocalDateTime.of(
                                startDate,
                                startTime,
                            ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        endMillis =
                            LocalDateTime.of(
                                endDate,
                                endTime,
                            ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    selectedCalendar?.let {
                        onCreate(
                            it.id,
                            title,
                            description.ifBlank { null },
                            location.ifBlank { null },
                            startMillis,
                            endMillis,
                            isAllDay,
                        )
                    }
                },
                enabled = title.isNotBlank() && selectedCalendar != null,
            ) {
                Text(stringResource(CoreR.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(CoreR.string.cancel))
            }
        },
    )

    // Date/Time Picker Dialogs
    if (showStartDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                        if (endDate.isBefore(startDate)) endDate = startDate
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute)
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showStartTimePicker = false
                }) { Text("OK") }
            },
        ) { TimePicker(state = timePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = endTime.hour, initialMinute = endTime.minute)
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showEndTimePicker = false
                }) { Text("OK") }
            },
        ) { TimePicker(state = timePickerState) }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() },
    )
}
