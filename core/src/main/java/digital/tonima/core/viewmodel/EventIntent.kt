package digital.tonima.core.viewmodel

import digital.tonima.core.model.InsightsPeriod
import digital.tonima.core.viewmodel.uimodel.EventUiModel

sealed class EventIntent : BaseIntent {
    data object ConsumeEffect : EventIntent()

    object RefreshEvents : EventIntent()

    data class ChangeMonth(val yearMonth: Long) : EventIntent()

    data class SelectDate(val date: Long) : EventIntent()

    data class JoinMeeting(val meetingUrl: String) : EventIntent()

    data class CopyMeetingUrl(val meetingUrl: String) : EventIntent()

    object ReturnToToday : EventIntent()

    data class ToggleEventAlarm(
        val event: EventUiModel,
        val enabled: Boolean,
        val allOccurrences: Boolean = false,
    ) : EventIntent()

    data class ToggleEventVibrate(val event: EventUiModel, val enabled: Boolean) : EventIntent()

    object FetchWeather : EventIntent()

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

    object LoadCalendars : EventIntent()

    data class ToggleCalendarFilter(val calendarId: Long, val enabled: Boolean) : EventIntent()

    object ClearCalendarFilter : EventIntent()

    object RateNow : EventIntent()

    object RateLater : EventIntent()

    object RateNever : EventIntent()

    object UpgradeToProRequest : EventIntent()

    object UpgradeToProIARequest : EventIntent()

    data class SearchQueryChanged(val query: String) : EventIntent()

    data class ChangeBottomTab(val tabIndex: Int) : EventIntent()

    data class ShowCreateEventDialog(val voiceEventData: VoiceEventData? = null) : EventIntent()

    object DismissCreateEventDialog : EventIntent()

    object ShowAiSuggestionsDialog : EventIntent()

    data class GenerateDailyBriefing(val language: String) : EventIntent()

    data class ChangeInsightsPeriod(val period: InsightsPeriod) : EventIntent()

    data class NotifyRunningLate(val eventId: String, val message: String) : EventIntent()

    data class ToggleFocusMode(val enabled: Boolean) : EventIntent()

    data class CreateFocusBlock(
        val startTime: Long,
        val endTime: Long,
        val title: String = "Foco (AI Sugestão)",
    ) : EventIntent()

    object OpenImportCalendarScreen : EventIntent()

    object CloseImportCalendarScreen : EventIntent()

    object OpenManageCalendarsScreen : EventIntent()

    object CloseManageCalendarsScreen : EventIntent()
}
