package digital.tonima.core.usecases

import digital.tonima.core.service.EventAlarmScheduler
import javax.inject.Inject

class ScheduleSnoozeAlarmUseCase
    @Inject
    constructor(
        private val eventAlarmScheduler: EventAlarmScheduler,
    ) {
        operator fun invoke(
            eventTitle: String,
            uniqueId: Int,
            eventId: Long,
            startTime: Long,
            meetingUrl: String? = null,
        ) {
            eventAlarmScheduler.scheduleSnooze(eventTitle, uniqueId, eventId, startTime, meetingUrl)
        }
    }
