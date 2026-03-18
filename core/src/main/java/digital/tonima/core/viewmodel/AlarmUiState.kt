package digital.tonima.core.viewmodel

data class AlarmUiState(
    val eventTitle: String = "",
    val uniqueId: Int = -1,
    val eventId: Long = -1L,
    val startTime: Long = -1L,
    val meetingUrl: String? = null,
) {
    val hasMeetingUrl: Boolean get() = !meetingUrl.isNullOrEmpty()
}
