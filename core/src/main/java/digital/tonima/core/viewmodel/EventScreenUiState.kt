package digital.tonima.core.viewmodel

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AudioWarningState
import java.time.LocalDate
import java.time.YearMonth

data class EventScreenUiState(
    val events: List<Event> = emptyList(),
    val isGlobalAlarmEnabled: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val showAutostartSuggestion: Boolean = false,
    val showPurchaseConfirmation: Boolean = false,
    val showSubscriptionConfirmation: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val hasPostNotificationsPermission: Boolean = false,
    val hasExactAlarmPermission: Boolean = false,
    val hasFullScreenIntentPermission: Boolean = false,
    val audioWarning: AudioWarningState = AudioWarningState.NORMAL,
    val vibrateOnly: Boolean = false,
    val showRatingBottomSheet: Boolean = false,
    val allDayAlarmsEnabled: Boolean = true,
    val allDayAlarmHour: Int = 9,
    val alarmOffsetMinutes: Long = 0L,
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val enabledCalendarIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val snoozeTimeMinutes: Int = 10,
    val dailyBriefing: String? = null,
    val isGeneratingBriefing: Boolean = false,
    val isProUser: Boolean = false,
    val isAiUser: Boolean = false,
    val aiResponse: String? = null,
    val isAskingAi: Boolean = false,
    val lastAiQuestion: String? = null,
    val isSpeaking: Boolean = false,
    val chatHistory: List<ChatMessage> = emptyList(),
    val showCreateEventDialog: Boolean = false,
    val voiceEventData: VoiceEventData? = null,
    val showAiSuggestionsDialog: Boolean = false,
    val isLocationAlarmEnabled: Boolean = false,
    val preferredTransportMode: String = "driving",
    val hasLocationPermission: Boolean = false,
    val hasBackgroundLocationPermission: Boolean = false,
    /** Holds a CRITICAL AI-generated intent that is waiting for user confirmation. */
    val pendingAIAction: EventIntent? = null,
)
