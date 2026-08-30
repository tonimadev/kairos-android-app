package digital.tonima.core.viewmodel

sealed class ImportCalendarIntent : BaseIntent {
    data object ConsumeEffect : ImportCalendarIntent()

    data class UpdateUrl(val url: String) : ImportCalendarIntent()

    data class FileSelected(val uri: String) : ImportCalendarIntent()

    data class UpdateName(val name: String) : ImportCalendarIntent()

    data class UpdateColor(val color: Int) : ImportCalendarIntent()

    data class ToggleAlarms(val enabled: Boolean) : ImportCalendarIntent()

    object SubmitImport : ImportCalendarIntent()

    object DismissError : ImportCalendarIntent()

    object ResetSuccess : ImportCalendarIntent()
}
