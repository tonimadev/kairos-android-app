package digital.tonima.core.viewmodel

import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.Event
import java.time.LocalDate
import java.time.YearMonth

sealed class EventIntent {
    object RefreshEvents : EventIntent()

    data class ChangeMonth(val yearMonth: YearMonth) : EventIntent()

    data class SelectDate(val date: LocalDate) : EventIntent()

    object ReturnToToday : EventIntent()

    data class ToggleGlobalAlarms(val enabled: Boolean) : EventIntent()

    data class ToggleVibrateOnly(val enabled: Boolean) : EventIntent()

    data class ToggleAllDayAlarms(val enabled: Boolean) : EventIntent()

    data class UpdateAllDayAlarmHour(val hour: Int) : EventIntent()

    data class ToggleEventAlarm(
        val event: Event,
        val enabled: Boolean,
        val allOccurrences: Boolean = false,
    ) : EventIntent()

    data class ToggleEventVibrate(val event: Event, val enabled: Boolean) : EventIntent()

    data class UpdateAlarmOffset(val offset: AlarmOffset) : EventIntent()

    data class UpdateSnoozeTime(val minutes: Int) : EventIntent()

    data class ToggleLocationAlarm(val enabled: Boolean) : EventIntent()

    data class ChangeTransportMode(val mode: String) : EventIntent()

    data class AskAi(val question: String, val language: String) : EventIntent()

    data class GenerateDailyBriefing(val language: String) : EventIntent()

    object SpeakAiResponse : EventIntent()

    object StopSpeaking : EventIntent()

    object ClearAiResponse : EventIntent()

    data class CreateEvent(
        val calendarId: Long,
        val title: String,
        val description: String?,
        val location: String?,
        val startTime: Long,
        val endTime: Long,
        val isAllDay: Boolean,
    ) : EventIntent()

    object DismissAutostartSuggestion : EventIntent()

    object CheckPermissions : EventIntent()

    object LoadCalendars : EventIntent()

    data class ToggleCalendarFilter(val calendarId: Long, val enabled: Boolean) : EventIntent()

    object ClearCalendarFilter : EventIntent()

    data class RateNow(val activity: android.app.Activity? = null) : EventIntent()

    object RateLater : EventIntent()

    object RateNever : EventIntent()

    object DismissUpgradeConfirmation : EventIntent()

    object UpgradeToProRequest : EventIntent()

    object UpgradeToProIARequest : EventIntent()

    data class SearchQueryChanged(val query: String) : EventIntent()

    // UI Dialog state intents
    data class ShowCreateEventDialog(val voiceEventData: VoiceEventData? = null) : EventIntent()

    object DismissCreateEventDialog : EventIntent()

    object ShowAiSuggestionsDialog : EventIntent()

    object DismissAiSuggestionsDialog : EventIntent()

    // Permission specific intents
    object SkipExactAlarmPermission : EventIntent()

    object SkipFullScreenIntentPermission : EventIntent()

    // ── AI Agent intents ────────────────────────────────────────────────

    /** User approved the pending CRITICAL action queued by the AI agent. */
    object ApprovePendingAction : EventIntent()

    /** User rejected the pending CRITICAL action queued by the AI agent. */
    object RejectPendingAction : EventIntent()
}
