package digital.tonima.core.repository

import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import java.time.YearMonth

interface CalendarRepository {
    suspend fun getAvailableCalendars(): List<DeviceCalendar>

    suspend fun getEventsForMonth(
        yearMonth: YearMonth,
        allowedCalendarIds: List<Long> = emptyList(),
    ): List<Event>

    suspend fun getNextUpcomingEvent(allowedCalendarIds: List<Long> = emptyList()): Event?

    suspend fun isRecurring(eventId: Long): Boolean

    suspend fun insertEvent(
        calendarId: Long,
        title: String,
        description: String? = null,
        location: String? = null,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean = false,
    ): Long?
}
