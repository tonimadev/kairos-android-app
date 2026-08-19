package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.model.Event
import digital.tonima.core.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.usecases.CancelEventAlarmUseCase
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.CreateEventUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.GetCurrentLocationUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.usecases.GetMeetingTimeStatsUseCase
import digital.tonima.core.usecases.GetWeatherUseCase
import digital.tonima.core.usecases.LogEventUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.ScheduleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventAlarmUseCase
import digital.tonima.core.usecases.ToggleEventVibrateUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.util.toOpenWeatherLang
import digital.tonima.core.viewmodel.EventIntent.ChangeBottomTab
import digital.tonima.core.viewmodel.EventIntent.ChangeInsightsPeriod
import digital.tonima.core.viewmodel.EventIntent.ChangeMonth
import digital.tonima.core.viewmodel.EventIntent.ClearCalendarFilter
import digital.tonima.core.viewmodel.EventIntent.CloseImportCalendarScreen
import digital.tonima.core.viewmodel.EventIntent.CloseManageCalendarsScreen
import digital.tonima.core.viewmodel.EventIntent.CopyMeetingUrl
import digital.tonima.core.viewmodel.EventIntent.CreateEvent
import digital.tonima.core.viewmodel.EventIntent.DismissCreateEventDialog
import digital.tonima.core.viewmodel.EventIntent.FetchWeather
import digital.tonima.core.viewmodel.EventIntent.JoinMeeting
import digital.tonima.core.viewmodel.EventIntent.LoadCalendars
import digital.tonima.core.viewmodel.EventIntent.OpenImportCalendarScreen
import digital.tonima.core.viewmodel.EventIntent.OpenManageCalendarsScreen
import digital.tonima.core.viewmodel.EventIntent.RateLater
import digital.tonima.core.viewmodel.EventIntent.RateNever
import digital.tonima.core.viewmodel.EventIntent.RateNow
import digital.tonima.core.viewmodel.EventIntent.RefreshEvents
import digital.tonima.core.viewmodel.EventIntent.ReturnToToday
import digital.tonima.core.viewmodel.EventIntent.SearchQueryChanged
import digital.tonima.core.viewmodel.EventIntent.SelectDate
import digital.tonima.core.viewmodel.EventIntent.ShowCreateEventDialog
import digital.tonima.core.viewmodel.EventIntent.ToggleCalendarFilter
import digital.tonima.core.viewmodel.EventIntent.ToggleEventAlarm
import digital.tonima.core.viewmodel.EventIntent.ToggleEventVibrate
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProIARequest
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProRequest
import digital.tonima.core.viewmodel.EventSideEffect.AIToolError
import digital.tonima.core.viewmodel.EventSideEffect.CopyToClipboard
import digital.tonima.core.viewmodel.EventSideEffect.OpenMeetingUrl
import digital.tonima.core.viewmodel.EventSideEffect.RequestPurchase
import digital.tonima.core.viewmodel.EventSideEffect.RequestSubscription
import digital.tonima.core.viewmodel.EventSideEffect.ShowSnackbar
import digital.tonima.core.viewmodel.UiText.StringResource
import digital.tonima.kairos.core.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
@Suppress("LongParameterList")
class EventViewModel
    @Inject
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
        private val calculateDepartureTimeUseCase: CalculateDepartureTimeUseCase,
        private val logEventUseCase: LogEventUseCase,
        private val checkPermissionsUseCase: CheckPermissionsUseCase,
        private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
        private val getWeatherUseCase: GetWeatherUseCase,
        private val getMeetingTimeStatsUseCase: GetMeetingTimeStatsUseCase,
    ) : ViewModel(), ProUserProvider by proUserProvider {
        private val _uiState = MutableStateFlow(EventScreenUiState())
        val uiState = _uiState.asStateFlow()

        private var isGlobalAlarmEnabled = true
        private var alarmOffsetMinutes = 0L

        fun onSideEffectConsumed(effect: EventSideEffect) {
            _uiState.update { it.copy(sideEffects = it.sideEffects - effect) }
        }

        init {
            observePreferences()
            handleIntent(LoadCalendars)

            viewModelScope.launch {
                val stats = getMeetingTimeStatsUseCase(_uiState.value.selectedInsightsPeriod)
                _uiState.update { it.copy(meetingStats = stats) }
            }

            viewModelScope.launch {
                isAiUser.collect { isAi -> _uiState.update { it.copy(isAiUser = isAi) } }
            }

            viewModelScope.launch {
                isProUser.collect { isPro -> _uiState.update { it.copy(isProUser = isPro) } }
            }
        }

        fun handleIntent(intent: EventIntent) {
            logEventUseCase(intent)
            viewModelScope.launch {
                when (intent) {
                    is RefreshEvents -> refreshEvents()
                    is ChangeMonth -> onMonthChanged(intent.yearMonth)
                    is SelectDate -> _uiState.update { it.copy(selectedDate = intent.date) }
                    ReturnToToday -> returnToToday()
                    LoadCalendars -> loadAvailableCalendars()
                    is ToggleCalendarFilter -> onCalendarFilterToggle(intent.calendarId, intent.enabled)
                    ClearCalendarFilter -> updateAppPreferenceUseCase.setEnabledCalendarIds(emptySet())
                    is CreateEvent -> createEvent(intent)
                    is JoinMeeting ->
                        _uiState.update {
                            it.copy(
                                sideEffects = it.sideEffects + OpenMeetingUrl(intent.meetingUrl),
                            )
                        }
                    is CopyMeetingUrl ->
                        _uiState.update {
                            it.copy(
                                sideEffects =
                                    it.sideEffects +
                                        CopyToClipboard(
                                            intent.meetingUrl,
                                            StringResource(R.string.link_copied),
                                        ),
                            )
                        }
                    is ToggleEventAlarm ->
                        toggleEventAlarmUseCase(
                            intent.event,
                            intent.enabled,
                            intent.allOccurrences,
                        )
                    is ToggleEventVibrate -> toggleEventVibrateUseCase(intent.event, intent.enabled)
                    FetchWeather -> fetchWeather()
                    UpgradeToProRequest ->
                        _uiState.update {
                            it.copy(sideEffects = it.sideEffects + RequestPurchase)
                        }
                    UpgradeToProIARequest ->
                        _uiState.update {
                            it.copy(sideEffects = it.sideEffects + RequestSubscription)
                        }
                    is ChangeBottomTab -> _uiState.update { it.copy(selectedBottomTab = intent.tabIndex) }
                    OpenImportCalendarScreen -> _uiState.update { it.copy(showImportCalendarScreen = true) }
                    CloseImportCalendarScreen -> _uiState.update { it.copy(showImportCalendarScreen = false) }
                    OpenManageCalendarsScreen -> _uiState.update { it.copy(showManageCalendarsScreen = true) }
                    CloseManageCalendarsScreen -> _uiState.update { it.copy(showManageCalendarsScreen = false) }
                    is ChangeInsightsPeriod -> {
                        _uiState.update { it.copy(selectedInsightsPeriod = intent.period) }
                        val stats = getMeetingTimeStatsUseCase(intent.period)
                        _uiState.update { it.copy(meetingStats = stats) }
                    }
                    is SearchQueryChanged -> _uiState.update { it.copy(searchQuery = intent.query) }
                    is ShowCreateEventDialog -> _uiState.update { it.copy(showCreateEventDialog = true) }
                    DismissCreateEventDialog -> _uiState.update { it.copy(showCreateEventDialog = false) }
                    is RateNow -> onRateNow()
                    RateLater -> _uiState.update { it.copy(showRatingBottomSheet = false) }
                    RateNever -> {
                        updateAppPreferenceUseCase.setRatingCompleted(true)
                        _uiState.update { it.copy(showRatingBottomSheet = false) }
                    }
                    else -> Unit
                }
            }
        }

        private fun observePreferences() {
            observeAppPreferencesUseCase().onEach { appPrefs ->
                val prevGlobalEnabled = isGlobalAlarmEnabled
                isGlobalAlarmEnabled = appPrefs.isGlobalAlarmEnabled
                alarmOffsetMinutes = appPrefs.alarmOffsetMinutes

                _uiState.update { state ->
                    state.copy(
                        enabledCalendarIds = appPrefs.enabledCalendarIds.mapNotNull { it.toLongOrNull() }.toSet(),
                    )
                }

                if (prevGlobalEnabled && !appPrefs.isGlobalAlarmEnabled) {
                    _uiState.value.events.forEach { cancelEventAlarmUseCase(it) }
                }

                refreshEvents()
            }.launchIn(viewModelScope)
        }

        private fun refreshEvents() {
            val permissions = checkPermissionsUseCase()
            if (!permissions.hasCalendarPermission) return

            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                val calendarEvents = getEventsForMonthUseCase(_uiState.value.currentMonth)
                val appPrefs = observeAppPreferencesUseCase().first()

                val updatedEvents =
                    calendarEvents.map { event ->
                        val isInstanceDisabled = appPrefs.disabledEventIds.contains(event.uniqueIntentId.toString())
                        val isSeriesDisabled = appPrefs.disabledSeriesIds.contains(event.id.toString())
                        val isVibrateOnly = appPrefs.vibrateOnlyEventIds.contains(event.uniqueIntentId.toString())
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

                if (isGlobalAlarmEnabled) {
                    scheduleImmediateEvents(enrichedEvents)
                }
            }
        }

        private fun detectConflictsAndBuffers(events: List<Event>): List<Event> {
            if (events.size < 2) return events
            val nonAllDay = events.filter { !it.isAllDay && it.endTime > it.startTime }
            val conflictIds = mutableSetOf<Long>()
            val backToBackIds = mutableSetOf<Long>()

            for (i in nonAllDay.indices) {
                for (j in i + 1 until nonAllDay.size) {
                    val a = nonAllDay[i]
                    val b = nonAllDay[j]
                    if (a.startTime < b.endTime && b.startTime < a.endTime) {
                        conflictIds.add(a.id)
                        conflictIds.add(b.id)
                    }
                    val gap = b.startTime - a.endTime
                    if (gap in 0..300_000) {
                        backToBackIds.add(a.id)
                        backToBackIds.add(b.id)
                    }
                }
            }
            return events.map { event ->
                event.copy(hasConflict = event.id in conflictIds, isBackToBack = event.id in backToBackIds)
            }
        }

        private fun scheduleImmediateEvents(events: List<Event>) {
            val now = System.currentTimeMillis()
            val windowEnd = now + TimeUnit.MINUTES.toMillis(75 + alarmOffsetMinutes)

            viewModelScope.launch {
                events.filter { it.isAlarmEnabled }.forEach { event ->
                    val triggerTime =
                        if (_uiState.value.isAiUser && event.location != null) {
                            calculateDepartureTimeUseCase(event)?.departureTime
                        } else {
                            null
                        }
                    val alarmFireTime =
                        triggerTime ?: (
                            event.startTime - TimeUnit.MINUTES.toMillis(alarmOffsetMinutes)
                        )
                    if (alarmFireTime in (now + 1)..windowEnd) {
                        scheduleEventAlarmUseCase(event, triggerTime)
                    }
                }
            }
        }

        private fun onMonthChanged(yearMonth: YearMonth) {
            _uiState.update { it.copy(currentMonth = yearMonth, isRefreshing = true) }
            refreshEvents()
        }

        private fun returnToToday() {
            _uiState.update { it.copy(selectedDate = LocalDate.now(), currentMonth = YearMonth.now()) }
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

            if (current.isEmpty()) current.addAll(allIds)
            if (enabled) current.add(calendarId) else current.remove(calendarId)

            val newLongSet: Set<Long> = if (current.containsAll(allIds)) emptySet() else current
            _uiState.update { it.copy(enabledCalendarIds = newLongSet) }

            val newStringSet = newLongSet.map { it.toString() }.toSet()
            updateAppPreferenceUseCase.setEnabledCalendarIds(newStringSet)
            refreshEvents()
        }

        private fun createEvent(intent: CreateEvent) {
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
                    logEventUseCase.logEventCreated()
                    handleIntent(DismissCreateEventDialog)

                    val eventDate = Instant.ofEpochMilli(intent.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    val eventMonth = YearMonth.from(eventDate)

                    _uiState.update { it.copy(selectedDate = eventDate) }
                    if (eventMonth != _uiState.value.currentMonth) {
                        onMonthChanged(eventMonth)
                    }

                    if (!_uiState.value.enabledCalendarIds.contains(intent.calendarId)) {
                        onCalendarFilterToggle(intent.calendarId, true)
                    }

                    delay(500.milliseconds)
                    refreshEvents()
                    _uiState.update {
                        it.copy(
                            sideEffects =
                                it.sideEffects +
                                    ShowSnackbar(
                                        StringResource(R.string.ai_agent_event_created),
                                    ),
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            sideEffects =
                                it.sideEffects +
                                    AIToolError(StringResource(R.string.ai_agent_event_creation_error)),
                        )
                    }
                }
            }
        }

        private fun onRateNow() {
            viewModelScope.launch {
                updateAppPreferenceUseCase.setRatingCompleted(true)
                _uiState.update {
                    it.copy(
                        showRatingBottomSheet = false,
                        sideEffects = it.sideEffects + EventSideEffect.RequestAppReview,
                    )
                }
            }
        }

        private fun fetchWeather() {
            viewModelScope.launch {
                if (_uiState.value.isWeatherLoading) return@launch
                val appPrefs = observeAppPreferencesUseCase().first()
                val isCelsius = appPrefs.isTemperatureInCelsius
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
    }
