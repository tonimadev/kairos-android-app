package digital.tonima.core.usecases

import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.kairos.core.model.Event
import javax.inject.Inject

class CancelEventAlarmUseCase
    @Inject
    constructor(
        private val eventAlarmScheduler: EventAlarmScheduler,
    ) {
        operator fun invoke(event: Event) {
            eventAlarmScheduler.cancel(event)
        }
    }
