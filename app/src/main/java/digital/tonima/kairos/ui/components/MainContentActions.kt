package digital.tonima.kairos.ui.components

import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.InsightsPeriod
import digital.tonima.core.viewmodel.uimodel.EventUiModel

data class EventActions(
    val onRefresh: () -> Unit,
    val onEventToggle: (event: EventUiModel, isEnabled: Boolean, disableAllOccurrences: Boolean) -> Unit,
    val onEventVibrateToggle: (event: EventUiModel, vibrateOnly: Boolean) -> Unit,
    val onMonthChanged: (Long) -> Unit,
    val onDateSelected: (Long) -> Unit,
    val onEventClick: (EventUiModel) -> Unit,
    val onReturnToToday: () -> Unit,
    val onSearchQueryChanged: (String) -> Unit = {},
    val onJoinMeeting: (String) -> Unit = {},
    val onCopyMeetingUrl: (String) -> Unit = {},
    val onFetchWeather: () -> Unit = {},
    val onCreateEvent: (
        calendarId: Long,
        title: String,
        description: String?,
        location: String?,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean,
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    val onDismissCreateEvent: () -> Unit = {},
    val onInsightsPeriodChange: (InsightsPeriod) -> Unit = {},
)

data class SettingsActions(
    val onToggle: (Boolean) -> Unit,
    val onDismissAutostart: () -> Unit,
    val onVibrateToggle: (Boolean) -> Unit,
    val onAllDayAlarmsToggle: (Boolean) -> Unit,
    val onAllDayAlarmHourChanged: (Int) -> Unit,
    val onAlarmOffsetChanged: (AlarmOffset) -> Unit,
    val onSnoozeTimeChanged: (Int) -> Unit = {},
    val onSkipWeekendsToggle: (Boolean) -> Unit = {},
    val onAutoDismissMinutesChanged: (Int) -> Unit = {},
    val onCalendarFilterToggle: (calendarId: Long, enabled: Boolean) -> Unit = { _, _ -> },
    val onLocationAlarmToggle: (Boolean) -> Unit = {},
    val onTransportModeChanged: (String) -> Unit = {},
    val onTemperatureUnitToggle: (Boolean) -> Unit = {},
    val onGoogleSignInClick: () -> Unit = {},
    val onGoogleSignOutClick: () -> Unit = {},
    val onCloseSettings: () -> Unit = {},
    val onCustomRingtoneSelected: (String?) -> Unit = {},
    val onCheckPermissions: () -> Unit = {},
    val onSkipExactAlarmPermission: () -> Unit = {},
    val onSkipFullScreenIntentPermission: () -> Unit = {},
)

data class AiActions(
    val onGenerateBriefing: () -> Unit = {},
    val onUpgradeToPro: () -> Unit = {},
    val onSubscriptionRequest: () -> Unit = {},
    val onVoiceCaptureClick: () -> Unit = {},
    val onClearAiResponse: () -> Unit = {},
    val onSpeakAiResponse: () -> Unit = {},
    val onStopSpeaking: () -> Unit = {},
    val onReply: () -> Unit = {},
    val onDismissSuggestions: () -> Unit = {},
    val onSuggestionClick: (String) -> Unit = {},
)
