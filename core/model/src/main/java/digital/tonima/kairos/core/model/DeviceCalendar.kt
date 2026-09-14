package digital.tonima.kairos.core.model

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int = 0,
)
