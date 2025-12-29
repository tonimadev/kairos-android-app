package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.util.openAutostartSettings
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R
import kotlin.math.roundToInt

@Composable
fun ControlPanel(
    uiState: EventScreenUiState,
    onToggle: (Boolean) -> Unit,
    onDismissAutostart: () -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onAllDayAlarmsToggle: (Boolean) -> Unit,
    onAllDayAlarmHourChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    Column {
        AlarmsToggleRow(
            modifier = Modifier.padding(vertical = 16.dp),
            alarmsEnabled = uiState.isGlobalAlarmEnabled,
            onToggle = onToggle,
        )

        Row(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.vibrate_only))
            Switch(checked = uiState.vibrateOnly, onCheckedChange = onVibrateToggle)
        }

        Row(
            modifier = Modifier
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
                    text = stringResource(R.string.all_day_alarm_time) +
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
