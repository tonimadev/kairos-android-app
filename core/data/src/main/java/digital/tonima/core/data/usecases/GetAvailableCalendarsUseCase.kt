package digital.tonima.core.data.usecases

import digital.tonima.core.data.repository.CalendarRepository
import digital.tonima.kairos.core.model.DeviceCalendar
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
