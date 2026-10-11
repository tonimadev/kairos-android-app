package digital.tonima.core.data.usecases

import digital.tonima.core.utils.WeekendDaysProvider
import java.time.DayOfWeek
import javax.inject.Inject

class GetWeekendDaysUseCase
    @Inject
    constructor(
        private val weekendDaysProvider: WeekendDaysProvider,
    ) {
        operator fun invoke(): Set<DayOfWeek> = weekendDaysProvider.weekendDays()
    }
