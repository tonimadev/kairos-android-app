package digital.tonima.core.repository

import com.google.common.collect.ImmutableList
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event

interface CalendarRepository {
    suspend fun getAvailableCalendars(): ImmutableList<DeviceCalendar>

    suspend fun getEventsForMonth(
        yearMonth: Long,
        allowedCalendarIds: ImmutableList<Long> = ImmutableList.of(),
    ): ImmutableList<Event>

    suspend fun getNextUpcomingEvent(allowedCalendarIds: ImmutableList<Long> = ImmutableList.of()): Event?

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

    suspend fun createLocalCalendar(
        name: String,
        color: Int,
    ): Long?

    suspend fun updateCalendar(
        calendarId: Long,
        name: String,
        color: Int,
    ): Boolean

    suspend fun deleteCalendar(calendarId: Long): Boolean
}
