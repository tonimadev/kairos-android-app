package digital.tonima.kairos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.tonima.core.model.Event
import digital.tonima.kairos.core.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: Event,
    isGloballyEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEventClick: () -> Unit,
    onJoinMeeting: ((String) -> Unit)? = null, // Kept for signature compatibility
    onCopyMeetingUrl: ((String) -> Unit)? = null, // Kept for signature compatibility
) {
    val targetCardColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "cardColor",
    )

    val locale = LocalConfiguration.current.locales.get(0)
    val timeFormat = remember(locale) { SimpleDateFormat("h:mm", locale) }
    val amPmFormat = remember(locale) { SimpleDateFormat("a", locale) }

    val timeString = remember(event.startTime, timeFormat) { timeFormat.format(Date(event.startTime)) }
    val amPmString = remember(event.startTime, amPmFormat) { amPmFormat.format(Date(event.startTime)).uppercase() }

    val calendar =
        remember(event.startTime) {
            Calendar.getInstance().apply { timeInMillis = event.startTime }
        }
    val eventDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = targetCardColor),
        shape = RoundedCornerShape(24.dp),
        onClick = onEventClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Label
            Text(
                text = event.title,
                fontSize = 14.sp,
                color = Color(0xFFE2E2E2), // Light grey text
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Time
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeString,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = amPmString,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Row: Days and Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (event.isRecurring) {
                    Text(
                        text = stringResource(R.string.everyday),
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0C0),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val days =
                            listOf(
                                stringResource(R.string.day_sunday_short),
                                stringResource(R.string.day_monday_short),
                                stringResource(R.string.day_tuesday_short),
                                stringResource(R.string.day_wednesday_short),
                                stringResource(R.string.day_thursday_short),
                                stringResource(R.string.day_friday_short),
                                stringResource(R.string.day_saturday_short),
                            )
                        days.forEachIndexed { index, day ->
                            // Calendar.SUNDAY is 1, so index 0 = Sunday
                            val isSelected = (index + 1) == eventDayOfWeek && event.isAlarmEnabled

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFB0B0C0),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (isSelected) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }

                // Custom Switch to match design
                Switch(
                    checked = event.isAlarmEnabled,
                    onCheckedChange = onToggle,
                    enabled = isGloballyEnabled,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedBorderColor = Color.Transparent,
                            checkedBorderColor = Color.Transparent,
                        ),
                    modifier = Modifier.height(24.dp),
                )
            }
        }
    }
}

@Preview
@Composable
fun EventCardPreview() {
    val sampleEvent =
        Event(
            id = 1L,
            title = "Wake up",
            startTime = System.currentTimeMillis() + 3600000,
            isAlarmEnabled = true,
            location = "Av. Paulista, 1000",
        )
    EventCard(
        event = sampleEvent,
        isGloballyEnabled = true,
        onToggle = {},
        onEventClick = {},
    )
}
