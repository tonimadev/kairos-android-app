package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import java.time.YearMonth

interface GetEventsForMonthUseCase {
    suspend operator fun invoke(yearMonth: YearMonth): List<Event>
}
