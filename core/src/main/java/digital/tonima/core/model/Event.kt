package digital.tonima.core.model

data class Event(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long = 0L,
    var isAlarmEnabled: Boolean = false,
    val isRecurring: Boolean = false,
    var vibrateOnly: Boolean = false,
    val isAllDay: Boolean = false,
    val calendarColor: Int = 0,
    val meetingUrl: String? = null,
    val location: String? = null,
    val departureTime: Long? = null,
    val travelTimeMinutes: Int? = null,
    val category: String? = null,
    val hasConflict: Boolean = false,
    val isBackToBack: Boolean = false,
) {
    val uniqueIntentId: Int
        get() = (id.toString() + startTime.toString()).hashCode()

    /** Duration in minutes. Returns 0 if endTime is not set. */
    val durationMinutes: Int
        get() = if (endTime > startTime) ((endTime - startTime) / 60_000).toInt() else 0

    /** Whether this event has a Google Meet / video call link. */
    val hasMeetingUrl: Boolean
        get() = !meetingUrl.isNullOrBlank()
}
