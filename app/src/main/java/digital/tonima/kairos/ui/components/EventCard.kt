package digital.tonima.kairos.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.tonima.core.model.Event
import digital.tonima.kairos.core.R
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: Event,
    isGloballyEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onEventClick: () -> Unit,
) {
    if (event.isAlarmEnabled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onEventClick,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (event.isAlarmEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.heightIn(min = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = (if (event.isAlarmEnabled) "🔔 " else "🔕 ") + formatMillisToTime(event.startTime),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                if (event.isRecurring) {
                    Spacer(modifier = Modifier.heightIn(min = 4.dp))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "🔁 " + stringResource(R.string.recurring_label),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = event.isAlarmEnabled,
                    onCheckedChange = onToggle,
                    enabled = isGloballyEnabled,
                )
                if (event.isAlarmEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.vibrate_only),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Checkbox(
                            checked = event.vibrateOnly,
                            onCheckedChange = onVibrateToggle,
                            enabled = isGloballyEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun formatMillisToTime(millis: Long): String {
    val context = LocalContext.current
    val timeFormat = DateFormat.getTimeFormat(context)
    val formattedTime = timeFormat.format(Date(millis))
    return stringResource(R.string.at_time, formattedTime)
}

@Preview
@Composable
fun EventCardPreview() {
    val sampleEvent =
        Event(
            id = 1L,
            title = "Team Meeting",
            startTime = System.currentTimeMillis() + 3600000,
            isAlarmEnabled = true,
        )
    EventCard(
        event = sampleEvent,
        isGloballyEnabled = true,
        onToggle = {},
        onVibrateToggle = {},
        onEventClick = {},
    )
}
