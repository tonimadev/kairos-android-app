package digital.tonima.core.model

data class Event(
    val id: Long,
    val title: String,
    val startTime: Long,
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
) {
    val uniqueIntentId: Int
        get() = (id.toString() + startTime.toString()).hashCode()
}
