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
import digital.tonima.core.analytics.EventAnalytics
import digital.tonima.core.database.mapper.toChatMessage
import digital.tonima.core.database.mapper.toEntity
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AuthRepository
import digital.tonima.core.repository.GoogleMeetRepository
import digital.tonima.core.review.ReviewManager
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.core.usecases.AskAiAgentUseCase
import digital.tonima.core.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.ObserveDailyBriefingUseCase
import digital.tonima.core.usecases.ObserveRingerModeUseCase
import digital.tonima.core.usecases.ToggleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventVibrateUseCase
import digital.tonima.core.usecases.ToggleFocusModeUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.util.toOpenWeatherLang
import digital.tonima.core.utils.TextToSpeechHelper
import digital.tonima.core.utils.WidgetUpdater
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
        private val calendar: CalendarDeps,
        private val prefs: PreferencesDeps,
        private val alarm: AlarmDeps,
        private val ai: AiDeps,
        private val eventAnalytics: EventAnalytics,
        private val observeRingerModeUseCase: ObserveRingerModeUseCase,
        private val checkPermissionsUseCase: CheckPermissionsUseCase,
        private val toggleFocusModeUseCase: ToggleFocusModeUseCase,
        private val reviewManager: ReviewManager,
        private val weather: WeatherDeps,
        private val googleMeetRepository: GoogleMeetRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel(), ProUserProvider by proUserProvider {
        /** Dependencies for calendar and event data operations. */
        class CalendarDeps
            @Inject
            constructor(
                val getEventsForMonth: GetEventsForMonthUseCase,
                val getAvailableCalendars: GetAvailableCalendarsUseCase,
                val createEvent: CreateEventUseCase,
            )

        /** Dependencies for reading and writing app preferences. */
        class PreferencesDeps
            @Inject
            constructor(
                val observe: ObserveAppPreferencesUseCase,
                val update: UpdateAppPreferenceUseCase,
            )

        /** Dependencies for alarm scheduling and toggling. */
        class AlarmDeps
            @Inject
            constructor(
                val toggleEventAlarm: ToggleEventAlarmUseCase,
                val toggleEventVibrate: ToggleEventVibrateUseCase,
                val scheduler: EventAlarmScheduler,
            )

        /** Dependencies for AI-powered features (briefing, suggestions, TTS, widgets, agent). */
        class AiDeps
            @Inject
            constructor(
                val generateDailyBriefing: GenerateDailyBriefingUseCase,
                val askAiAgent: AskAiAgentUseCase,
                val calculateDepartureTime: CalculateDepartureTimeUseCase,
                val observeDailyBriefing: ObserveDailyBriefingUseCase,
                val tts: TextToSpeechHelper,
                val widgetUpdater: WidgetUpdater,
                val actionRegistry: ActionRegistry,
                val chatHistoryDao: digital.tonima.core.database.dao.ChatHistoryDao,
            )

        /** Dependencies for weather operations. */
        class WeatherDeps
            @Inject
            constructor(
                val locationRepository: digital.tonima.core.repository.LocationRepository,
                val weatherRepository: digital.tonima.core.repository.WeatherRepository,
            )

        private val _uiState = MutableStateFlow(EventScreenUiState())
        val uiState = _uiState.asStateFlow()

        private val _sideEffect = Channel<EventSideEffect>(Channel.BUFFERED)
        val sideEffect = _sideEffect.receiveAsFlow()

        init {
            observePreferences()
            observeRingerMode()
            observeDailyBriefing()
            observeChatHistory()
            handleIntent(EventIntent.CheckPermissions)

            _uiState.update { it.copy(isGoogleConnected = authRepository.isSignedIn()) }

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
            eventAnalytics.logIntent(intent)
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
                        prefs.update.setEnabledCalendarIds(emptySet())
                    }
                is EventIntent.CreateEvent -> createEvent(intent)
                else -> Unit
            }
        }

        private fun handleSettingsIntent(intent: EventIntent) {
            viewModelScope.launch {
                when (intent) {
                    is EventIntent.ToggleGlobalAlarms -> prefs.update.setGlobalAlarmEnabled(intent.enabled)
                    is EventIntent.ToggleVibrateOnly -> prefs.update.setVibrateOnly(intent.enabled)
                    is EventIntent.ToggleAllDayAlarms -> prefs.update.setAllDayAlarmsEnabled(intent.enabled)
                    is EventIntent.UpdateAllDayAlarmHour -> prefs.update.setAllDayAlarmHour(intent.hour)
                    is EventIntent.UpdateAlarmOffset -> prefs.update.setAlarmOffsetMinutes(intent.offset.minutes)
                    is EventIntent.UpdateSnoozeTime -> prefs.update.setSnoozeTimeMinutes(intent.minutes)
                    is EventIntent.ToggleSkipWeekends -> prefs.update.setSkipWeekendsEnabled(intent.enabled)
                    is EventIntent.UpdateAutoDismissMinutes -> prefs.update.setAutoDismissMinutes(intent.minutes)
                    is EventIntent.ToggleLocationAlarm -> onLocationAlarmToggle(intent.enabled)
                    is EventIntent.ToggleAutoJoin -> prefs.update.setAutoJoinEnabled(intent.enabled)
                    is EventIntent.ToggleAutoFocusMode -> prefs.update.setAutoFocusModeEnabled(intent.enabled)
                    is EventIntent.ChangeTransportMode -> prefs.update.setPreferredTransportMode(intent.mode)
                    is EventIntent.ToggleTemperatureUnit -> {
                        prefs.update.setTemperatureInCelsius(intent.isCelsius)
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
                    alarm.toggleEventAlarm(intent.event, intent.enabled, intent.allOccurrences)
                is EventIntent.ToggleEventVibrate ->
                    alarm.toggleEventVibrate(intent.event, intent.enabled)
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
                else -> Unit
            }
        }

        private fun handleGoogleSignInResult(intent: android.content.Intent?) {
            viewModelScope.launch {
                val result = authRepository.handleSignInResult(intent)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isGoogleConnected = true) }
                    _sideEffect.trySend(
                        EventSideEffect.ShowSnackbar(
                            digital.tonima.core.viewmodel.UiText.StringResource(
                                digital.tonima.kairos.core.R.string.google_logout_title,
                            ),
                        ),
                    )
                } else {
                    _sideEffect.trySend(
                        EventSideEffect.ShowSnackbar(
                            digital.tonima.core.viewmodel.UiText.DynamicString("Login failed"),
                        ),
                    )
                }
            }
        }

        private fun handleSignInWithGoogle() {
            val signInIntent = authRepository.getSignInIntent()
            _sideEffect.trySend(EventSideEffect.LaunchGoogleSignIn(signInIntent))
        }

        private fun handleSignOutFromGoogle() {
            viewModelScope.launch {
                authRepository.signOut()
                _uiState.update { it.copy(isGoogleConnected = false) }
            }
        }

        private fun handleAiIntent(intent: EventIntent) {
            when (intent) {
                is EventIntent.AskAi -> askAi(intent.question, intent.language)
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
                        prefs.update.setExactAlarmPermissionSkipped(true)
                        checkPermissions()
                    }
                }
                EventIntent.SkipFullScreenIntentPermission -> {
                    logcat { "User skipped full-screen intent permission request" }
                    viewModelScope.launch {
                        prefs.update.setFullScreenIntentPermissionSkipped(true)
                        checkPermissions()
                    }
                }
                else -> Unit
            }
        }

        private suspend fun handleUiIntent(intent: EventIntent) {
            when (intent) {
                EventIntent.DismissAutostartSuggestion ->
                    prefs.update.setAutostartSuggestionDismissed(true)
                EventIntent.UpgradeToProRequest ->
                    _uiState.update { it.copy(showPurchaseConfirmation = true) }
                EventIntent.UpgradeToProIARequest ->
                    _uiState.update { it.copy(showSubscriptionConfirmation = true) }
                EventIntent.DismissUpgradeConfirmation ->
                    _uiState.update {
                        it.copy(showSubscriptionConfirmation = false, showPurchaseConfirmation = false)
                    }
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
                    prefs.update.setRatingCompleted(true)
                    _uiState.update { it.copy(showRatingBottomSheet = false) }
                }
                else -> Unit
            }
        }

        private fun observePreferences() {
            prefs.observe().onEach { appPrefs ->
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
                    _uiState.value.events.forEach { alarm.scheduler.cancel(it) }
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
            ai.observeDailyBriefing().onEach { briefing ->
                _uiState.update { it.copy(dailyBriefing = briefing) }
            }.launchIn(viewModelScope)
        }

        private fun observeChatHistory() {
            ai.chatHistoryDao.observeHistory().onEach { entities ->
                val messages = entities.mapNotNull { it.toChatMessage() }
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
                val calendarEvents = calendar.getEventsForMonth(_uiState.value.currentMonth)
                val appPrefs = prefs.observe().first()

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
                            val departureInfo = ai.calculateDepartureTime(event)
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
                            ai.calculateDepartureTime(event)?.departureTime
                        } else {
                            null
                        }
                    val alarmFireTime =
                        triggerTime ?: (event.startTime - TimeUnit.MINUTES.toMillis(offsetMinutes))
                    if (alarmFireTime in (now + 1)..windowEnd) {
                        alarm.scheduler.schedule(event, triggerTime)
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
            viewModelScope.launch { prefs.update.setLocationAlarmEnabled(enabled) }
        }

        private fun loadAvailableCalendars() {
            viewModelScope.launch {
                val calendars = calendar.getAvailableCalendars()
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
            prefs.update.setEnabledCalendarIds(newStringSet)

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
                val briefing = ai.generateDailyBriefing(eventsToday, language)
                if (briefing != null) {
                    ai.widgetUpdater.updateDailyBriefingWidget()
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
                val currentHistory = ai.chatHistoryDao.getHistory().mapNotNull { it.toChatMessage() }

                if (!question.isNullOrBlank()) {
                    val questionMsg = ChatMessage.Text(ChatMessage.Role.USER, question)
                    ai.chatHistoryDao.insertMessage(questionMsg.toEntity())
                }

                _uiState.update {
                    it.copy(
                        isAskingAi = true,
                        aiResponse = null,
                        lastAiQuestion = question ?: it.lastAiQuestion,
                    )
                }
                val eventsRecent = calendar.getEventsForMonth(_uiState.value.currentMonth)

                val agentResponse =
                    ai.askAiAgent(
                        eventsRecent,
                        question,
                        language,
                        ai.actionRegistry.registeredTools(),
                        currentHistory,
                    )

                when (agentResponse) {
                    is AIAgentResponse.Text -> {
                        val answerMsg = ChatMessage.Text(ChatMessage.Role.ASSISTANT, agentResponse.content)
                        ai.chatHistoryDao.insertMessage(answerMsg.toEntity())
                        processAiResponse(agentResponse.content)
                    }
                    is AIAgentResponse.FunctionCall -> {
                        val callMsg = ChatMessage.FunctionCall(agentResponse.name, agentResponse.args)
                        ai.chatHistoryDao.insertMessage(callMsg.toEntity())
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
            ai.tts.speak(text) { _uiState.update { it.copy(isSpeaking = false) } }
        }

        private fun speakAiResponse() {
            _uiState.value.aiResponse?.let {
                speak(it)
            }
        }

        private fun stopSpeaking() {
            ai.tts.stop()
            _uiState.update { it.copy(isSpeaking = false) }
        }

        private fun clearAiResponse() {
            stopSpeaking()
            viewModelScope.launch {
                ai.chatHistoryDao.clearHistory()
            }
            _uiState.update {
                it.copy(
                    aiResponse = null,
                    lastAiQuestion = null,
                    chatHistory = emptyList(),
                )
            }
        }

        private fun createEvent(intent: EventIntent.CreateEvent) {
            logcat { "Criando evento: ${intent.title} no calendário ${intent.calendarId}" }
            viewModelScope.launch {
                val result =
                    calendar.createEvent(
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
                    eventAnalytics.logEventCreated()
                    handleIntent(EventIntent.DismissCreateEventDialog)

                    // Navigate to the event date so the user can see it
                    val eventDate =
                        java.time.Instant.ofEpochMilli(intent.startTime)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    val eventMonth = java.time.YearMonth.from(eventDate)

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
                    logcat { "Falha ao criar evento: calendar.createEvent retornou null" }
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
                prefs.update.setRatingCompleted(true)
                _uiState.update { it.copy(showRatingBottomSheet = false) }
                activity?.let { reviewManager.requestReview(it) {} }
            }
        }

        private fun fetchWeather() {
            viewModelScope.launch {
                val isCelsius = _uiState.value.isTemperatureInCelsius
                val lang = java.util.Locale.getDefault().toOpenWeatherLang()
                val locationStr = weather.locationRepository.getCurrentLocation()
                if (locationStr != null) {
                    val parts = locationStr.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lon = parts[1].toDoubleOrNull()
                        if (lat != null && lon != null) {
                            val weatherData = weather.weatherRepository.getWeather(lat, lon, isCelsius, lang)
                            if (weatherData != null) {
                                _uiState.update { it.copy(weather = weatherData) }
                            }
                        }
                    }
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
                when (val result = ai.actionRegistry.processAIToolCall(toolName, args)) {
                    is AIToolResult.Success -> {
                        routeByRiskLevel(result)
                        val responseMsg =
                            ChatMessage
                                .FunctionResponse(
                                    toolName,
                                    mapOf("status" to "success", "message" to "Intent gerado e processado"),
                                )
                        ai.chatHistoryDao.insertMessage(responseMsg.toEntity())
                        askAi(null)
                    }
                    is AIToolResult.ToolNotFound -> {
                        logcat { "AI Agent: tool '${result.toolName}' not found" }
                        val responseMsg = ChatMessage.FunctionResponse(toolName, mapOf("error" to "Tool not found"))
                        ai.chatHistoryDao.insertMessage(responseMsg.toEntity())
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
                        ai.chatHistoryDao.insertMessage(responseMsg.toEntity())
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
                val result = googleMeetRepository.fetchMeetingTranscript(intent.meetingUrl)
                result.onSuccess { transcript ->
                    val questionMsg =
                        digital.tonima.core.ai.model.ChatMessage.Text(
                            digital.tonima.core.ai.model.ChatMessage.Role.USER,
                            "Aqui está a transcrição da reunião: \n$transcript\n\n" +
                                "Por favor, resuma os principais pontos discutidos e extraia as ações (action items).",
                        )
                    ai.chatHistoryDao.insertMessage(questionMsg.toEntity())
                    askAi(null)
                }.onFailure { e ->
                    _sideEffect.trySend(
                        EventSideEffect.AIToolError(
                            digital.tonima.core.viewmodel.UiText.DynamicString(
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
    }
