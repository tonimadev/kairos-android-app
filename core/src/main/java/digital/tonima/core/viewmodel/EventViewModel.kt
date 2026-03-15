package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.Event
import digital.tonima.core.review.ReviewManager
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.core.usecases.AskAiAboutScheduleUseCase
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
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.utils.TextToSpeechHelper
import digital.tonima.core.utils.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    constructor(
        proUserProvider: ProUserProvider,
        private val calendar: CalendarDeps,
        private val prefs: PreferencesDeps,
        private val alarm: AlarmDeps,
        private val ai: AiDeps,
        private val observeRingerModeUseCase: ObserveRingerModeUseCase,
        private val checkPermissionsUseCase: CheckPermissionsUseCase,
        private val reviewManager: ReviewManager,
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

        /** Dependencies for AI-powered features (briefing, suggestions, TTS, widgets). */
        class AiDeps
            @Inject
            constructor(
                val generateDailyBriefing: GenerateDailyBriefingUseCase,
                val askAiAboutSchedule: AskAiAboutScheduleUseCase,
                val calculateDepartureTime: CalculateDepartureTimeUseCase,
                val observeDailyBriefing: ObserveDailyBriefingUseCase,
                val tts: TextToSpeechHelper,
                val widgetUpdater: WidgetUpdater,
            )

        private val _uiState = MutableStateFlow(EventScreenUiState())
        val uiState = _uiState.asStateFlow()

        init {
            observePreferences()
            observeRingerMode()
            observeDailyBriefing()
            handleIntent(EventIntent.CheckPermissions)
        }

        fun handleIntent(intent: EventIntent) {
            viewModelScope.launch {
                when (intent) {
                    is EventIntent.RefreshEvents -> refreshEvents()
                    is EventIntent.ChangeMonth -> onMonthChanged(intent.yearMonth)
                    is EventIntent.SelectDate -> _uiState.update { it.copy(selectedDate = intent.date) }
                    EventIntent.ReturnToToday -> returnToToday()
                    is EventIntent.ToggleGlobalAlarms -> prefs.update.setGlobalAlarmEnabled(intent.enabled)
                    is EventIntent.ToggleVibrateOnly -> prefs.update.setVibrateOnly(intent.enabled)
                    is EventIntent.ToggleEventAlarm ->
                        alarm.toggleEventAlarm(intent.event, intent.enabled, intent.allOccurrences)
                    is EventIntent.ToggleEventVibrate ->
                        alarm.toggleEventVibrate(intent.event, intent.enabled)
                    is EventIntent.UpdateAlarmOffset ->
                        prefs.update.setAlarmOffsetMinutes(intent.offset.minutes)
                    is EventIntent.UpdateSnoozeTime -> prefs.update.setSnoozeTimeMinutes(intent.minutes)
                    is EventIntent.ToggleLocationAlarm -> onLocationAlarmToggle(intent.enabled)
                    is EventIntent.ChangeTransportMode ->
                        prefs.update.setPreferredTransportMode(intent.mode)
                    is EventIntent.CreateEvent -> createEvent(intent)
                    EventIntent.LoadCalendars -> loadAvailableCalendars()
                    is EventIntent.ToggleCalendarFilter ->
                        onCalendarFilterToggle(intent.calendarId, intent.enabled)
                    EventIntent.ClearCalendarFilter -> prefs.update.setEnabledCalendarIds(emptySet())

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

                    // Grouped: UI / dialog / rating intents
                    EventIntent.DismissAutostartSuggestion,
                    EventIntent.UpgradeToProRequest,
                    EventIntent.DismissUpgradeConfirmation,
                    is EventIntent.SearchQueryChanged,
                    is EventIntent.ShowCreateEventDialog,
                    EventIntent.DismissCreateEventDialog,
                    EventIntent.ShowAiSuggestionsDialog,
                    EventIntent.DismissAiSuggestionsDialog,
                    is EventIntent.RateNow,
                    EventIntent.RateLater,
                    EventIntent.RateNever,
                    -> handleUiIntent(intent)
                }
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
                    _uiState.update { it.copy(hasExactAlarmPermission = true) }
                    checkPermissions()
                }
                EventIntent.SkipFullScreenIntentPermission -> {
                    logcat { "User skipped full-screen intent permission request" }
                    _uiState.update { it.copy(hasFullScreenIntentPermission = true) }
                    checkPermissions()
                }
                else -> Unit
            }
        }

        private suspend fun handleUiIntent(intent: EventIntent) {
            when (intent) {
                EventIntent.DismissAutostartSuggestion ->
                    prefs.update.setAutostartSuggestionDismissed(true)
                EventIntent.UpgradeToProRequest ->
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
                EventIntent.RateLater -> _uiState.update { it.copy(showRatingBottomSheet = false) }
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
                        showAutostartSuggestion = !appPrefs.autostartSuggestionDismissed,
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

        private fun checkPermissions() {
            val p = checkPermissionsUseCase()
            _uiState.update {
                it.copy(
                    hasCalendarPermission = p.hasCalendarPermission,
                    hasPostNotificationsPermission = p.hasPostNotificationsPermission,
                    hasExactAlarmPermission = p.hasExactAlarmPermission,
                    hasFullScreenIntentPermission = p.hasFullScreenIntentPermission,
                    hasLocationPermission = p.hasLocationPermission,
                    hasBackgroundLocationPermission = p.hasBackgroundLocationPermission,
                )
            }
            if (p.hasCalendarPermission) {
                refreshEvents()
                handleIntent(EventIntent.LoadCalendars)
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

                _uiState.update { it.copy(events = updatedEvents, isRefreshing = false) }

                if (_uiState.value.isGlobalAlarmEnabled) {
                    scheduleImmediateEvents(updatedEvents)
                }
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

        private fun onCalendarFilterToggle(
            calendarId: Long,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                val allIds = _uiState.value.availableCalendars.map { it.id.toString() }.toSet()
                val current = prefs.observe().first().enabledCalendarIds.toMutableSet()

                if (current.isEmpty()) current.addAll(allIds)
                if (enabled) current.add(calendarId.toString()) else current.remove(calendarId.toString())

                val newSet = if (current.containsAll(allIds)) emptySet() else current
                prefs.update.setEnabledCalendarIds(newSet)
            }
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
                if (briefing != null) ai.widgetUpdater.updateDailyBriefingWidget()
                _uiState.update { it.copy(isGeneratingBriefing = false) }
            }
        }

        private fun askAi(
            question: String,
            language: String,
        ) {
            if (question.isBlank() || _uiState.value.isAskingAi || !_uiState.value.isAiUser) return

            viewModelScope.launch {
                _uiState.update { it.copy(isAskingAi = true, aiResponse = null) }
                val eventsRecent = calendar.getEventsForMonth(_uiState.value.currentMonth)
                val response = ai.askAiAboutSchedule(eventsRecent, question, language)
                if (response != null) processAiResponse(response)
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
            _uiState.update { it.copy(aiResponse = response) }
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
            _uiState.value.aiResponse?.let { speak(it) }
        }

        private fun stopSpeaking() {
            ai.tts.stop()
            _uiState.update { it.copy(isSpeaking = false) }
        }

        private fun clearAiResponse() {
            stopSpeaking()
            _uiState.update { it.copy(aiResponse = null) }
        }

        private fun createEvent(intent: EventIntent.CreateEvent) {
            viewModelScope.launch {
                calendar.createEvent(
                    intent.calendarId,
                    intent.title,
                    intent.description,
                    intent.location,
                    intent.startTime,
                    intent.endTime,
                    intent.isAllDay,
                )
                handleIntent(EventIntent.DismissCreateEventDialog)
                refreshEvents()
            }
        }

        private fun onRateNow(activity: android.app.Activity?) {
            viewModelScope.launch {
                prefs.update.setRatingCompleted(true)
                _uiState.update { it.copy(showRatingBottomSheet = false) }
                activity?.let { reviewManager.requestReview(it) {} }
            }
        }
    }
