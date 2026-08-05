package digital.tonima.core.viewmodel

sealed class AlarmSideEffect {
    data class SendSnoozeBroadcast(
        val eventTitle: String,
        val uniqueId: Int,
        val eventId: Long,
        val startTime: Long,
        val meetingUrl: String?,
    ) : AlarmSideEffect()

    data object FinishScreen : AlarmSideEffect()

    data class OpenMeetingUrl(val url: String) : AlarmSideEffect()

    data class OpenMapUrl(val location: String) : AlarmSideEffect()
}
