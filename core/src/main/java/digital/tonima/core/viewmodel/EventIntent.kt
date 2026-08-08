package digital.tonima.core.viewmodel

import android.content.Intent
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.Event
import digital.tonima.core.model.InsightsPeriod
import java.time.LocalDate
import java.time.YearMonth

sealed class EventIntent {
    object RefreshEvents : EventIntent()

    data class ChangeMonth(val yearMonth: YearMonth) : EventIntent()

    data class SelectDate(val date: LocalDate) : EventIntent()

    data class JoinMeeting(val meetingUrl: String) : EventIntent()

    data class CopyMeetingUrl(val meetingUrl: String) : EventIntent()

    data object ReturnToToday : EventIntent()

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

    data class ToggleSkipWeekends(val enabled: Boolean) : EventIntent()

    data class UpdateAutoDismissMinutes(val minutes: Int) : EventIntent()

    data class ToggleLocationAlarm(val enabled: Boolean) : EventIntent()

    data class ToggleAutoJoin(val enabled: Boolean) : EventIntent()

    data class ToggleAutoFocusMode(val enabled: Boolean) : EventIntent()

    data class ChangeTransportMode(val mode: String) : EventIntent()

    object FetchWeather : EventIntent()

    data class ToggleTemperatureUnit(val isCelsius: Boolean) : EventIntent()

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
        val requestMeetLink: Boolean = false,
    ) : EventIntent()

    object DismissAutostartSuggestion : EventIntent()

    object CheckPermissions : EventIntent()

    object LoadCalendars : EventIntent()

    data class ToggleCalendarFilter(val calendarId: Long, val enabled: Boolean) : EventIntent()

    object ClearCalendarFilter : EventIntent()

    data object RateNow : EventIntent()

    object RateLater : EventIntent()

    object RateNever : EventIntent()

    object UpgradeToProRequest : EventIntent()

    object UpgradeToProIARequest : EventIntent()

    data class SearchQueryChanged(val query: String) : EventIntent()

    data class ChangeBottomTab(val tabIndex: Int) : EventIntent()

    // UI Dialog state intents
    data class ShowCreateEventDialog(val voiceEventData: VoiceEventData? = null) : EventIntent()

    object DismissCreateEventDialog : EventIntent()

    object ShowAiSuggestionsDialog : EventIntent()

    object DismissAiSuggestionsDialog : EventIntent()

    // Permission specific intents
    object SkipExactAlarmPermission : EventIntent()

    object SkipFullScreenIntentPermission : EventIntent()

    object OpenSettings : EventIntent()

    object CloseSettings : EventIntent()

    data class UpdateCustomRingtoneUri(val uri: String?) : EventIntent()

    // ── Chat Screen intents ─────────────────────────────────────────────
    object OpenChatHistoryScreen : EventIntent()

    object CloseChatHistoryScreen : EventIntent()

    data class OpenChatDetail(val conversationId: Long) : EventIntent()

    object CloseChatDetail : EventIntent()

    data class CreateNewChat(val title: String) : EventIntent()

    data class DeleteChat(val conversationId: Long) : EventIntent()

    // ── AI Agent intents ────────────────────────────────────────────────

    /** User approved the pending CRITICAL action queued by the AI agent. */
    object ApprovePendingAction : EventIntent()

    /** User rejected the pending CRITICAL action queued by the AI agent. */
    object RejectPendingAction : EventIntent()

    /**
     * AI Agent intent to notify the user that they are running late.
     * This is used by the AI to suggest sending a message to participants.
     */
    data class NotifyRunningLate(val eventId: String, val message: String) : EventIntent()

    /** AI Agent intent to toggle Do Not Disturb mode. */
    data class ToggleFocusMode(val enabled: Boolean) : EventIntent()

    data class RescheduleEvent(
        val eventId: String,
        val newStartTime: Long,
        val newEndTime: Long,
    ) : EventIntent()

    data class CategorizeEvent(
        val eventId: String,
        val category: String,
    ) : EventIntent()

    data class CreateFocusBlock(
        val startTime: Long,
        val endTime: Long,
        val title: String = "Foco (AI Sugestão)",
    ) : EventIntent()

    data class SummarizeMeetTranscript(
        val meetingUrl: String,
    ) : EventIntent()

    object SignInWithGoogle : EventIntent()

    object SignOutFromGoogle : EventIntent()

    data class HandleGoogleSignInResult(val resultData: Intent?) : EventIntent()

    data class ChangeInsightsPeriod(val period: InsightsPeriod) : EventIntent()

    data class AnalyzeSchedule(val timeframe: String) : EventIntent()

    object OpenImportCalendarScreen : EventIntent()

    object CloseImportCalendarScreen : EventIntent()

    object OpenManageCalendarsScreen : EventIntent()

    object CloseManageCalendarsScreen : EventIntent()
}
