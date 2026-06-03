package digital.tonima.core.usecases

import digital.tonima.core.repository.CalendarRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCalendarUseCase
    @Inject
    constructor(
        private val repository: CalendarRepository,
    ) {
        suspend operator fun invoke(
            calendarId: Long,
            name: String,
            color: Int,
        ): Boolean {
            return repository.updateCalendar(calendarId, name, color)
        }
    }
