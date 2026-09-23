package digital.tonima.core.usecases

import digital.tonima.core.data.repository.CalendarRepository
import digital.tonima.core.utils.IcsParser
import javax.inject.Inject

/** Failures the UI explains to the user; anything else is reported as a generic import error. */
sealed class ImportIcsException(message: String) : Exception(message) {
    class NoEvents : ImportIcsException("No events found in the ICS content")

    class CalendarCreationFailed : ImportIcsException("Could not create the local calendar")
}

class ImportIcsUseCase
    @Inject
    constructor(
        private val calendarRepository: CalendarRepository,
        private val toggleEventAlarmUseCase: ToggleEventAlarmUseCase,
    ) {
        suspend operator fun invoke(
            content: String,
            calendarName: String,
            color: Int,
            alarmsEnabled: Boolean,
        ): Result<Unit> {
            return try {
                val events = IcsParser.parseIcs(content)
                if (events.isEmpty()) return Result.failure(ImportIcsException.NoEvents())

                val calendarId =
                    calendarRepository.createLocalCalendar(calendarName, color)
                        ?: return Result.failure(ImportIcsException.CalendarCreationFailed())

                for (event in events) {
                    val insertedId =
                        calendarRepository.insertEvent(
                            calendarId = calendarId,
                            title = event.title,
                            description = null,
                            location = event.location,
                            startTime = event.startTime,
                            endTime = event.endTime,
                            isAllDay = event.isAllDay,
                        )

                    if (insertedId != null && !alarmsEnabled) {
                        val savedEvent = event.copy(id = insertedId)
                        toggleEventAlarmUseCase.invoke(
                            event = savedEvent,
                            isEnabled = false,
                            disableAllOccurrences = true,
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
