package digital.tonima.core.viewmodel

data class VoiceEventData(
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isAllDay: Boolean = false,
)
