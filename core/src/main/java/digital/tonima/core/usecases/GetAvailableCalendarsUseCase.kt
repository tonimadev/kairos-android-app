package digital.tonima.core.usecases

import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.repository.CalendarRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAvailableCalendarsUseCase
    @Inject
    constructor(
        private val repository: CalendarRepository,
    ) {
        suspend operator fun invoke(): List<DeviceCalendar> = repository.getAvailableCalendars()
    }
