package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.usecases.CheckPermissionsUseCase
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.ObserveRingerModeUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.core.viewmodel.SettingsIntent.ChangeTransportMode
import digital.tonima.core.viewmodel.SettingsIntent.CheckPermissions
import digital.tonima.core.viewmodel.SettingsIntent.CloseSettings
import digital.tonima.core.viewmodel.SettingsIntent.ConsumeEffect
import digital.tonima.core.viewmodel.SettingsIntent.DismissAutostartSuggestion
import digital.tonima.core.viewmodel.SettingsIntent.OpenSettings
import digital.tonima.core.viewmodel.SettingsIntent.SkipExactAlarmPermission
import digital.tonima.core.viewmodel.SettingsIntent.SkipFullScreenIntentPermission
import digital.tonima.core.viewmodel.SettingsIntent.ToggleAllDayAlarms
import digital.tonima.core.viewmodel.SettingsIntent.ToggleAutoFocusMode
import digital.tonima.core.viewmodel.SettingsIntent.ToggleAutoJoin
import digital.tonima.core.viewmodel.SettingsIntent.ToggleGlobalAlarms
import digital.tonima.core.viewmodel.SettingsIntent.ToggleLocationAlarm
import digital.tonima.core.viewmodel.SettingsIntent.ToggleSkipWeekends
import digital.tonima.core.viewmodel.SettingsIntent.ToggleTemperatureUnit
import digital.tonima.core.viewmodel.SettingsIntent.ToggleVibrateOnly
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAlarmOffset
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAllDayAlarmHour
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAutoDismissMinutes
import digital.tonima.core.viewmodel.SettingsIntent.UpdateCustomRingtoneUri
import digital.tonima.core.viewmodel.SettingsIntent.UpdateSnoozeTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val observeAppPreferencesUseCase: ObserveAppPreferencesUseCase,
        private val updateAppPreferenceUseCase: UpdateAppPreferenceUseCase,
        private val checkPermissionsUseCase: CheckPermissionsUseCase,
        private val observeRingerModeUseCase: ObserveRingerModeUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState = _uiState.asStateFlow()

        val effect = uiState.map { it.effect }.distinctUntilChanged()

        init {
            observePreferences()
            observeRingerMode()
            checkPermissions()
        }

        fun handleIntent(intent: SettingsIntent) {
            viewModelScope.launch {
                when (intent) {
                    is ConsumeEffect -> _uiState.update { it.copy(effect = null) }
                    is ToggleGlobalAlarms -> updateAppPreferenceUseCase.setGlobalAlarmEnabled(intent.enabled)
                    is ToggleVibrateOnly -> updateAppPreferenceUseCase.setVibrateOnly(intent.enabled)
                    is ToggleAllDayAlarms -> updateAppPreferenceUseCase.setAllDayAlarmsEnabled(intent.enabled)
                    is UpdateAllDayAlarmHour -> updateAppPreferenceUseCase.setAllDayAlarmHour(intent.hour)
                    is UpdateAlarmOffset -> updateAppPreferenceUseCase.setAlarmOffsetMinutes(intent.offset.minutes)
                    is UpdateSnoozeTime -> updateAppPreferenceUseCase.setSnoozeTimeMinutes(intent.minutes)
                    is ToggleSkipWeekends -> updateAppPreferenceUseCase.setSkipWeekendsEnabled(intent.enabled)
                    is UpdateAutoDismissMinutes -> updateAppPreferenceUseCase.setAutoDismissMinutes(intent.minutes)
                    is ToggleLocationAlarm -> updateAppPreferenceUseCase.setLocationAlarmEnabled(intent.enabled)
                    is ToggleAutoJoin -> updateAppPreferenceUseCase.setAutoJoinEnabled(intent.enabled)
                    is ToggleAutoFocusMode -> updateAppPreferenceUseCase.setAutoFocusModeEnabled(intent.enabled)
                    is ChangeTransportMode -> updateAppPreferenceUseCase.setPreferredTransportMode(intent.mode)
                    is ToggleTemperatureUnit -> updateAppPreferenceUseCase.setTemperatureInCelsius(intent.isCelsius)
                    DismissAutostartSuggestion -> updateAppPreferenceUseCase.setAutostartSuggestionDismissed(true)
                    CheckPermissions -> checkPermissions()
                    SkipExactAlarmPermission -> {
                        updateAppPreferenceUseCase.setExactAlarmPermissionSkipped(true)
                        checkPermissions()
                    }
                    SkipFullScreenIntentPermission -> {
                        updateAppPreferenceUseCase.setFullScreenIntentPermissionSkipped(true)
                        checkPermissions()
                    }
                    OpenSettings -> _uiState.update { it.copy(showSettingsScreen = true) }
                    CloseSettings -> _uiState.update { it.copy(showSettingsScreen = false) }
                    is UpdateCustomRingtoneUri -> _uiState.update { it.copy(customRingtoneUri = intent.uri) }
                }
            }
        }

        private fun observePreferences() {
            observeAppPreferencesUseCase().onEach { appPrefs ->
                _uiState.update { state ->
                    state.copy(
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
                    )
                }
            }.launchIn(viewModelScope)
        }

        private fun observeRingerMode() {
            observeRingerModeUseCase().onEach { mode ->
                _uiState.update { it.copy(audioWarning = mode) }
            }.launchIn(viewModelScope)
        }

        private fun checkPermissions() {
            val p = checkPermissionsUseCase()
            _uiState.update {
                it.copy(
                    hasCalendarPermission = p.hasCalendarPermission,
                    hasPostNotificationsPermission = p.hasPostNotificationsPermission,
                    hasExactAlarmPermission = p.hasExactAlarmPermission || it.skippedExactAlarmPermission,
                    hasFullScreenIntentPermission =
                        p.hasFullScreenIntentPermission ||
                            it.skippedFullScreenIntentPermission,
                    hasLocationPermission = p.hasLocationPermission,
                    hasBackgroundLocationPermission = p.hasBackgroundLocationPermission,
                )
            }
        }
    }
