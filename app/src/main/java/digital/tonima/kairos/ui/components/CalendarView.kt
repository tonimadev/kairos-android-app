package digital.tonima.kairos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.R
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun CalendarView(
    modifier: Modifier = Modifier,
    currentMonth: Long,
    selectedDate: Long,
    eventsByDate: ImmutableMap<Long, ImmutableList<EventUiModel>>,
    onMonthChanged: (Long) -> Unit,
    onDateSelected: (Long) -> Unit,
    onReturnToToday: () -> Unit,
) {
    val currentYearMonth =
        remember(currentMonth) {
            YearMonth.from(LocalDate.ofEpochDay(currentMonth))
        }

    val startMonth = remember { YearMonth.now().minusMonths(100) }
    val endMonth = remember { YearMonth.now().plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val todayEpoch = remember { LocalDate.now().toEpochDay() }

    val state =
        rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentYearMonth,
            firstDayOfWeek = firstDayOfWeek,
        )
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentYearMonth) {
        if (state.firstVisibleMonth.yearMonth != currentYearMonth) {
            scope.launch { state.animateScrollToMonth(currentYearMonth) }
        }
    }

    LaunchedEffect(state.firstVisibleMonth.yearMonth) {
        val visibleMonthEpochDay = state.firstVisibleMonth.yearMonth.atDay(1).toEpochDay()
        if (visibleMonthEpochDay != currentMonth) {
            onMonthChanged(visibleMonthEpochDay)
        }
    }

    Column(modifier = modifier) {
        MonthHeader(
            month = state.firstVisibleMonth.yearMonth,
            onReturnToTodayClicked = onReturnToToday,
        )
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                val dayEpoch = day.date.toEpochDay()
                Day(
                    day = day,
                    isSelected = selectedDate == dayEpoch,
                    isToday = todayEpoch == dayEpoch,
                    hasEvents = eventsByDate.containsKey(dayEpoch),
                ) { clickedDay ->
                    onDateSelected(clickedDay.date.toEpochDay())
                }
            },
            monthHeader = { month ->
                DaysOfWeekHeader(
                    daysOfWeek = ImmutableList.copyOf(month.weekDays.first().map { it.date.dayOfWeek }),
                )
            },
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onReturnToTodayClicked: () -> Unit,
) {
    val currentMonth = remember { YearMonth.now() }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val locale = LocalConfiguration.current.locales.get(0)
        val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }

        Text(
            text =
                remember(month, formatter, locale) {
                    month.format(formatter).replaceFirstChar { it.titlecase(locale) }
                },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        if (month != currentMonth) {
            IconButton(
                onClick = onReturnToTodayClicked,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Today,
                    contentDescription = stringResource(R.string.back_to_today),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DaysOfWeekHeader(daysOfWeek: ImmutableList<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        val locale = LocalConfiguration.current.locales.get(0)
        for (dayOfWeek in daysOfWeek) {
            val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
            val dayName = remember(dayOfWeek, locale) { dayOfWeek.getDisplayName(TextStyle.SHORT, locale) }
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayName,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isWeekend) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                color =
                    if (isWeekend) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    onClick: (CalendarDay) -> Unit,
) {
    val isMonthDate = day.position == DayPosition.MonthDate

    val targetBg =
        when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else -> Color.Transparent
        }
    val bgColor by animateColorAsState(targetValue = targetBg, label = "dayBg")

    val targetContentColor =
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.primary
            !isMonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    val contentColor by animateColorAsState(targetValue = targetContentColor, label = "contentColor")

    val borderSize by animateDpAsState(
        targetValue = if (isToday && !isSelected) 2.dp else 0.dp,
        label = "borderSize",
    )

    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .then(
                    if (borderSize > 0.dp) {
                        Modifier.border(
                            borderSize,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = isMonthDate) { onClick(day) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (hasEvents && isMonthDate) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .background(
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarViewPreview() {
    val sampleEvents =
        listOf(
            EventUiModel(
                id = 1L,
                title = "Meeting with Team",
                startTime = System.currentTimeMillis() + 3600000,
            ),
            EventUiModel(
                id = 2L,
                title = "Doctor Appointment",
                startTime = System.currentTimeMillis() + 7200000,
            ),
        )

    val todayEpoch = LocalDate.now().toEpochDay()
    val eventsByDate =
        mapOf(
            todayEpoch to ImmutableList.copyOf(sampleEvents),
        )

    CalendarView(
        currentMonth = YearMonth.now().atDay(1).toEpochDay(),
        selectedDate = todayEpoch,
        eventsByDate = ImmutableMap.copyOf(eventsByDate),
        onMonthChanged = {},
        onDateSelected = {},
        onReturnToToday = {},
    )
}
