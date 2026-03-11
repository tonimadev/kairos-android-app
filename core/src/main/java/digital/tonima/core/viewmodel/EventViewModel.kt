package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.model.Event
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.repository.CalendarRepository
import digital.tonima.core.repository.DailyBriefingRepository
import digital.tonima.core.repository.RingerModeRepository
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.core.usecases.AskAiAboutScheduleUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.utils.TextToSpeechHelper
import digital.tonima.core.utils.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class VoiceEventData(
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isAllDay: Boolean = false,
)

data class EventScreenUiState(
    val events: List<Event> = emptyList(),
    val isGlobalAlarmEnabled: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val showAutostartSuggestion: Boolean = false,
    val showUpgradeConfirmation: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val hasPostNotificationsPermission: Boolean = false,
    val hasExactAlarmPermission: Boolean = false,
    val hasFullScreenIntentPermission: Boolean = false,
    val audioWarning: AudioWarningState = AudioWarningState.NORMAL,
    val vibrateOnly: Boolean = false,
    val showRatingDialog: Boolean = false,
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
    val showCreateEventDialog: Boolean = false,
    val voiceEventData: VoiceEventData? = null,
    val showAiSuggestionsDialog: Boolean = false,
)

@HiltViewModel
class EventViewModel
    @Inject
    @Suppress("LongParameterList")
    constructor(
        proUserProvider: ProUserProvider,
        private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
        private val appPreferencesRepository: AppPreferencesRepository,
        private val ringerModeRepository: RingerModeRepository,
        private val scheduler: EventAlarmScheduler,
        private val permissionManager: PermissionManager,
        private val calendarRepository: CalendarRepository,
        private val dailyBriefingRepository: DailyBriefingRepository,
        private val generateDailyBriefingUseCase: GenerateDailyBriefingUseCase,
        private val askAiAboutScheduleUseCase: AskAiAboutScheduleUseCase,
        private val createEventUseCase: CreateEventUseCase,
        private val ttsHelper: TextToSpeechHelper,
        private val widgetUpdater: WidgetUpdater,
    ) : ViewModel(), ProUserProvider by proUserProvider {
        private val _uiState = MutableStateFlow(EventScreenUiState())
        val uiState = _uiState.asStateFlow()

        init {
            appPreferencesRepository.isGlobalAlarmEnabled()
                .onEach { isEnabled ->
                    _uiState.update { it.copy(isGlobalAlarmEnabled = isEnabled) }
                    if (!isEnabled) {
                        cancelAllLoadedAlarms()
                    } else {
                        if (_uiState.value.hasCalendarPermission) {
                            onMonthChanged(_uiState.value.currentMonth, forceRefresh = true)
                        }
                    }
                }
                .launchIn(viewModelScope)

            appPreferencesRepository.getAutostartSuggestionDismissed()
                .onEach { dismissed ->
                    _uiState.update { it.copy(showAutostartSuggestion = !dismissed) }
                }
                .launchIn(viewModelScope)

            appPreferencesRepository.getVibrateOnly()
                .onEach { vibrate -> _uiState.update { it.copy(vibrateOnly = vibrate) } }
                .launchIn(viewModelScope)

            appPreferencesRepository.isAllDayAlarmsEnabled()
                .onEach { enabled ->
                    _uiState.update { it.copy(allDayAlarmsEnabled = enabled) }
                    if (_uiState.value.hasCalendarPermission && _uiState.value.isGlobalAlarmEnabled) {
                        onMonthChanged(_uiState.value.currentMonth, forceRefresh = true)
                    }
                }
                .launchIn(viewModelScope)

            appPreferencesRepository.getAllDayAlarmHour()
                .onEach { hour ->
                    _uiState.update { it.copy(allDayAlarmHour = hour) }
                    if (_uiState.value.hasCalendarPermission &&
                        _uiState.value.isGlobalAlarmEnabled &&
                        _uiState.value.allDayAlarmsEnabled
                    ) {
                        onMonthChanged(_uiState.value.currentMonth, forceRefresh = true)
                    }
                }
                .launchIn(viewModelScope)

            appPreferencesRepository.getAlarmOffsetMinutes()
                .onEach { minutes ->
                    _uiState.update { it.copy(alarmOffsetMinutes = minutes) }
                    if (_uiState.value.hasCalendarPermission && _uiState.value.isGlobalAlarmEnabled) {
                        onMonthChanged(_uiState.value.currentMonth, forceRefresh = true)
                    }
                }
                .launchIn(viewModelScope)

            appPreferencesRepository.getEnabledCalendarIds()
                .onEach { idStrings ->
                    val ids = idStrings.mapNotNull { it.toLongOrNull() }.toSet()
                    _uiState.update { it.copy(enabledCalendarIds = ids) }
                    if (_uiState.value.hasCalendarPermission) {
                        onMonthChanged(_uiState.value.currentMonth, forceRefresh = true)
                    }
                }
                .launchIn(viewModelScope)

            appPreferencesRepository.getSnoozeTimeMinutes()
                .onEach { minutes -> _uiState.update { it.copy(snoozeTimeMinutes = minutes) } }
                .launchIn(viewModelScope)

            dailyBriefingRepository.getDailyBriefing()
                .onEach { briefing -> _uiState.update { it.copy(dailyBriefing = briefing) } }
                .launchIn(viewModelScope)

            isProUser
                .onEach { pro -> _uiState.update { it.copy(isProUser = pro) } }
                .launchIn(viewModelScope)

            isAiUser
                .onEach { ai -> _uiState.update { it.copy(isAiUser = ai) } }
                .launchIn(viewModelScope)

            checkAllPermissions()

            ringerModeRepository.startObserving()
            ringerModeRepository.ringerMode
                .onEach { warning -> _uiState.update { it.copy(audioWarning = warning) } }
                .launchIn(viewModelScope)

            viewModelScope.launch {
                val installationDate = appPreferencesRepository.getInstallationDate().firstOrNull() ?: 0L
                val hasPrompted = appPreferencesRepository.isRatingPrompted().firstOrNull() ?: false
                val hasCompleted = appPreferencesRepository.isRatingCompleted().firstOrNull() ?: false
                val currentTime = System.currentTimeMillis()
                val twoDaysInMillis = TimeUnit.DAYS.toMillis(2)
                if (installationDate != 0L &&
                    currentTime - installationDate >= twoDaysInMillis &&
                    !hasPrompted &&
                    !hasCompleted
                ) {
                    _uiState.update { it.copy(showRatingDialog = true) }
                    appPreferencesRepository.setRatingPrompted(true)
                }
            }
        }

        public override fun onCleared() {
            super.onCleared()
            ringerModeRepository.stopObserving()
            ttsHelper.shutdown()
        }

        fun checkAllPermissions() {
            _uiState.update { currentState ->
                currentState.copy(
                    hasCalendarPermission = permissionManager.hasCalendarPermission(),
                    hasPostNotificationsPermission = permissionManager.hasPostNotificationsPermission(),
                    hasExactAlarmPermission = permissionManager.hasExactAlarmPermission(),
                    hasFullScreenIntentPermission = permissionManager.hasFullScreenIntentPermission(),
                )
            }
            if (_uiState.value.hasCalendarPermission) {
                onMonthChanged(_uiState.value.currentMonth, forceRefresh = true)
                loadAvailableCalendars()
            }
        }

        fun dismissAutostartSuggestion() {
            viewModelScope.launch {
                appPreferencesRepository.setAutostartSuggestionDismissed(true)
            }
        }

        fun skipExactAlarmPermission() {
            logcat { "User skipped exact alarm permission request - alarms will be inexact" }
            _uiState.update { it.copy(hasExactAlarmPermission = true) }
        }

        fun skipFullScreenIntentPermission() {
            logcat { "User skipped full-screen intent permission request" }
            _uiState.update { it.copy(hasFullScreenIntentPermission = true) }
        }

        fun onMonthChanged(
            yearMonth: YearMonth,
            forceRefresh: Boolean = false,
        ) {
            if (!_uiState.value.hasCalendarPermission) {
                logcat(LogPriority.WARN) {
                    "Permissão de calendário não concedida. Não é possível mudar o mês ou carregar eventos."
                }
                _uiState.update { it.copy(events = emptyList()) }
                return
            }
            if (!forceRefresh && yearMonth == _uiState.value.currentMonth) return

            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true, currentMonth = yearMonth) }

                val calendarEvents = getEventsForMonthUseCase.invoke(yearMonth)
                val disabledInstanceIds = appPreferencesRepository.getDisabledEventIds().firstOrNull() ?: emptySet()
                val disabledSeriesIds = appPreferencesRepository.getDisabledSeriesIds().firstOrNull() ?: emptySet()
                val vibrateOnlyEventIds = appPreferencesRepository.getVibrateOnlyEventIds().firstOrNull() ?: emptySet()

                val updatedEvents =
                    calendarEvents.map { event ->
                        val isInstanceDisabled = disabledInstanceIds.contains(event.uniqueIntentId.toString())
                        val isSeriesDisabled = disabledSeriesIds.contains(event.id.toString())
                        val isVibrateOnly = vibrateOnlyEventIds.contains(event.uniqueIntentId.toString())
                        event.copy(
                            isAlarmEnabled = !(isInstanceDisabled || isSeriesDisabled),
                            vibrateOnly = isVibrateOnly,
                        )
                    }

                _uiState.update { currentState ->
                    currentState.copy(
                        events = updatedEvents,
                        isRefreshing = false,
                    )
                }

                if (appPreferencesRepository.isGlobalAlarmEnabled().firstOrNull() == true) {
                    scheduleImmediateEvents(updatedEvents)
                }
            }
        }

        private fun scheduleImmediateEvents(events: List<Event>) {
            val now = System.currentTimeMillis()
            val offsetMinutes = _uiState.value.alarmOffsetMinutes
            val scheduleWindowEnd = now + TimeUnit.MINUTES.toMillis(75) + TimeUnit.MINUTES.toMillis(offsetMinutes)

            events
                .filter { it.isAlarmEnabled }
                .filter { event ->
                    val alarmFireTime = event.startTime - TimeUnit.MINUTES.toMillis(offsetMinutes)
                    alarmFireTime in (now + 1)..scheduleWindowEnd
                }
                .forEach { event ->
                    scheduler.schedule(event)
                }
        }

        fun onDateSelected(date: LocalDate) {
            _uiState.update { it.copy(selectedDate = date) }
        }

        fun returnToToday() {
            _uiState.update {
                it.copy(
                    selectedDate = LocalDate.now(),
                    currentMonth = YearMonth.now(),
                )
            }
        }

        fun onAlarmsToggle(isEnabled: Boolean) {
            viewModelScope.launch {
                appPreferencesRepository.setGlobalAlarmEnabled(isEnabled)
            }
        }

        fun onVibrateOnlyChanged(enabled: Boolean) {
            viewModelScope.launch {
                appPreferencesRepository.setVibrateOnly(enabled)
            }
        }

        fun onEventVibrateToggle(
            event: Event,
            vibrateOnly: Boolean,
        ) {
            viewModelScope.launch {
                val currentVibrateOnlyIds =
                    appPreferencesRepository.getVibrateOnlyEventIds().firstOrNull()?.toMutableSet()
                        ?: mutableSetOf()
                val eventIdStr = event.uniqueIntentId.toString()

                if (vibrateOnly) {
                    currentVibrateOnlyIds.add(eventIdStr)
                } else {
                    currentVibrateOnlyIds.remove(eventIdStr)
                }
                appPreferencesRepository.setVibrateOnlyEventIds(currentVibrateOnlyIds)

                _uiState.update { currentState ->
                    val updatedEvents =
                        currentState.events.map {
                            if (it.uniqueIntentId == event.uniqueIntentId) {
                                it.copy(vibrateOnly = vibrateOnly)
                            } else {
                                it
                            }
                        }
                    currentState.copy(events = updatedEvents)
                }
            }
        }

        fun onEventAlarmToggle(
            event: Event,
            isEnabled: Boolean,
            disableAllOccurrences: Boolean = false,
        ) {
            viewModelScope.launch {
                val currentDisabledInstanceIds =
                    appPreferencesRepository.getDisabledEventIds().firstOrNull()?.toMutableSet() ?: mutableSetOf()
                val currentDisabledSeriesIds =
                    appPreferencesRepository.getDisabledSeriesIds().firstOrNull()?.toMutableSet() ?: mutableSetOf()
                val instanceIdStr = event.uniqueIntentId.toString()
                val seriesIdStr = event.id.toString()

                if (isEnabled) {
                    if (disableAllOccurrences) {
                        currentDisabledSeriesIds.remove(seriesIdStr)
                        appPreferencesRepository.setDisabledSeriesIds(currentDisabledSeriesIds)
                    } else {
                        currentDisabledInstanceIds.remove(instanceIdStr)
                        appPreferencesRepository.setDisabledEventIds(currentDisabledInstanceIds)
                    }
                } else {
                    if (disableAllOccurrences) {
                        currentDisabledSeriesIds.add(seriesIdStr)
                        appPreferencesRepository.setDisabledSeriesIds(currentDisabledSeriesIds)
                    } else {
                        currentDisabledInstanceIds.add(instanceIdStr)
                        appPreferencesRepository.setDisabledEventIds(currentDisabledInstanceIds)
                    }
                }

                _uiState.update { currentState ->
                    val updatedEvents =
                        currentState.events.map {
                            if (it.uniqueIntentId == event.uniqueIntentId) {
                                it.copy(isAlarmEnabled = isEnabled)
                            } else {
                                it
                            }
                        }
                    currentState.copy(events = updatedEvents)
                }

                if (appPreferencesRepository.isGlobalAlarmEnabled().firstOrNull() == true) {
                    if (isEnabled) {
                        scheduler.schedule(event)
                    } else {
                        scheduler.cancel(event)
                    }
                }
            }
        }

        private fun cancelAllLoadedAlarms() {
            viewModelScope.launch {
                _uiState.value.events.forEach { event ->
                    scheduler.cancel(event)
                }
            }
        }

        fun onUpgradeToProRequest() {
            _uiState.update { it.copy(showUpgradeConfirmation = true) }
        }

        fun onDismissUpgradeConfirmation() {
            _uiState.update { it.copy(showUpgradeConfirmation = false) }
        }

        fun onRatingDialogDismiss() {
            _uiState.update { it.copy(showRatingDialog = false) }
        }

        fun onRateNow() {
            viewModelScope.launch {
                appPreferencesRepository.setRatingCompleted(true)
                _uiState.update { it.copy(showRatingDialog = false) }
            }
        }

        fun onRateLater() {
            _uiState.update { it.copy(showRatingDialog = false) }
        }

        fun onRateNeverShow() {
            viewModelScope.launch {
                appPreferencesRepository.setRatingCompleted(true)
                _uiState.update { it.copy(showRatingDialog = false) }
            }
        }

        fun onAllDayAlarmsToggle(enabled: Boolean) {
            viewModelScope.launch {
                appPreferencesRepository.setAllDayAlarmsEnabled(enabled)
            }
        }

        fun onAllDayAlarmHourChanged(hour: Int) {
            viewModelScope.launch {
                appPreferencesRepository.setAllDayAlarmHour(hour)
            }
        }

        fun onAlarmOffsetChanged(offset: AlarmOffset) {
            viewModelScope.launch {
                appPreferencesRepository.setAlarmOffsetMinutes(offset.minutes)
            }
        }

        fun loadAvailableCalendars() {
            viewModelScope.launch {
                val calendars = calendarRepository.getAvailableCalendars()
                _uiState.update { it.copy(availableCalendars = calendars) }
            }
        }

        fun onCalendarFilterToggle(
            calendarId: Long,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                val allIds = _uiState.value.availableCalendars.map { it.id.toString() }.toSet()
                val current =
                    appPreferencesRepository.getEnabledCalendarIds()
                        .firstOrNull()
                        .let { saved ->
                            if (saved.isNullOrEmpty()) allIds.toMutableSet() else saved.toMutableSet()
                        }

                if (enabled) {
                    current.add(calendarId.toString())
                } else {
                    current.remove(calendarId.toString())
                }

                val newSet = if (current.containsAll(allIds)) emptySet() else current
                appPreferencesRepository.setEnabledCalendarIds(newSet)
            }
        }

        fun clearCalendarFilter() {
            viewModelScope.launch {
                appPreferencesRepository.setEnabledCalendarIds(emptySet())
            }
        }

        fun onSearchQueryChanged(query: String) {
            _uiState.update { it.copy(searchQuery = query) }
        }

        fun onSnoozeTimeChanged(minutes: Int) {
            viewModelScope.launch {
                appPreferencesRepository.setSnoozeTimeMinutes(minutes)
            }
        }

        fun generateDailyBriefing(languageInstruction: String) {
            val today = LocalDate.now()
            val uiValue = _uiState.value
            if (uiValue.selectedDate != today ||
                uiValue.dailyBriefing != null ||
                uiValue.isGeneratingBriefing ||
                !uiValue.isAiUser
            ) {
                return
            }

            val eventsToday =
                uiValue.events.filter { event ->
                    Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault()).toLocalDate() == today
                }

            viewModelScope.launch {
                _uiState.update { it.copy(isGeneratingBriefing = true) }
                val briefing = generateDailyBriefingUseCase.invoke(eventsToday, languageInstruction)
                if (briefing != null) {
                    dailyBriefingRepository.saveDailyBriefing(briefing)
                    widgetUpdater.updateDailyBriefingWidget()
                }
                _uiState.update { it.copy(dailyBriefing = briefing, isGeneratingBriefing = false) }
            }
        }

        fun askAi(
            question: String,
            languageInstruction: String,
        ) {
            if (question.isBlank() || _uiState.value.isAskingAi || !_uiState.value.isAiUser) return

            viewModelScope.launch {
                _uiState.update { it.copy(isAskingAi = true, lastAiQuestion = question, aiResponse = null) }

                val currentMonth = YearMonth.now()
                val eventsRecent =
                    getEventsForMonthUseCase.invoke(currentMonth.minusMonths(1)) +
                        getEventsForMonthUseCase.invoke(currentMonth) +
                        getEventsForMonthUseCase.invoke(currentMonth.plusMonths(1))

                val response =
                    askAiAboutScheduleUseCase.invoke(
                        events = eventsRecent,
                        question = question,
                        languageInstruction = languageInstruction,
                    )

                if (response != null) {
                    val trimmedResponse = response.trim()
                    val hasJsonStart = trimmedResponse.contains("\"title\":") && trimmedResponse.contains("{")

                    if (hasJsonStart) {
                        try {
                            val voiceEventData = parseVoiceEventData(trimmedResponse)
                            if (voiceEventData != null) {
                                _uiState.update {
                                    it.copy(
                                        isAskingAi = false,
                                        showCreateEventDialog = true,
                                        voiceEventData = voiceEventData,
                                    )
                                }
                            } else {
                                // Caso não consiga extrair dados válidos do JSON suspeito
                                _uiState.update { it.copy(aiResponse = response, isAskingAi = false) }
                                speak(response)
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) { "Error parsing voice event data: ${e.message}" }
                            _uiState.update { it.copy(aiResponse = response, isAskingAi = false) }
                            speak(response)
                        }
                    } else {
                        _uiState.update { it.copy(aiResponse = response, isAskingAi = false) }
                        speak(response)
                    }
                } else {
                    _uiState.update { it.copy(isAskingAi = false) }
                }
            }
        }

        private fun parseVoiceEventData(jsonStr: String): VoiceEventData? {
            val titleMatch = Regex("\"title\":\\s*\"([^\"]+)\"").find(jsonStr)
            val title = titleMatch?.groupValues?.get(1) ?: return null

            val description = Regex("\"description\":\\s*\"([^\"]+)\"").find(jsonStr)?.groupValues?.get(1)
            val location = Regex("\"location\":\\s*\"([^\"]+)\"").find(jsonStr)?.groupValues?.get(1)
            val startTime = Regex("\"startTime\":\\s*(\\d+)").find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
            val endTime = Regex("\"endTime\":\\s*(\\d+)").find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
            val isAllDay =
                Regex(
                    "\"isAllDay\":\\s*(true|false)",
                ).find(jsonStr)?.groupValues?.get(1)?.toBoolean() ?: false

            return VoiceEventData(
                title = title,
                description = description,
                location = location,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
            )
        }

        private fun speak(text: String) {
            _uiState.update { it.copy(isSpeaking = true) }
            ttsHelper.speak(text) {
                _uiState.update { it.copy(isSpeaking = false) }
            }
        }

        fun speakAiResponse() {
            _uiState.value.aiResponse?.let {
                speak(it)
            }
        }

        fun stopSpeaking() {
            ttsHelper.stop()
            _uiState.update { it.copy(isSpeaking = false) }
        }

        fun clearAiResponse() {
            stopSpeaking()
            _uiState.update { it.copy(aiResponse = null, lastAiQuestion = null) }
        }

        fun onCreateEventRequest(voiceEventData: VoiceEventData? = null) {
            _uiState.update { it.copy(showCreateEventDialog = true, voiceEventData = voiceEventData) }
        }

        fun onCreateEventDismiss() {
            _uiState.update { it.copy(showCreateEventDialog = false, voiceEventData = null) }
        }

        fun onStartVoiceCaptureRequest() {
            _uiState.update { it.copy(showAiSuggestionsDialog = true) }
        }

        fun onDismissAiSuggestions() {
            _uiState.update { it.copy(showAiSuggestionsDialog = false) }
        }

        fun createEvent(
            calendarId: Long,
            title: String,
            description: String?,
            location: String?,
            startTime: Long,
            endTime: Long,
            isAllDay: Boolean,
        ) {
            viewModelScope.launch {
                createEventUseCase.invoke(
                    calendarId = calendarId,
                    title = title,
                    description = description,
                    location = location,
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                )
                _uiState.update { it.copy(showCreateEventDialog = false) }
                onMonthChanged(_uiState.value.currentMonth, forceRefresh = true)
            }
        }
    }
