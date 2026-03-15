package digital.tonima.kairos.wear.ui

sealed class WearCalendarIntent {
    object RequestRescan : WearCalendarIntent()

    data class ToggleGlobalAlarm(val enabled: Boolean) : WearCalendarIntent()

    object ReloadFromCache : WearCalendarIntent()
}
