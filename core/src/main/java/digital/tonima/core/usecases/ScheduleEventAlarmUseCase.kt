package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.service.EventAlarmScheduler
import javax.inject.Inject

class ScheduleEventAlarmUseCase
    @Inject
    constructor(
        private val eventAlarmScheduler: EventAlarmScheduler,
    ) {
        operator fun invoke(
            event: Event,
            triggerTime: Long? = null,
        ) {
            eventAlarmScheduler.schedule(event, triggerTime)
        }
    }
