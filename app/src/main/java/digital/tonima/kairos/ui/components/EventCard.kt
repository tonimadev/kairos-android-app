package digital.tonima.kairos.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val targetCardColor by animateColorAsState(
        targetValue =
            if (event.isAlarmEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        animationSpec = tween(300),
        label = "cardColor",
    )
    val accentColor by animateColorAsState(
        targetValue =
            if (event.isAlarmEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        animationSpec = tween(300),
        label = "accentColor",
    )

    val calendarColor = if (event.calendarColor != 0) Color(event.calendarColor) else accentColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (event.isAlarmEnabled) 4.dp else 1.dp,
            ),
        colors = CardDefaults.cardColors(containerColor = targetCardColor),
        shape = RoundedCornerShape(16.dp),
        onClick = onEventClick,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(start = 0.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
                    .heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(5.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        .background(calendarColor),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color =
                        if (event.isAlarmEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (event.isAlarmEnabled) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ).padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = (if (event.isAlarmEnabled) "🔔 " else "🔕 ") + formatMillisToTime(event.startTime),
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                if (event.isAlarmEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    if (event.isRecurring) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "🔁 " + stringResource(R.string.recurring_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = event.isAlarmEnabled,
                    onCheckedChange = onToggle,
                    enabled = isGloballyEnabled,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
                if (event.isAlarmEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.vibrate_only),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Checkbox(
                            checked = event.vibrateOnly,
                            onCheckedChange = onVibrateToggle,
                            enabled = isGloballyEnabled,
                            modifier = Modifier.size(32.dp),
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
