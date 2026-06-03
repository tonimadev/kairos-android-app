package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.ai.AITool
import digital.tonima.core.ai.AIToolResult
import digital.tonima.core.ai.ActionRegistry
import digital.tonima.core.ai.RiskLevel
import digital.tonima.core.ai.model.AIAgentResponse
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.Event
import digital.tonima.core.review.ReviewManager
import digital.tonima.core.usecases.AskAiAgentUseCase
import digital.tonima.core.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.usecases.CancelEventAlarmUseCase
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.ClearChatHistoryUseCase
import digital.tonima.core.usecases.CreateConversationUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.DeleteConversationUseCase
import digital.tonima.core.usecases.FetchMeetingTranscriptUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.GetChatHistoryUseCase
import digital.tonima.core.usecases.GetCurrentLocationUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.usecases.GetGoogleSignInIntentUseCase
import digital.tonima.core.usecases.GetMeetingTimeStatsUseCase
import digital.tonima.core.usecases.GetRegisteredAiToolsUseCase
import digital.tonima.core.usecases.GetWeatherUseCase
import digital.tonima.core.usecases.HandleGoogleSignInResultUseCase
import digital.tonima.core.usecases.InsertChatMessageUseCase
import digital.tonima.core.usecases.IsGoogleSignedInUseCase
import digital.tonima.core.usecases.LogEventUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.ObserveChatHistoryUseCase
import digital.tonima.core.usecases.ObserveConversationsUseCase
import digital.tonima.core.usecases.ObserveDailyBriefingUseCase
import digital.tonima.core.usecases.ObserveRingerModeUseCase
import digital.tonima.core.usecases.ProcessAiResponseUseCase
import digital.tonima.core.usecases.ScheduleEventAlarmUseCase
import digital.tonima.core.usecases.SignOutFromGoogleUseCase
import digital.tonima.core.usecases.SpeakTextUseCase
import digital.tonima.core.usecases.ToggleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventVibrateUseCase
import digital.tonima.core.usecases.ToggleFocusModeUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.usecases.UpdateWidgetUseCase
import digital.tonima.core.util.toOpenWeatherLang
import digital.tonima.kairos.core.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class EventViewModel
    @Inject
    @Suppress("LongParameterList")
    constructor(
        proUserProvider: ProUserProvider,
        private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
        private val getAvailableCalendarsUseCase: GetAvailableCalendarsUseCase,
        private val createEventUseCase: CreateEventUseCase,
        private val observeAppPreferencesUseCase: ObserveAppPreferencesUseCase,
        private val updateAppPreferenceUseCase: UpdateAppPreferenceUseCase,
        private val toggleEventAlarmUseCase: ToggleEventAlarmUseCase,
        private val toggleEventVibrateUseCase: ToggleEventVibrateUseCase,
        private val scheduleEventAlarmUseCase: ScheduleEventAlarmUseCase,
        private val cancelEventAlarmUseCase: CancelEventAlarmUseCase,
        private val generateDailyBriefingUseCase: GenerateDailyBriefingUseCase,
        private val askAiAgentUseCase: AskAiAgentUseCase,
        private val calculateDepartureTimeUseCase: CalculateDepartureTimeUseCase,
        private val observeDailyBriefingUseCase: ObserveDailyBriefingUseCase,
        private val getRegisteredAiToolsUseCase: GetRegisteredAiToolsUseCase,
        private val processAiResponseUseCase: ProcessAiResponseUseCase,
        private val speakTextUseCase: SpeakTextUseCase,
        private val updateWidgetUseCase: UpdateWidgetUseCase,
        private val observeChatHistoryUseCase: ObserveChatHistoryUseCase,
        private val getChatHistoryUseCase: GetChatHistoryUseCase,
        private val insertChatMessageUseCase: InsertChatMessageUseCase,
        private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
        private val logEventUseCase: LogEventUseCase,
        private val observeRingerModeUseCase: ObserveRingerModeUseCase,
        private val checkPermissionsUseCase: CheckPermissionsUseCase,
        private val toggleFocusModeUseCase: ToggleFocusModeUseCase,
        private val reviewManager: ReviewManager,
        private val fetchMeetingTranscriptUseCase: FetchMeetingTranscriptUseCase,
        private val isGoogleSignedIn: IsGoogleSignedInUseCase,
        private val getGoogleSignInIntent: GetGoogleSignInIntentUseCase,
        private val handleGoogleSignInResultUseCase: HandleGoogleSignInResultUseCase,
        private val signOutFromGoogle: SignOutFromGoogleUseCase,
        private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
        private val getWeatherUseCase: GetWeatherUseCase,
        private val getMeetingTimeStatsUseCase: GetMeetingTimeStatsUseCase,
        private val observeConversationsUseCase: ObserveConversationsUseCase,
        private val createConversationUseCase: CreateConversationUseCase,
        private val deleteConversationUseCase: DeleteConversationUseCase,
    ) : ViewModel(), ProUserProvider by proUserProvider {
        private val _uiState = MutableStateFlow(EventScreenUiState())
        val uiState = _uiState.asStateFlow()

        private val _sideEffect = Channel<EventSideEffect>(Channel.BUFFERED)
        val sideEffect = _sideEffect.receiveAsFlow()

        private var chatHistoryJob: kotlinx.coroutines.Job? = null

        init {
            observePreferences()
            observeRingerMode()
            observeDailyBriefing()
            observeConversations()
            handleIntent(EventIntent.CheckPermissions)

            _uiState.update { it.copy(isGoogleConnected = isGoogleSignedIn()) }

            viewModelScope.launch {
                val stats = getMeetingTimeStatsUseCase(_uiState.value.selectedInsightsPeriod)
                _uiState.update { it.copy(meetingStats = stats) }
            }

            viewModelScope.launch {
                isAiUser.collect { isAi ->
                    _uiState.update { it.copy(isAiUser = isAi) }
                }
            }

            viewModelScope.launch {
                isProUser.collect { isPro ->
                    _uiState.update { it.copy(isProUser = isPro) }
                }
            }
        }

        fun handleIntent(intent: EventIntent) {
            logEventUseCase(intent)
            viewModelScope.launch {
                when (intent) {
                    // Grouped: Calendar intents
                    is EventIntent.RefreshEvents,
                    is EventIntent.ChangeMonth,
                    is EventIntent.SelectDate,
                    EventIntent.ReturnToToday,
                    EventIntent.LoadCalendars,
                    is EventIntent.ToggleCalendarFilter,
                    EventIntent.ClearCalendarFilter,
                    is EventIntent.CreateEvent,
                    -> handleCalendarIntent(intent)

                    // Grouped: Settings intents
                    is EventIntent.ToggleGlobalAlarms,
                    is EventIntent.ToggleVibrateOnly,
                    is EventIntent.ToggleAllDayAlarms,
                    is EventIntent.UpdateAllDayAlarmHour,
                    is EventIntent.UpdateAlarmOffset,
                    is EventIntent.UpdateSnoozeTime,
                    is EventIntent.ToggleSkipWeekends,
                    is EventIntent.UpdateAutoDismissMinutes,
                    is EventIntent.ToggleLocationAlarm,
                    is EventIntent.ToggleAutoJoin,
                    is EventIntent.ToggleAutoFocusMode,
                    is EventIntent.ChangeTransportMode,
                    is EventIntent.ToggleTemperatureUnit,
                    EventIntent.FetchWeather,
                    -> handleSettingsIntent(intent)

                    // Grouped: Event action intents
                    is EventIntent.JoinMeeting,
                    is EventIntent.CopyMeetingUrl,
                    is EventIntent.ToggleEventAlarm,
                    is EventIntent.ToggleEventVibrate,
                    -> handleEventActionIntent(intent)

                    // Grouped: AI intents
                    is EventIntent.AskAi,
                    is EventIntent.GenerateDailyBriefing,
                    EventIntent.SpeakAiResponse,
                    EventIntent.StopSpeaking,
                    EventIntent.ClearAiResponse,
                    -> handleAiIntent(intent)

                    // Grouped: permission intents
                    EventIntent.CheckPermissions,
                    EventIntent.SkipExactAlarmPermission,
                    EventIntent.SkipFullScreenIntentPermission,
                    -> handlePermissionIntent(intent)

                    // Grouped: Special action intents
                    EventIntent.ApprovePendingAction,
                    EventIntent.RejectPendingAction,
                    is EventIntent.NotifyRunningLate,
                    is EventIntent.ToggleFocusMode,
                    EventIntent.SignInWithGoogle,
                    EventIntent.SignOutFromGoogle,
                    is EventIntent.HandleGoogleSignInResult,
                    is EventIntent.SummarizeMeetTranscript,
                    -> handleSpecialActionIntent(intent)

                    // Grouped: UI / dialog / rating intents
                    else -> handleUiIntent(intent)
                }
            }
        }

        private fun handleCalendarIntent(intent: EventIntent) {
            when (intent) {
                is EventIntent.RefreshEvents -> refreshEvents()
                is EventIntent.ChangeMonth -> onMonthChanged(intent.yearMonth)
                is EventIntent.SelectDate -> _uiState.update { it.copy(selectedDate = intent.date) }
                EventIntent.ReturnToToday -> returnToToday()
                EventIntent.LoadCalendars -> loadAvailableCalendars()
                is EventIntent.ToggleCalendarFilter ->
                    viewModelScope.launch { onCalendarFilterToggle(intent.calendarId, intent.enabled) }

                EventIntent.ClearCalendarFilter ->
                    viewModelScope.launch {
                        updateAppPreferenceUseCase.setEnabledCalendarIds(emptySet())
                    }

                is EventIntent.CreateEvent -> createEvent(intent)
                else -> Unit
            }
        }

        private fun handleSettingsIntent(intent: EventIntent) {
            viewModelScope.launch {
                when (intent) {
                    is EventIntent.ToggleGlobalAlarms ->
                        updateAppPreferenceUseCase.setGlobalAlarmEnabled(intent.enabled)

                    is EventIntent.ToggleVibrateOnly -> updateAppPreferenceUseCase.setVibrateOnly(intent.enabled)
                    is EventIntent.ToggleAllDayAlarms ->
                        updateAppPreferenceUseCase.setAllDayAlarmsEnabled(intent.enabled)

                    is EventIntent.UpdateAllDayAlarmHour -> updateAppPreferenceUseCase.setAllDayAlarmHour(intent.hour)
                    is EventIntent.UpdateAlarmOffset ->
                        updateAppPreferenceUseCase.setAlarmOffsetMinutes(intent.offset.minutes)

                    is EventIntent.UpdateSnoozeTime -> updateAppPreferenceUseCase.setSnoozeTimeMinutes(intent.minutes)
                    is EventIntent.ToggleSkipWeekends ->
                        updateAppPreferenceUseCase.setSkipWeekendsEnabled(intent.enabled)

                    is EventIntent.UpdateAutoDismissMinutes ->
                        updateAppPreferenceUseCase.setAutoDismissMinutes(intent.minutes)

                    is EventIntent.ToggleLocationAlarm -> onLocationAlarmToggle(intent.enabled)
                    is EventIntent.ToggleAutoJoin -> updateAppPreferenceUseCase.setAutoJoinEnabled(intent.enabled)
                    is EventIntent.ToggleAutoFocusMode ->
                        updateAppPreferenceUseCase.setAutoFocusModeEnabled(intent.enabled)

                    is EventIntent.ChangeTransportMode ->
                        updateAppPreferenceUseCase.setPreferredTransportMode(intent.mode)

                    is EventIntent.ToggleTemperatureUnit -> {
                        updateAppPreferenceUseCase.setTemperatureInCelsius(intent.isCelsius)
                        handleIntent(EventIntent.FetchWeather)
                    }

                    EventIntent.FetchWeather -> fetchWeather()
                    else -> Unit
                }
            }
        }

        private suspend fun handleEventActionIntent(intent: EventIntent) {
            when (intent) {
                is EventIntent.JoinMeeting -> _sideEffect.send(EventSideEffect.OpenMeetingUrl(intent.meetingUrl))
                is EventIntent.CopyMeetingUrl -> {
                    _sideEffect.send(
                        EventSideEffect.CopyToClipboard(
                            intent.meetingUrl,
                            UiText.StringResource(R.string.link_copied),
                        ),
                    )
                }

                is EventIntent.ToggleEventAlarm ->
                    toggleEventAlarmUseCase(intent.event, intent.enabled, intent.allOccurrences)

                is EventIntent.ToggleEventVibrate ->
                    toggleEventVibrateUseCase(intent.event, intent.enabled)

                else -> Unit
            }
        }

        private fun handleSpecialActionIntent(intent: EventIntent) {
            when (intent) {
                EventIntent.ApprovePendingAction -> executePendingAction()
                EventIntent.RejectPendingAction -> rejectPendingAction()
                is EventIntent.NotifyRunningLate -> handleNotifyRunningLate(intent)
                is EventIntent.ToggleFocusMode -> handleToggleFocusMode(intent)
                is EventIntent.SummarizeMeetTranscript -> handleSummarizeMeetTranscript(intent)
                EventIntent.SignInWithGoogle -> handleSignInWithGoogle()
                EventIntent.SignOutFromGoogle -> handleSignOutFromGoogle()
                is EventIntent.HandleGoogleSignInResult -> handleGoogleSignInResult(intent.resultData)
                is EventIntent.CreateFocusBlock -> handleCreateFocusBlock(intent)
                is EventIntent.AnalyzeSchedule -> handleAnalyzeSchedule(intent)
                else -> Unit
            }
        }

        private fun handleGoogleSignInResult(intent: android.content.Intent?) {
            viewModelScope.launch {
                val result = handleGoogleSignInResultUseCase(intent)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isGoogleConnected = true) }
                    _sideEffect.trySend(
                        EventSideEffect.ShowSnackbar(
                            UiText.StringResource(
                                R.string.google_logout_title,
                            ),
                        ),
                    )
                } else {
                    _sideEffect.trySend(
                        EventSideEffect.ShowSnackbar(
                            UiText.DynamicString("Login failed"),
                        ),
                    )
                }
            }
        }

        private fun handleSignInWithGoogle() {
            val signInIntent = getGoogleSignInIntent()
            _sideEffect.trySend(EventSideEffect.LaunchGoogleSignIn(signInIntent))
        }

        private fun handleSignOutFromGoogle() {
            viewModelScope.launch {
                signOutFromGoogle()
                _uiState.update { it.copy(isGoogleConnected = false) }
            }
        }

        private fun handleAiIntent(intent: EventIntent) {
            when (intent) {
                is EventIntent.AskAi -> askAi(intent.question, intent.language)
                is EventIntent.OpenChatHistoryScreen -> {
                    _uiState.update { it.copy(showChatHistoryScreen = true) }
                }
                is EventIntent.CloseChatHistoryScreen -> {
                    _uiState.update { it.copy(showChatHistoryScreen = false, selectedConversationId = null) }
                    chatHistoryJob?.cancel()
                }
                is EventIntent.OpenChatDetail -> {
                    _uiState.update { it.copy(selectedConversationId = intent.conversationId) }
                    observeChatHistory(intent.conversationId)
                }
                is EventIntent.CloseChatDetail -> {
                    _uiState.update { it.copy(selectedConversationId = null) }
                    chatHistoryJob?.cancel()
                }
                is EventIntent.CreateNewChat -> {
                    viewModelScope.launch {
                        val id = createConversationUseCase(intent.title)
                        handleIntent(EventIntent.OpenChatDetail(id))
                    }
                }
                is EventIntent.DeleteChat -> {
                    viewModelScope.launch {
                        deleteConversationUseCase(intent.conversationId)
                        if (_uiState.value.selectedConversationId == intent.conversationId) {
                            handleIntent(EventIntent.CloseChatDetail)
                        }
                    }
                }
                is EventIntent.GenerateDailyBriefing -> generateDailyBriefing(intent.language)
                EventIntent.SpeakAiResponse -> speakAiResponse()
                EventIntent.StopSpeaking -> stopSpeaking()
                EventIntent.ClearAiResponse -> clearAiResponse()
                else -> Unit
            }
        }

        private fun handlePermissionIntent(intent: EventIntent) {
            when (intent) {
                EventIntent.CheckPermissions -> checkPermissions()
                EventIntent.SkipExactAlarmPermission -> {
                    logcat { "User skipped exact alarm permission request - alarms will be inexact" }
                    viewModelScope.launch {
                        updateAppPreferenceUseCase.setExactAlarmPermissionSkipped(true)
                        checkPermissions()
                    }
                }

                EventIntent.SkipFullScreenIntentPermission -> {
                    logcat { "User skipped full-screen intent permission request" }
                    viewModelScope.launch {
                        updateAppPreferenceUseCase.setFullScreenIntentPermissionSkipped(true)
                        checkPermissions()
                    }
                }

                else -> Unit
            }
        }

        private suspend fun handleUiIntent(intent: EventIntent) {
            when (intent) {
                EventIntent.DismissAutostartSuggestion ->
                    updateAppPreferenceUseCase.setAutostartSuggestionDismissed(true)

                EventIntent.UpgradeToProRequest ->
                    _uiState.update { it.copy(showPurchaseConfirmation = true) }

                EventIntent.UpgradeToProIARequest ->
                    _uiState.update { it.copy(showSubscriptionConfirmation = true) }

                EventIntent.DismissUpgradeConfirmation ->
                    _uiState.update {
                        it.copy(showSubscriptionConfirmation = false, showPurchaseConfirmation = false)
                    }

                is EventIntent.ChangeBottomTab ->
                    _uiState.update { it.copy(selectedBottomTab = intent.tabIndex) }

                EventIntent.OpenSettings ->
                    _uiState.update { it.copy(showSettingsScreen = true) }

                EventIntent.CloseSettings ->
                    _uiState.update { it.copy(showSettingsScreen = false) }

                EventIntent.OpenImportCalendarScreen ->
                    _uiState.update { it.copy(showImportCalendarScreen = true) }

                EventIntent.CloseImportCalendarScreen ->
                    _uiState.update { it.copy(showImportCalendarScreen = false) }

                EventIntent.OpenManageCalendarsScreen ->
                    _uiState.update { it.copy(showManageCalendarsScreen = true) }

                EventIntent.CloseManageCalendarsScreen ->
                    _uiState.update { it.copy(showManageCalendarsScreen = false) }

                is EventIntent.ChangeInsightsPeriod -> {
                    _uiState.update { it.copy(selectedInsightsPeriod = intent.period) }
                    viewModelScope.launch {
                        val stats = getMeetingTimeStatsUseCase(intent.period)
                        _uiState.update { it.copy(meetingStats = stats) }
                    }
                }

                is EventIntent.UpdateCustomRingtoneUri ->
                    _uiState.update { it.copy(customRingtoneUri = intent.uri) }

                is EventIntent.SearchQueryChanged ->
                    _uiState.update { it.copy(searchQuery = intent.query) }

                is EventIntent.ShowCreateEventDialog ->
                    _uiState.update {
                        it.copy(showCreateEventDialog = true, voiceEventData = intent.voiceEventData)
                    }

                EventIntent.DismissCreateEventDialog ->
                    _uiState.update { it.copy(showCreateEventDialog = false, voiceEventData = null) }

                EventIntent.ShowAiSuggestionsDialog ->
                    _uiState.update { it.copy(showAiSuggestionsDialog = true) }

                EventIntent.DismissAiSuggestionsDialog ->
                    _uiState.update { it.copy(showAiSuggestionsDialog = false) }

                is EventIntent.RateNow -> onRateNow(intent.activity)
                EventIntent.RateLater ->
                    _uiState.update { it.copy(showRatingBottomSheet = false) }

                EventIntent.RateNever -> {
                    updateAppPreferenceUseCase.setRatingCompleted(true)
                    _uiState.update { it.copy(showRatingBottomSheet = false) }
                }

                else -> Unit
            }
        }

        private fun observePreferences() {
            observeAppPreferencesUseCase().onEach { appPrefs ->
                val prevGlobalEnabled = _uiState.value.isGlobalAlarmEnabled
                _uiState.update {
                    it.copy(
                        isGlobalAlarmEnabled = appPrefs.isGlobalAlarmEnabled,
                        vibrateOnly = appPrefs.vibrateOnly,
                        allDayAlarmsEnabled = appPrefs.allDayAlarmsEnabled,
                        allDayAlarmHour = appPrefs.allDayAlarmHour,
                        alarmOffsetMinutes = appPrefs.alarmOffsetMinutes,
                        isLocationAlarmEnabled = appPrefs.isLocationAlarmEnabled,
                        preferredTransportMode = appPrefs.preferredTransportMode,
                        snoozeTimeMinutes = appPrefs.snoozeTimeMinutes,
                        skippedExactAlarmPermission = appPrefs.exactAlarmPermissionSkipped,
                        skippedFullScreenIntentPermission = appPrefs.fullScreenIntentPermissionSkipped,
                        showAutostartSuggestion = !appPrefs.autostartSuggestionDismissed,
                        skipWeekends = appPrefs.skipWeekendsEnabled,
                        autoDismissMinutes = appPrefs.autoDismissMinutes,
                        isTemperatureInCelsius = appPrefs.isTemperatureInCelsius,
                        isAutoJoinEnabled = appPrefs.isAutoJoinEnabled,
                        isAutoFocusModeEnabled = appPrefs.isAutoFocusModeEnabled,
                        enabledCalendarIds =
                            appPrefs.enabledCalendarIds.mapNotNull { it.toLongOrNull() }.toSet(),
                    )
                }

                if (prevGlobalEnabled && !appPrefs.isGlobalAlarmEnabled) {
                    _uiState.value.events.forEach { cancelEventAlarmUseCase(it) }
                }

                refreshEvents()
            }.launchIn(viewModelScope)

            isProUser.onEach { pro -> _uiState.update { it.copy(isProUser = pro) } }
                .launchIn(viewModelScope)
            isAiUser.onEach { aiUser -> _uiState.update { it.copy(isAiUser = aiUser) } }
                .launchIn(viewModelScope)
        }

        private fun observeRingerMode() {
            observeRingerModeUseCase().onEach { mode ->
                _uiState.update { it.copy(audioWarning = mode) }
            }.launchIn(viewModelScope)
        }

        private fun observeDailyBriefing() {
            observeDailyBriefingUseCase().onEach { briefing ->
                _uiState.update { it.copy(dailyBriefing = briefing) }
            }.launchIn(viewModelScope)
        }

        private fun observeConversations() {
            observeConversationsUseCase().onEach { list ->
                _uiState.update { it.copy(conversations = list) }
            }.launchIn(viewModelScope)
        }

        private fun observeChatHistory(conversationId: Long) {
            chatHistoryJob?.cancel()
            chatHistoryJob =
                observeChatHistoryUseCase(conversationId).onEach { messages ->
                    _uiState.update { it.copy(chatHistory = messages) }
                }.launchIn(viewModelScope)
        }

        private fun checkPermissions() {
            val p = checkPermissionsUseCase()
            _uiState.update {
                it.copy(
                    hasCalendarPermission = p.hasCalendarPermission,
                    hasPostNotificationsPermission = p.hasPostNotificationsPermission,
                    hasExactAlarmPermission = p.hasExactAlarmPermission || _uiState.value.skippedExactAlarmPermission,
                    hasFullScreenIntentPermission =
                        p.hasFullScreenIntentPermission ||
                            _uiState.value.skippedFullScreenIntentPermission,
                    hasLocationPermission = p.hasLocationPermission,
                    hasBackgroundLocationPermission = p.hasBackgroundLocationPermission,
                )
            }
            if (p.hasCalendarPermission) {
                refreshEvents()
                handleIntent(EventIntent.LoadCalendars)
            }
            if (p.hasLocationPermission) {
                handleIntent(EventIntent.FetchWeather)
            }
        }

        private fun onMonthChanged(yearMonth: YearMonth) {
            _uiState.update { it.copy(currentMonth = yearMonth, isRefreshing = true) }
            refreshEvents()
        }

        private fun refreshEvents() {
            if (!_uiState.value.hasCalendarPermission) return

            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                val calendarEvents = getEventsForMonthUseCase(_uiState.value.currentMonth)
                val appPrefs = observeAppPreferencesUseCase().first()

                val updatedEvents =
                    calendarEvents.map { event ->
                        val isInstanceDisabled =
                            appPrefs.disabledEventIds.contains(event.uniqueIntentId.toString())
                        val isSeriesDisabled =
                            appPrefs.disabledSeriesIds.contains(event.id.toString())
                        val isVibrateOnly =
                            appPrefs.vibrateOnlyEventIds.contains(event.uniqueIntentId.toString())
                        val isAlarmEnabled = !(isInstanceDisabled || isSeriesDisabled)

                        var departureTime: Long? = null
                        var travelTimeMinutes: Int? = null

                        if (isAlarmEnabled && _uiState.value.isAiUser && event.location != null) {
                            val departureInfo = calculateDepartureTimeUseCase(event)
                            departureTime = departureInfo?.departureTime
                            travelTimeMinutes = departureInfo?.travelTimeMinutes
                        }

                        event.copy(
                            isAlarmEnabled = isAlarmEnabled,
                            vibrateOnly = isVibrateOnly,
                            departureTime = departureTime,
                            travelTimeMinutes = travelTimeMinutes,
                        )
                    }
                val enrichedEvents = detectConflictsAndBuffers(updatedEvents)

                _uiState.update { it.copy(events = enrichedEvents, isRefreshing = false) }

                if (_uiState.value.isGlobalAlarmEnabled) {
                    scheduleImmediateEvents(enrichedEvents)
                }

                val stats = getMeetingTimeStatsUseCase(_uiState.value.selectedInsightsPeriod)
                _uiState.update { it.copy(meetingStats = stats) }
            }
        }

        /**
         * Detects scheduling conflicts (overlapping events) and back-to-back meetings
         * (gap < 5 minutes) and annotates events with `hasConflict` / `isBackToBack` flags.
         */
        private fun detectConflictsAndBuffers(events: List<Event>): List<Event> {
            if (events.size < 2) return events

            val nonAllDay = events.filter { !it.isAllDay && it.endTime > it.startTime }
            val conflictIds = mutableSetOf<Long>()
            val backToBackIds = mutableSetOf<Long>()

            for (i in nonAllDay.indices) {
                for (j in i + 1 until nonAllDay.size) {
                    val a = nonAllDay[i]
                    val b = nonAllDay[j]

                    // Conflict: events overlap
                    if (a.startTime < b.endTime && b.startTime < a.endTime) {
                        conflictIds.add(a.id)
                        conflictIds.add(b.id)
                    }

                    // Back-to-back: gap < 5 minutes (300_000 ms)
                    val gap = b.startTime - a.endTime
                    if (gap in 0..300_000) {
                        backToBackIds.add(a.id)
                        backToBackIds.add(b.id)
                    }
                }
            }

            return events.map { event ->
                event.copy(
                    hasConflict = event.id in conflictIds,
                    isBackToBack = event.id in backToBackIds,
                )
            }
        }

        private fun scheduleImmediateEvents(events: List<Event>) {
            val now = System.currentTimeMillis()
            val offsetMinutes = _uiState.value.alarmOffsetMinutes
            val windowEnd = now + TimeUnit.MINUTES.toMillis(75 + offsetMinutes)

            viewModelScope.launch {
                events.filter { it.isAlarmEnabled }.forEach { event ->
                    val triggerTime =
                        if (_uiState.value.isAiUser && event.location != null) {
                            calculateDepartureTimeUseCase(event)?.departureTime
                        } else {
                            null
                        }
                    val alarmFireTime =
                        triggerTime ?: (event.startTime - TimeUnit.MINUTES.toMillis(offsetMinutes))
                    if (alarmFireTime in (now + 1)..windowEnd) {
                        scheduleEventAlarmUseCase(event, triggerTime)
                    }
                }
            }
        }

        private fun returnToToday() {
            _uiState.update { it.copy(selectedDate = LocalDate.now(), currentMonth = YearMonth.now()) }
        }

        private fun onLocationAlarmToggle(enabled: Boolean) {
            if (!_uiState.value.isAiUser && enabled) {
                _uiState.update { it.copy(showSubscriptionConfirmation = true) }
                return
            }
            viewModelScope.launch { updateAppPreferenceUseCase.setLocationAlarmEnabled(enabled) }
        }

        private fun loadAvailableCalendars() {
            viewModelScope.launch {
                val calendars = getAvailableCalendarsUseCase()
                _uiState.update { it.copy(availableCalendars = calendars) }
            }
        }

        private suspend fun onCalendarFilterToggle(
            calendarId: Long,
            enabled: Boolean,
        ) {
            val allIds = _uiState.value.availableCalendars.map { it.id }.toSet()
            val current = _uiState.value.enabledCalendarIds.toMutableSet()

            logcat { "CalendarFilter: toggling id=$calendarId enabled=$enabled | allIds=$allIds | current=$current" }

            if (current.isEmpty()) current.addAll(allIds)
            if (enabled) current.add(calendarId) else current.remove(calendarId)

            val newLongSet: Set<Long> = if (current.containsAll(allIds)) emptySet() else current

            logcat { "CalendarFilter: newLongSet=$newLongSet" }

            _uiState.update { it.copy(enabledCalendarIds = newLongSet) }

            val newStringSet = newLongSet.map { it.toString() }.toSet()
            updateAppPreferenceUseCase.setEnabledCalendarIds(newStringSet)

            refreshEvents()
        }

        private fun generateDailyBriefing(language: String) {
            val ui = _uiState.value
            if (ui.dailyBriefing != null || ui.isGeneratingBriefing || !ui.isAiUser) return

            val today = LocalDate.now()
            val eventsToday =
                ui.events.filter {
                    Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() ==
                        today
                }

            viewModelScope.launch {
                _uiState.update { it.copy(isGeneratingBriefing = true) }
                val briefing = generateDailyBriefingUseCase(eventsToday, language)
                if (briefing != null) {
                    updateWidgetUseCase.updateDailyBriefingWidget()
                }
                _uiState.update { it.copy(isGeneratingBriefing = false) }
            }
        }

        private fun askAi(
            question: String?,
            language: String = java.util.Locale.getDefault().language,
        ) {
            if (_uiState.value.isAskingAi || !_uiState.value.isAiUser) return

            viewModelScope.launch {
                var convId = _uiState.value.selectedConversationId
                if (convId == null) {
                    convId = createConversationUseCase(question ?: "Nova Conversa")
                    _uiState.update { it.copy(selectedConversationId = convId) }
                    observeChatHistory(convId)
                }

                val currentHistory = getChatHistoryUseCase(convId)

                if (!question.isNullOrBlank()) {
                    val questionMsg = ChatMessage.Text(ChatMessage.Role.USER, question)
                    insertChatMessageUseCase(convId, questionMsg)
                }

                _uiState.update {
                    it.copy(
                        isAskingAi = true,
                        aiResponse = null,
                        lastAiQuestion = question ?: it.lastAiQuestion,
                    )
                }
                val eventsRecent = getEventsForMonthUseCase(_uiState.value.currentMonth)

                val agentResponse =
                    askAiAgentUseCase(
                        eventsRecent,
                        question,
                        language,
                        getRegisteredAiToolsUseCase(),
                        currentHistory,
                    )

                when (agentResponse) {
                    is AIAgentResponse.Text -> {
                        val answerMsg = ChatMessage.Text(ChatMessage.Role.ASSISTANT, agentResponse.content)
                        insertChatMessageUseCase(convId, answerMsg)
                        processAiResponse(agentResponse.content)
                    }

                    is AIAgentResponse.FunctionCall -> {
                        val callMsg = ChatMessage.FunctionCall(agentResponse.name, agentResponse.args)
                        insertChatMessageUseCase(convId, callMsg)
                        onAIFunctionCalled(
                            agentResponse.name,
                            agentResponse.args,
                        )
                    }

                    is AIAgentResponse.Empty -> Unit
                }

                _uiState.update { it.copy(isAskingAi = false) }
            }
        }

        private fun processAiResponse(response: String) {
            val trimmedResponse = response.trim()
            val hasJsonStart =
                trimmedResponse.contains("\"title\":") && trimmedResponse.contains("{")

            if (hasJsonStart) {
                parseVoiceEventData(trimmedResponse)?.let { voiceEventData ->
                    handleIntent(EventIntent.ShowCreateEventDialog(voiceEventData))
                    return
                }
            }
            _uiState.update {
                it.copy(
                    aiResponse = response,
                )
            }
            speak(response)
        }

        private fun parseVoiceEventData(jsonStr: String): VoiceEventData? {
            return try {
                val title =
                    Regex("\"title\":\\s*\"([^\"]+)\"").find(jsonStr)?.groupValues?.get(1)
                        ?: return null
                val description =
                    Regex("\"description\":\\s*\"([^\"]+)\"").find(jsonStr)?.groupValues?.get(1)
                val location =
                    Regex("\"location\":\\s*\"([^\"]+)\"").find(jsonStr)?.groupValues?.get(1)
                val startTime =
                    Regex("\"startTime\":\\s*(\\d+)").find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
                val endTime =
                    Regex("\"endTime\":\\s*(\\d+)").find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
                val isAllDay =
                    Regex("\"isAllDay\":\\s*(true|false)")
                        .find(jsonStr)?.groupValues?.get(1)?.toBoolean() ?: false
                VoiceEventData(title, description, location, startTime, endTime, isAllDay)
            } catch (_: Exception) {
                null
            }
        }

        private fun speak(text: String) {
            _uiState.update { it.copy(isSpeaking = true) }
            speakTextUseCase(text) { _uiState.update { it.copy(isSpeaking = false) } }
        }

        private fun speakAiResponse() {
            _uiState.value.aiResponse?.let {
                speak(it)
            }
        }

        private fun stopSpeaking() {
            speakTextUseCase.stop()
            _uiState.update { it.copy(isSpeaking = false) }
        }

        private fun clearAiResponse() {
            viewModelScope.launch {
                val convId = _uiState.value.selectedConversationId
                if (convId != null) {
                    clearChatHistoryUseCase(convId)
                }
                _uiState.update {
                    it.copy(
                        aiResponse = null,
                        chatHistory = emptyList(),
                        lastAiQuestion = null,
                    )
                }
                stopSpeaking()
            }
        }

        private fun createEvent(intent: EventIntent.CreateEvent) {
            logcat { "Criando evento: ${intent.title} no calendário ${intent.calendarId}" }
            viewModelScope.launch {
                val result =
                    createEventUseCase(
                        intent.calendarId,
                        intent.title,
                        intent.description,
                        intent.location,
                        intent.startTime,
                        intent.endTime,
                        intent.isAllDay,
                        intent.requestMeetLink,
                    )
                if (result != null) {
                    logcat { "Evento criado com sucesso, ID: $result" }
                    logEventUseCase.logEventCreated()
                    handleIntent(EventIntent.DismissCreateEventDialog)

                    // Navigate to the event date so the user can see it
                    val eventDate =
                        Instant.ofEpochMilli(intent.startTime)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    val eventMonth = YearMonth.from(eventDate)

                    _uiState.update { it.copy(selectedDate = eventDate) }
                    if (eventMonth != _uiState.value.currentMonth) {
                        onMonthChanged(eventMonth)
                    }

                    // Ensure the calendar is enabled so the user can see the new event
                    if (!_uiState.value.enabledCalendarIds.contains(intent.calendarId)) {
                        onCalendarFilterToggle(intent.calendarId, true)
                    }

                    // Small delay to allow ContentProvider/Instances table to sync
                    kotlinx.coroutines.delay(500)
                    refreshEvents()

                    _sideEffect.send(
                        EventSideEffect.ShowSnackbar(
                            UiText.StringResource(R.string.ai_agent_event_created),
                        ),
                    )
                } else {
                    logcat { "Falha ao criar evento: createEvent retornou null" }
                    _sideEffect.send(
                        EventSideEffect.AIToolError(
                            UiText.StringResource(R.string.ai_agent_event_creation_error),
                        ),
                    )
                }
            }
        }

        private fun onRateNow(activity: android.app.Activity?) {
            viewModelScope.launch {
                updateAppPreferenceUseCase.setRatingCompleted(true)
                _uiState.update { it.copy(showRatingBottomSheet = false) }
                activity?.let { reviewManager.requestReview(it) {} }
            }
        }

        private fun fetchWeather() {
            viewModelScope.launch {
                if (_uiState.value.isWeatherLoading) return@launch
                val isCelsius = _uiState.value.isTemperatureInCelsius
                val lang = java.util.Locale.getDefault().toOpenWeatherLang()
                _uiState.update { it.copy(isWeatherLoading = true, weatherError = null) }
                val locationStr = getCurrentLocationUseCase()
                if (locationStr != null) {
                    val parts = locationStr.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lon = parts[1].toDoubleOrNull()
                        if (lat != null && lon != null) {
                            val weatherData = getWeatherUseCase(lat, lon, isCelsius, lang)
                            if (weatherData != null) {
                                _uiState.update { it.copy(weather = weatherData, isWeatherLoading = false) }
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isWeatherLoading = false,
                                        weatherError = "Failed to fetch weather",
                                    )
                                }
                            }
                        } else {
                            _uiState.update { it.copy(isWeatherLoading = false, weatherError = "Invalid location") }
                        }
                    } else {
                        _uiState.update { it.copy(isWeatherLoading = false, weatherError = "Invalid location format") }
                    }
                } else {
                    _uiState.update { it.copy(isWeatherLoading = false, weatherError = "Location unavailable") }
                }
            }
        }

        // ── AI Agent: Function Calling entry-point ──────────────────────────

        /**
         * Entry-point called when the LLM response contains a function/tool call.
         *
         * Flow:
         * 1. Delegates to [ActionRegistry] to resolve the tool and parse arguments.
         * 2. Based on [RiskLevel]:
         *    - **SAFE** → dispatches the intent immediately.
         *    - **MODERATE** → dispatches immediately + emits a [EventSideEffect.ShowSnackbar].
         *    - **CRITICAL** → saves the intent in [EventScreenUiState.pendingAIAction] and
         *      emits [EventSideEffect.RequireUserConfirmation] so the UI can ask the user.
         */
        fun onAIFunctionCalled(
            toolName: String,
            args: Map<String, Any?>,
        ) {
            viewModelScope.launch {
                var convId = _uiState.value.selectedConversationId
                if (convId == null) {
                    convId = createConversationUseCase("Nova Conversa")
                    _uiState.update { it.copy(selectedConversationId = convId) }
                    observeChatHistory(convId)
                }
                when (val result = processAiResponseUseCase(toolName, args)) {
                    is AIToolResult.Success -> {
                        routeByRiskLevel(result)
                        val responseMsg =
                            ChatMessage
                                .FunctionResponse(
                                    toolName,
                                    mapOf("status" to "success", "message" to "Intent gerado e processado"),
                                )
                        insertChatMessageUseCase(convId, responseMsg)
                        askAi(null)
                    }

                    is AIToolResult.ToolNotFound -> {
                        logcat { "AI Agent: tool '${result.toolName}' not found" }
                        val responseMsg = ChatMessage.FunctionResponse(toolName, mapOf("error" to "Tool not found"))
                        insertChatMessageUseCase(convId, responseMsg)
                        askAi(null)
                        _sideEffect.trySend(
                            EventSideEffect.AIToolError(
                                UiText.StringResource(
                                    R.string.ai_agent_tool_not_found,
                                    listOf(result.toolName),
                                ),
                            ),
                        )
                    }

                    is AIToolResult.InvalidArguments -> {
                        logcat { "AI Agent: invalid args for '${result.toolName}': ${result.args}" }
                        val responseMsg = ChatMessage.FunctionResponse(toolName, mapOf("error" to "Invalid arguments"))
                        insertChatMessageUseCase(convId, responseMsg)
                        askAi(null)
                        _sideEffect.trySend(
                            EventSideEffect.AIToolError(
                                UiText.StringResource(
                                    R.string.ai_agent_invalid_args,
                                    listOf(result.toolName),
                                ),
                            ),
                        )
                    }
                }
            }
        }

        private fun routeByRiskLevel(result: AIToolResult.Success) {
            val tool = result.tool
            val intent = result.intent

            when (tool.riskLevel) {
                RiskLevel.SAFE -> {
                    logcat { "AI Agent [SAFE]: dispatching ${intent::class.simpleName}" }
                    handleIntent(intent)
                }

                RiskLevel.MODERATE -> {
                    logcat { "AI Agent [MODERATE]: dispatching ${intent::class.simpleName} + snackbar" }
                    handleIntent(intent)
                    _sideEffect.trySend(
                        EventSideEffect.ShowSnackbar(
                            UiText.StringResource(
                                R.string.ai_agent_snackbar_executed,
                                listOf(tool.name),
                            ),
                        ),
                    )
                }

                RiskLevel.CRITICAL -> {
                    logcat { "AI Agent [CRITICAL]: pausing for confirmation — ${intent::class.simpleName}" }
                    _uiState.update { it.copy(pendingAIAction = intent) }
                    _sideEffect.trySend(
                        EventSideEffect.RequireUserConfirmation(
                            title = UiText.StringResource(R.string.ai_agent_confirmation_title),
                            message = formatConfirmationMessage(tool, intent),
                        ),
                    )
                }
            }
        }

        // ── AI Agent: Approve / Reject pending CRITICAL action ──────────────

        private fun executePendingAction() {
            val pending = _uiState.value.pendingAIAction ?: return
            _uiState.update { it.copy(pendingAIAction = null) }

            if (pending is EventIntent.CreateEvent) {
                handleIntent(
                    EventIntent.ShowCreateEventDialog(
                        voiceEventData =
                            VoiceEventData(
                                title = pending.title,
                                description = pending.description,
                                location = pending.location,
                                startTime = pending.startTime,
                                endTime = pending.endTime,
                                isAllDay = pending.isAllDay,
                            ),
                    ),
                )
            } else {
                handleIntent(pending)
            }
        }

        private fun rejectPendingAction() {
            _uiState.update { it.copy(pendingAIAction = null) }
        }

        private fun handleNotifyRunningLate(intent: EventIntent.NotifyRunningLate) {
            logcat { "AI Agent: Notifying running late for event ${intent.eventId}: ${intent.message}" }
            // In a real implementation, this would trigger a message sending service.
            // For now, we show a side effect to inform the user.
            _sideEffect.trySend(
                EventSideEffect.ShowSnackbar(
                    UiText.StringResource(R.string.ai_suggested_late_notification, listOf(intent.message)),
                ),
            )
        }

        private fun handleToggleFocusMode(intent: EventIntent.ToggleFocusMode) {
            toggleFocusModeUseCase(intent.enabled).onSuccess {
                val msgRes = R.string.ai_agent_snackbar_executed
                _sideEffect.trySend(
                    EventSideEffect.ShowSnackbar(
                        UiText.StringResource(
                            msgRes,
                            listOf("DND " + (if (intent.enabled) "enabled" else "disabled")),
                        ),
                    ),
                )
            }.onFailure {
                _sideEffect.trySend(
                    EventSideEffect.AIToolError(
                        UiText.DynamicString(
                            "Permission for DND access required. Please enable it in system settings.",
                        ),
                    ),
                )
            }
        }

        private fun handleSummarizeMeetTranscript(intent: EventIntent.SummarizeMeetTranscript) {
            viewModelScope.launch {
                val result = fetchMeetingTranscriptUseCase(intent.meetingUrl)
                result.onSuccess { transcript ->
                    var convId = _uiState.value.selectedConversationId
                    if (convId == null) {
                        convId = createConversationUseCase("Resumo Reunião")
                        _uiState.update { it.copy(selectedConversationId = convId) }
                        observeChatHistory(convId)
                    }
                    val questionMsg =
                        ChatMessage.Text(
                            ChatMessage.Role.USER,
                            "Aqui está a transcrição da reunião: \n$transcript\n\n" +
                                "Por favor, resuma os principais pontos discutidos e extraia as ações (action items).",
                        )
                    insertChatMessageUseCase(convId, questionMsg)
                    askAi(null)
                }.onFailure { e ->
                    _sideEffect.trySend(
                        EventSideEffect.AIToolError(
                            UiText.DynamicString(
                                "Falha ao baixar transcrição: ${e.message}",
                            ),
                        ),
                    )
                }
            }
        }

        /**
         * Builds a localized confirmation message for a CRITICAL action.
         */
        private fun formatConfirmationMessage(
            tool: AITool,
            intent: EventIntent,
        ): UiText =
            when (intent) {
                is EventIntent.CreateEvent ->
                    if (intent.location != null) {
                        UiText.StringResource(
                            R.string.ai_agent_create_event_with_location_confirmation,
                            listOf(intent.title, intent.location),
                        )
                    } else {
                        UiText.StringResource(
                            R.string.ai_agent_create_event_confirmation,
                            listOf(intent.title),
                        )
                    }

                else ->
                    UiText.StringResource(
                        R.string.ai_agent_generic_confirmation,
                        listOf(tool.name),
                    )
            }

        private fun handleCreateFocusBlock(intent: EventIntent.CreateFocusBlock) {
            viewModelScope.launch {
                val defaultCalendar = _uiState.value.availableCalendars.firstOrNull() ?: return@launch
                createEventUseCase(
                    calendarId = defaultCalendar.id,
                    title = intent.title,
                    description = "Focus time recommended by AI",
                    location = null,
                    startTime = intent.startTime,
                    endTime = intent.endTime,
                    isAllDay = false,
                    requestMeetLink = false,
                )
                refreshEvents()
            }
        }

        private fun handleAnalyzeSchedule(intent: EventIntent.AnalyzeSchedule) {
            _sideEffect.trySend(EventSideEffect.ShowSnackbar(UiText.DynamicString("Analysing schedule...")))
        }
    }
