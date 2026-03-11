package digital.tonima.core.service

import digital.tonima.core.model.Event

interface EventAlarmScheduler {
    fun schedule(event: Event)

    fun scheduleSnooze(
        eventTitle: String,
        uniqueId: Int,
        eventId: Long,
        startTime: Long,
        meetingUrl: String? = null,
    )

    fun cancel(event: Event)
}
