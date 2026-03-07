package digital.tonima.kairos.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.Event
import digital.tonima.core.viewmodel.EventScreenUiState
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun MainContent(
    uiState: EventScreenUiState,
    onRefresh: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEventToggle: (event: Event, isEnabled: Boolean, disableAllOccurrences: Boolean) -> Unit,
    onEventVibrateToggle: (event: Event, vibrateOnly: Boolean) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit,
    onDismissAutostart: () -> Unit,
    onReturnToToday: () -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onAllDayAlarmsToggle: (Boolean) -> Unit,
    onAllDayAlarmHourChanged: (Int) -> Unit,
    onAlarmOffsetChanged: (AlarmOffset) -> Unit,
    onSnoozeTimeChanged: (Int) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onCalendarFilterToggle: (calendarId: Long, enabled: Boolean) -> Unit = { _, _ -> },
    windowSizeClass: WindowSizeClass? = null,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Medium

    val showSideBySide = isLandscape || isExpanded || isMedium

    val eventsByDate =
        remember(uiState.events) {
            uiState.events.groupBy {
                Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }

    if (showSideBySide) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 8.dp),
            ) {
                ControlPanel(
                    uiState = uiState,
                    onToggle = onToggle,
                    onDismissAutostart = onDismissAutostart,
                    onVibrateToggle = onVibrateToggle,
                    onAllDayAlarmsToggle = onAllDayAlarmsToggle,
                    onAllDayAlarmHourChanged = onAllDayAlarmHourChanged,
                    onAlarmOffsetChanged = onAlarmOffsetChanged,
                    onSnoozeTimeChanged = onSnoozeTimeChanged,
                    onCalendarFilterToggle = onCalendarFilterToggle,
                )
                CalendarView(
                    modifier = Modifier.padding(top = 8.dp),
                    currentMonth = uiState.currentMonth,
                    selectedDate = uiState.selectedDate,
                    eventsByDate = eventsByDate,
                    onMonthChanged = onMonthChanged,
                    onDateSelected = onDateSelected,
                    onReturnToToday = onReturnToToday,
                )
            }
            EventList(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 16.dp),
                uiState = uiState,
                eventsByDate = eventsByDate,
                onRefresh = onRefresh,
                onEventToggle = onEventToggle,
                onEventVibrateToggle = onEventVibrateToggle,
                onEventClick = onEventClick,
                onSearchQueryChanged = onSearchQueryChanged,
            )
        }
    } else {
        EventList(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            uiState = uiState,
            eventsByDate = eventsByDate,
            onRefresh = onRefresh,
            onEventToggle = onEventToggle,
            onEventVibrateToggle = onEventVibrateToggle,
            onEventClick = onEventClick,
            onSearchQueryChanged = onSearchQueryChanged,
            headerContent = {
                Column {
                    ControlPanel(
                        uiState = uiState,
                        onToggle = onToggle,
                        onDismissAutostart = onDismissAutostart,
                        onVibrateToggle = onVibrateToggle,
                        onAllDayAlarmsToggle = onAllDayAlarmsToggle,
                        onAllDayAlarmHourChanged = onAllDayAlarmHourChanged,
                        onAlarmOffsetChanged = onAlarmOffsetChanged,
                        onSnoozeTimeChanged = onSnoozeTimeChanged,
                        onCalendarFilterToggle = onCalendarFilterToggle,
                    )
                    CalendarView(
                        modifier = Modifier.padding(top = 8.dp),
                        currentMonth = uiState.currentMonth,
                        selectedDate = uiState.selectedDate,
                        eventsByDate = eventsByDate,
                        onMonthChanged = onMonthChanged,
                        onDateSelected = onDateSelected,
                        onReturnToToday = onReturnToToday,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
        )
    }
}
