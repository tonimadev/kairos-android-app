package digital.tonima.core.viewmodel

import androidx.compose.runtime.Immutable
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import digital.tonima.core.model.InsightsPeriod
import digital.tonima.core.model.InsightsPeriod.WEEK
import digital.tonima.core.model.Weather
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class EventScreenUiState(
    val events: List<Event> = emptyList(),
    val isRefreshing: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
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
    val meetingStats: List<Pair<String, Float>> = emptyList(),
    val showImportCalendarScreen: Boolean = false,
    val showManageCalendarsScreen: Boolean = false,
    val showRatingBottomSheet: Boolean = false,
    val selectedBottomTab: Int = 0,
    val currentStreak: Int = 0,
    val punctualityScore: Int = 100,
    val aiUsageCount: Int = 0,
    val snoozeCount: Int = 0,
)
