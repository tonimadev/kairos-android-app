package digital.tonima.core.viewmodel

import digital.tonima.core.repository.AudioWarningState

data class SettingsUiState(
    val isGlobalAlarmEnabled: Boolean = true,
    val vibrateOnly: Boolean = false,
    val allDayAlarmsEnabled: Boolean = true,
    val allDayAlarmHour: Int = 9,
    val alarmOffsetMinutes: Long = 0L,
    val snoozeTimeMinutes: Int = 10,
    val skipWeekends: Boolean = false,
    val autoDismissMinutes: Int = 10,
    val isLocationAlarmEnabled: Boolean = false,
    val preferredTransportMode: String = "driving",
    val isTemperatureInCelsius: Boolean = true,
    val isAutoJoinEnabled: Boolean = false,
    val isAutoFocusModeEnabled: Boolean = false,
    val showSettingsScreen: Boolean = false,
    val customRingtoneUri: String? = null,
    val showAutostartSuggestion: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val hasPostNotificationsPermission: Boolean = false,
    val hasExactAlarmPermission: Boolean = false,
    val hasFullScreenIntentPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasBackgroundLocationPermission: Boolean = false,
    val skippedExactAlarmPermission: Boolean = false,
    val skippedFullScreenIntentPermission: Boolean = false,
    val audioWarning: AudioWarningState = AudioWarningState.NORMAL,
)
