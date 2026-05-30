package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.SwitchDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import digital.tonima.core.model.Event
import digital.tonima.kairos.core.R
import digital.tonima.kairos.wear.ui.theme.Dimensions
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun EventCard(
    event: Event,
    isGloballyEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val allDayText = stringResource(R.string.all_day_event)
    val localTime =
        Instant
            .ofEpochMilli(event.startTime)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
    val formattedTime =
        remember(localTime, event.isAllDay, allDayText) {
            if (event.isAllDay) {
                allDayText
            } else {
                formatter.format(localTime)
            }
        }

    val alarmStateDescription =
        stringResource(
            if (event.isAlarmEnabled) R.string.cd_alarms_enabled else R.string.cd_alarms_disabled,
        )
    val recurringDescription = stringResource(R.string.cd_event_recurring)

    Card(
        onClick = { onToggle(!event.isAlarmEnabled) },
        enabled = isGloballyEnabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.PaddingExtraSmall, horizontal = Dimensions.PaddingNormal)
                .semantics(mergeDescendants = true) {
                    role = Role.Switch
                    stateDescription = alarmStateDescription
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingMedium),
        ) {
            Text(text = event.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)

            if (!event.location.isNullOrBlank()) {
                Text(
                    text = "📍 " + event.location,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingExtraSmall))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (event.isRecurring) {
                Text(
                    text = "🔁 " + stringResource(R.string.recurring_label),
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                        Modifier.semantics {
                            contentDescription = recurringDescription
                        },
                )
            }

            if (event.isAlarmEnabled && event.travelTimeMinutes != null) {
                Text(
                    text = "🚗 " + stringResource(R.string.minutes_short, event.travelTimeMinutes!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            if (event.isAlarmEnabled) {
                val alarmTime = event.departureTime ?: (event.startTime)
                val diff = alarmTime - System.currentTimeMillis()
                val diffMinutes = diff / (1000 * 60)
                if (diffMinutes > 0) {
                    Text(
                        text = "⏰ " + stringResource(R.string.minutes_short, diffMinutes.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (event.isAlarmEnabled) "🔔" else "🔕",
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                        Modifier.semantics {
                            contentDescription = alarmStateDescription
                        },
                )
                Switch(
                    checked = event.isAlarmEnabled,
                    enabled = isGloballyEnabled,
                    onCheckedChange = null,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                )
            }
        }
    }
}
