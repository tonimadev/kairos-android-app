package digital.tonima.kairos.ui.components

import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.Event
import java.time.LocalDate
import java.time.YearMonth

data class EventActions(
    val onRefresh: () -> Unit,
    val onEventToggle: (event: Event, isEnabled: Boolean, disableAllOccurrences: Boolean) -> Unit,
    val onEventVibrateToggle: (event: Event, vibrateOnly: Boolean) -> Unit,
    val onMonthChanged: (YearMonth) -> Unit,
    val onDateSelected: (LocalDate) -> Unit,
    val onEventClick: (Event) -> Unit,
    val onReturnToToday: () -> Unit,
    val onSearchQueryChanged: (String) -> Unit = {},
    val onJoinMeeting: (String) -> Unit = {},
    val onCopyMeetingUrl: (String) -> Unit = {},
    val onFetchWeather: () -> Unit = {},
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
)
