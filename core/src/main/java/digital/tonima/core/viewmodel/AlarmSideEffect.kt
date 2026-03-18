package digital.tonima.core.viewmodel

sealed class AlarmSideEffect {
    object FinishScreen : AlarmSideEffect()

    data class OpenMeetingUrl(val url: String) : AlarmSideEffect()

    data class SendSnoozeBroadcast(
        val eventTitle: String,
        val uniqueId: Int,
        val eventId: Long,
        val startTime: Long,
        val meetingUrl: String?,
    ) : AlarmSideEffect()
}
