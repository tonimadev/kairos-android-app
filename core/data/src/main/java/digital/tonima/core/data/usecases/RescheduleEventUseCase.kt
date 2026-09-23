package digital.tonima.core.data.usecases

import digital.tonima.core.data.repository.CalendarRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RescheduleResult {
    data object Success : RescheduleResult

    /** Moving the series start would move every occurrence, so recurring events are refused. */
    data object RecurringNotSupported : RescheduleResult

    data object InvalidTime : RescheduleResult

    data object Failed : RescheduleResult
}

@Singleton
class RescheduleEventUseCase
    @Inject
    constructor(
        private val repository: CalendarRepository,
    ) {
        suspend operator fun invoke(
            eventId: Long,
            startTime: Long,
            endTime: Long,
        ): RescheduleResult =
            when {
                endTime <= startTime -> RescheduleResult.InvalidTime
                repository.isRecurring(eventId) -> RescheduleResult.RecurringNotSupported
                repository.rescheduleEvent(eventId, startTime, endTime) -> RescheduleResult.Success
                else -> RescheduleResult.Failed
            }
    }
