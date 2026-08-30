package digital.tonima.core.viewmodel

import androidx.compose.runtime.Immutable
import digital.tonima.core.model.DeviceCalendar

@Immutable
data class ManageCalendarsUiState(
    val calendars: List<DeviceCalendar> = emptyList(),
    val isLoading: Boolean = true,
    val showEditDialog: Boolean = false,
    val selectedCalendarId: Long? = null,
    val editName: String = "",
    val editColor: Int = 0xFF2196F3.toInt(),
    val effect: ManageCalendarsSideEffect? = null,
)
