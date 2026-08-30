package digital.tonima.core.usecases

import digital.tonima.core.model.Event

interface GetEventsForMonthUseCase {
    suspend operator fun invoke(yearMonth: Long): List<Event>
}
