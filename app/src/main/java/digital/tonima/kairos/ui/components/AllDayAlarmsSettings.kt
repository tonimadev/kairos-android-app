package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.core.R
import kotlin.math.roundToInt

@Composable
fun AllDayAlarmsSettings(
    modifier: Modifier = Modifier,
    allDayAlarmsEnabled: Boolean,
    allDayAlarmHour: Int,
    onAllDayAlarmsToggle: (Boolean) -> Unit,
    onAllDayAlarmHourChanged: (Int) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Toggle para habilitar/desabilitar alarmes all-day
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.all_day_alarms),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(
                checked = allDayAlarmsEnabled,
                onCheckedChange = onAllDayAlarmsToggle,
            )
        }

        // Slider para escolher o horário (só aparece se habilitado)
        if (allDayAlarmsEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.all_day_alarms_time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Slider(
                        value = allDayAlarmHour.toFloat(),
                        onValueChange = { onAllDayAlarmHourChanged(it.roundToInt()) },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.all_day_alarm_time_format, allDayAlarmHour),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
