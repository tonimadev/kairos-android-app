package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.util.openAutostartSettings
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanel(
    uiState: EventScreenUiState,
    onToggle: (Boolean) -> Unit,
    onDismissAutostart: () -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onAllDayAlarmsToggle: (Boolean) -> Unit,
    onAllDayAlarmHourChanged: (Int) -> Unit,
    onAlarmOffsetChanged: (AlarmOffset) -> Unit,
) {
    val context = LocalContext.current
    var offsetExpanded by remember { mutableStateOf(false) }
    val currentOffset = AlarmOffset.fromMinutes(uiState.alarmOffsetMinutes)

    Column {
        AlarmsToggleRow(
            modifier = Modifier.padding(vertical = 16.dp),
            alarmsEnabled = uiState.isGlobalAlarmEnabled,
            onToggle = onToggle,
        )

        Row(
            modifier =
                Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.vibrate_only))
            Switch(checked = uiState.vibrateOnly, onCheckedChange = onVibrateToggle)
        }

        // Alarm offset (when to fire before the event)
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = stringResource(R.string.alarm_offset_label),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            ExposedDropdownMenuBox(
                expanded = offsetExpanded,
                onExpandedChange = { offsetExpanded = !offsetExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = offsetLabel(currentOffset),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offsetExpanded) },
                    modifier =
                        Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = offsetExpanded,
                    onDismissRequest = { offsetExpanded = false },
                ) {
                    AlarmOffset.entries.forEach { offset ->
                        DropdownMenuItem(
                            text = { Text(offsetLabel(offset)) },
                            onClick = {
                                onAlarmOffsetChanged(offset)
                                offsetExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.all_day_alarms))
            Switch(checked = uiState.allDayAlarmsEnabled, onCheckedChange = onAllDayAlarmsToggle)
        }

        if (uiState.allDayAlarmsEnabled) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text =
                        stringResource(R.string.all_day_alarm_time) +
                            ": ${"%02d:00".format(uiState.allDayAlarmHour)}",
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Slider(
                    value = uiState.allDayAlarmHour.toFloat(),
                    onValueChange = { onAllDayAlarmHourChanged(it.roundToInt()) },
                    valueRange = 0f..23f,
                    steps = 22,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (uiState.audioWarning != AudioWarningState.NORMAL) {
            RingerModeWarningCard(ringerMode = uiState.audioWarning)
        }

        if (uiState.showAutostartSuggestion) {
            AutostartSuggestionCard(
                onOpenSettings = { openAutostartSettings(context) },
                onDismiss = onDismissAutostart,
            )
        }
    }
}

@Composable
private fun offsetLabel(offset: AlarmOffset): String =
    when (offset) {
        AlarmOffset.AT_TIME -> stringResource(R.string.alarm_offset_at_time)
        AlarmOffset.FIFTEEN_MINUTES -> stringResource(R.string.alarm_offset_15_min)
        AlarmOffset.THIRTY_MINUTES -> stringResource(R.string.alarm_offset_30_min)
        AlarmOffset.ONE_HOUR -> stringResource(R.string.alarm_offset_1_hour)
    }
