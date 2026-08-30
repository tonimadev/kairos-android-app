package digital.tonima.core.viewmodel.uimodel

import androidx.compose.runtime.Immutable

@Immutable
data class EventUiModel(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val isAlarmEnabled: Boolean = false,
    val isRecurring: Boolean = false,
    val vibrateOnly: Boolean = false,
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
}
