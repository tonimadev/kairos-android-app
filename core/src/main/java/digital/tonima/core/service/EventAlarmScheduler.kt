package digital.tonima.core.service

import digital.tonima.kairos.core.model.Event

interface EventAlarmScheduler {
    fun schedule(
        event: Event,
        triggerTime: Long? = null,
    )

    fun scheduleSnooze(
        eventTitle: String,
        uniqueId: Int,
        eventId: Long,
        startTime: Long,
        meetingUrl: String? = null,
        eventLocation: String? = null,
        eventEndTime: Long = -1L,
    )

    fun cancel(event: Event)
}
