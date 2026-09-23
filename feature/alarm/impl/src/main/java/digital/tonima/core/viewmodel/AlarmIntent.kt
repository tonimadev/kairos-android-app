package digital.tonima.core.viewmodel

sealed class AlarmIntent {
    data class Init(
        val eventTitle: String,
        val uniqueId: Int,
        val eventId: Long,
        val startTime: Long,
        val meetingUrl: String?,
        val eventLocation: String?,
        val eventEndTime: Long = -1L,
    ) : AlarmIntent()

    data object Snooze : AlarmIntent()

    data object Stop : AlarmIntent()

    data object JoinMeeting : AlarmIntent()

    data object OpenMap : AlarmIntent()
}
