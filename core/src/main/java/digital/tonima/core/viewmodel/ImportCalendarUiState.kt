package digital.tonima.core.viewmodel

data class ImportCalendarUiState(
    val url: String = "",
    val fileUri: String? = null,
    val calendarName: String = "",
    val calendarColor: Int = 0xFF2196F3.toInt(),
    val alarmsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)
