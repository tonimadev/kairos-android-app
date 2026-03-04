package digital.tonima.kairos.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import digital.tonima.core.model.Event
import digital.tonima.kairos.core.R
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    modifier: Modifier = Modifier,
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<Event>>,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onReturnToToday: () -> Unit,
) {
    val startMonth = remember { YearMonth.now().minusMonths(100) }
    val endMonth = remember { YearMonth.now().plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val state =
        rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = firstDayOfWeek,
        )
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentMonth) {
        if (state.firstVisibleMonth.yearMonth != currentMonth) {
            scope.launch { state.animateScrollToMonth(currentMonth) }
        }
    }

    LaunchedEffect(state.firstVisibleMonth.yearMonth) {
        onMonthChanged(state.firstVisibleMonth.yearMonth)
    }

    Column(modifier = modifier) {
        MonthHeader(
            month = state.firstVisibleMonth.yearMonth,
            onReturnToTodayClicked = onReturnToToday, // Passa a ação para o cabeçalho
        )
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                Day(
                    day = day,
                    isSelected = selectedDate == day.date,
                    hasEvents = eventsByDate.containsKey(day.date),
                ) { onDateSelected(it.date) }
            },
            monthHeader = {
                DaysOfWeekHeader(
                    daysOfWeek = it.weekDays.first().map { it.date.dayOfWeek },
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
    val currentMonth = YearMonth.now()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val locale = Locale.getDefault()
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        Text(
            text = month.format(formatter).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (month != currentMonth) {
            TextButton(onClick = onReturnToTodayClicked) {
                Text(stringResource(R.string.back_to_today))
            }
        }
    }
}

@Composable
private fun DaysOfWeekHeader(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        val locale = Locale.getDefault()
        for (dayOfWeek in daysOfWeek) {
            val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
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
    hasEvents: Boolean,
    onClick: (CalendarDay) -> Unit,
) {
    val isToday = day.date == LocalDate.now()
    val targetBg =
        when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else -> Color.Transparent
        }
    val bgColor by animateColorAsState(targetValue = targetBg, label = "dayBg")

    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .background(
                    color = bgColor,
                    shape = CircleShape,
                ).border(
                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0f else 1f),
                    shape = CircleShape,
                ).clickable(enabled = day.position == DayPosition.MonthDate) { onClick(day) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color =
                    when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        day.position != DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (hasEvents && day.position == DayPosition.MonthDate) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 3.dp)
                            .size(6.dp)
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
            Event(
                id = 1L,
                title = "Meeting with Team",
                startTime = System.currentTimeMillis() + 3600000,
            ),
            Event(
                id = 2L,
                title = "Doctor Appointment",
                startTime = System.currentTimeMillis() + 7200000,
            ),
            Event(
                id = 3L,
                title = "Lunch with Sarah",
                startTime = System.currentTimeMillis() + 10800000,
            ),
        )
    val eventsByDate = sampleEvents.groupBy { LocalDate.now() }
    CalendarView(
        currentMonth = YearMonth.now(),
        selectedDate = LocalDate.now(),
        eventsByDate = eventsByDate,
        onMonthChanged = {},
        onDateSelected = {},
        onReturnToToday = {},
    )
}
