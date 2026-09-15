package digital.tonima.core.viewmodel

import androidx.compose.runtime.Immutable
import com.google.common.collect.ImmutableList
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.model.DeviceCalendar
import digital.tonima.kairos.core.model.InsightsPeriod
import digital.tonima.kairos.core.model.InsightsPeriod.WEEK
import digital.tonima.kairos.core.model.Weather
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class EventScreenUiState(
    val events: ImmutableList<EventUiModel> = ImmutableList.copyOf(emptyList()),
    val isRefreshing: Boolean = false,
    val selectedDate: Long = LocalDate.now().toEpochDay(),
    // Epoch day of the 1st of the current month — NOT YearMonth.monthValue (1-12). Every other
    // writer of this field (onMonthChanged/ChangeMonth, returnToToday) uses this same
    // representation; CalendarView and getEventsForMonthUseCase both expect it. A month-number
    // default here made CalendarRepositoryImpl.getEventsForMonth() resolve it via
    // LocalDate.ofEpochDay(1..12), i.e. early January 1970, so on cold start (before the first
    // ReturnToToday/ChangeMonth) the app queried and rendered the wrong month instead of today.
    val currentMonth: Long = YearMonth.now().atDay(1).toEpochDay(),
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val enabledCalendarIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val isProUser: Boolean = false,
    val isAiUser: Boolean = false,
    val showCreateEventDialog: Boolean = false,
    val weather: Weather? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,
    val selectedInsightsPeriod: InsightsPeriod = WEEK,
    val meetingStats: ImmutableList<Pair<String, Float>> = ImmutableList.copyOf(emptyList()),
    val showRatingBottomSheet: Boolean = false,
    val selectedBottomTab: Int = 0,
    val currentStreak: Int = 0,
    val punctualityScore: Int = 100,
    val aiUsageCount: Int = 0,
    val snoozeCount: Int = 0,
    val effect: EventSideEffect? = null,
)
