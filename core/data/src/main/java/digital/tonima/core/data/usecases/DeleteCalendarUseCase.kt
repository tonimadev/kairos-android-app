package digital.tonima.core.data.usecases

import digital.tonima.core.data.repository.CalendarRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteCalendarUseCase
    @Inject
    constructor(
        private val repository: CalendarRepository,
    ) {
        suspend operator fun invoke(calendarId: Long): Boolean {
            return repository.deleteCalendar(calendarId)
        }
    }
