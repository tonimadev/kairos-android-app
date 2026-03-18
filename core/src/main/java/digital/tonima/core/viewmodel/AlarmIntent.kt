package digital.tonima.core.viewmodel

sealed class AlarmIntent {
    data class Init(
        val eventTitle: String,
        val uniqueId: Int,
        val eventId: Long,
        val startTime: Long,
        val meetingUrl: String?,
    ) : AlarmIntent()

    object Snooze : AlarmIntent()

    object Stop : AlarmIntent()

    object JoinMeeting : AlarmIntent()
}
