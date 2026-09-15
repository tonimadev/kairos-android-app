package digital.tonima.core.data.usecases

import digital.tonima.kairos.core.model.Event

interface GetEventsForMonthUseCase {
    suspend operator fun invoke(yearMonth: Long): List<Event>
}
