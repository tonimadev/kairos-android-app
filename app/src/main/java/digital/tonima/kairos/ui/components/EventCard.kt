package digital.tonima.kairos.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Repeat
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import digital.tonima.core.model.Event
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions
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
    val alarmStateDescription =
        stringResource(
            if (event.isAlarmEnabled) R.string.cd_alarms_enabled else R.string.cd_alarms_disabled,
        )
    val formattedTime = formatMillisToTime(event.startTime)
    val recurringDescription = stringResource(R.string.cd_event_recurring)
    val indicatorDescription = stringResource(R.string.cd_event_indicator)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (event.isAlarmEnabled) {
                        Dimensions.ElevationMedium
                    } else {
                        Dimensions.ElevationExtraSmall
                    },
            ),
        colors = CardDefaults.cardColors(containerColor = targetCardColor),
        shape = RoundedCornerShape(Dimensions.RadiusLarge),
        onClick = onEventClick,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(
                        start = Dimensions.PaddingNone,
                        top = Dimensions.PaddingDefault,
                        end = Dimensions.PaddingNormal,
                        bottom = Dimensions.PaddingDefault,
                    )
                    .height(IntrinsicSize.Min)
                    .heightIn(min = Dimensions.EventCardMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(Dimensions.EventIndicatorWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = Dimensions.RadiusSmall, bottomEnd = Dimensions.RadiusSmall))
                        .background(calendarColor)
                        .clearAndSetSemantics {
                            contentDescription = indicatorDescription
                        },
            )
            Spacer(modifier = Modifier.width(Dimensions.EventCardHorizontalPadding))
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
                if (!event.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = event.location!!,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.EventCardSpacing))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.EventCardSpacing),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(Dimensions.RadiusFull))
                                .background(
                                    if (event.isAlarmEnabled) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                )
                                .padding(
                                    horizontal = Dimensions.EventTagHorizontalPadding,
                                    vertical = Dimensions.EventTagVerticalPadding,
                                )
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "$alarmStateDescription $formattedTime"
                                },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = if (event.isAlarmEnabled) Icons.Rounded.Alarm else Icons.Rounded.AlarmOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint =
                                    if (event.isAlarmEnabled) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                            Text(
                                text = formatMillisToTime(event.startTime),
                                style = MaterialTheme.typography.labelMedium,
                                color =
                                    if (event.isAlarmEnabled) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                    if (event.isRecurring) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(Dimensions.RadiusFull))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                    .padding(
                                        horizontal = Dimensions.EventTagHorizontalPadding,
                                        vertical = Dimensions.EventTagVerticalPadding,
                                    )
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = recurringDescription
                                    },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = stringResource(R.string.recurring_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    if (event.isAlarmEnabled && event.travelTimeMinutes != null) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(Dimensions.RadiusFull))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
                                    .padding(
                                        horizontal = Dimensions.EventTagHorizontalPadding,
                                        vertical = Dimensions.EventTagVerticalPadding,
                                    ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    text = stringResource(R.string.travel_time_label, event.travelTimeMinutes!!),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                    if (event.isAlarmEnabled) {
                        val alarmTime = event.departureTime ?: (event.startTime)
                        val timeRemaining = formatTimeRemaining(LocalContext.current, alarmTime)
                        Text(
                            text = stringResource(R.string.alarm_in_label, timeRemaining),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val switchLabel =
                    stringResource(
                        if (event.isAlarmEnabled) R.string.cd_alarms_enabled else R.string.cd_alarms_disabled,
                    )
                Switch(
                    checked = event.isAlarmEnabled,
                    onCheckedChange = onToggle,
                    enabled = isGloballyEnabled,
                    modifier =
                        Modifier.semantics {
                            contentDescription = switchLabel
                        },
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
                        modifier =
                            Modifier
                                .padding(top = 2.dp)
                                .semantics(mergeDescendants = true) {
                                    role = Role.Checkbox
                                },
                    ) {
                        Text(
                            text = stringResource(R.string.vibrate_only),
                            modifier = Modifier.weight(1f, fill = false),
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

@Composable
fun formatTimeRemaining(
    context: android.content.Context,
    targetTime: Long,
): String {
    val now = System.currentTimeMillis()
    val diff = targetTime - now
    if (diff <= 0) return stringResource(R.string.alarm_now_label)

    val diffSeconds = diff / 1000
    val diffMinutes = diffSeconds / 60
    val diffHours = diffMinutes / 60

    return when {
        diffHours > 0 -> {
            val h = diffHours.toInt()
            val m = (diffMinutes % 60).toInt()
            if (m > 0) {
                stringResource(R.string.hours_short, h) + " " + stringResource(R.string.minutes_short, m)
            } else {
                stringResource(R.string.hours_short, h)
            }
        }
        else -> stringResource(R.string.minutes_short, diffMinutes.toInt())
    }
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
            location = "Av. Paulista, 1000",
            travelTimeMinutes = 25,
            departureTime = System.currentTimeMillis() + 3600000 - (25 * 60 * 1000L) - (300 * 1000L),
        )
    EventCard(
        event = sampleEvent,
        isGloballyEnabled = true,
        onToggle = {},
        onVibrateToggle = {},
        onEventClick = {},
    )
}
