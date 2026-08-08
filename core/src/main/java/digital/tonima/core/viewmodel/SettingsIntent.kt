package digital.tonima.core.viewmodel

import digital.tonima.core.model.AlarmOffset

sealed class SettingsIntent : BaseIntent {
    data class ToggleGlobalAlarms(val enabled: Boolean) : SettingsIntent()

    data class ToggleVibrateOnly(val enabled: Boolean) : SettingsIntent()

    data class ToggleAllDayAlarms(val enabled: Boolean) : SettingsIntent()

    data class UpdateAllDayAlarmHour(val hour: Int) : SettingsIntent()

    data class UpdateAlarmOffset(val offset: AlarmOffset) : SettingsIntent()

    data class UpdateSnoozeTime(val minutes: Int) : SettingsIntent()

    data class ToggleSkipWeekends(val enabled: Boolean) : SettingsIntent()

    data class UpdateAutoDismissMinutes(val minutes: Int) : SettingsIntent()

    data class ToggleLocationAlarm(val enabled: Boolean) : SettingsIntent()

    data class ToggleAutoJoin(val enabled: Boolean) : SettingsIntent()

    data class ToggleAutoFocusMode(val enabled: Boolean) : SettingsIntent()

    data class ChangeTransportMode(val mode: String) : SettingsIntent()

    data class ToggleTemperatureUnit(val isCelsius: Boolean) : SettingsIntent()

    object DismissAutostartSuggestion : SettingsIntent()

    object CheckPermissions : SettingsIntent()

    object SkipExactAlarmPermission : SettingsIntent()

    object SkipFullScreenIntentPermission : SettingsIntent()

    object OpenSettings : SettingsIntent()

    object CloseSettings : SettingsIntent()

    data class UpdateCustomRingtoneUri(val uri: String?) : SettingsIntent()
}
