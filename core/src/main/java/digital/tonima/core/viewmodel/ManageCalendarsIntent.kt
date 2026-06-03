package digital.tonima.core.viewmodel

import digital.tonima.core.model.DeviceCalendar

sealed class ManageCalendarsIntent {
    object LoadCalendars : ManageCalendarsIntent()

    data class OpenEditDialog(val calendar: DeviceCalendar) : ManageCalendarsIntent()

    object CloseEditDialog : ManageCalendarsIntent()

    data class UpdateEditName(val name: String) : ManageCalendarsIntent()

    data class UpdateEditColor(val color: Int) : ManageCalendarsIntent()

    object SaveEdit : ManageCalendarsIntent()

    data class DeleteCalendar(val calendarId: Long) : ManageCalendarsIntent()
}
