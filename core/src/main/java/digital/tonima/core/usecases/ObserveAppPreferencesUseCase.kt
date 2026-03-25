package digital.tonima.core.usecases

import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class AppPreferences(
    val isGlobalAlarmEnabled: Boolean,
    val vibrateOnly: Boolean,
    val allDayAlarmsEnabled: Boolean,
    val allDayAlarmHour: Int,
    val alarmOffsetMinutes: Long,
    val isLocationAlarmEnabled: Boolean,
    val preferredTransportMode: String,
    val enabledCalendarIds: Set<String>,
    val snoozeTimeMinutes: Int,
    val autostartSuggestionDismissed: Boolean,
    val disabledEventIds: Set<String>,
    val disabledSeriesIds: Set<String>,
    val vibrateOnlyEventIds: Set<String>,
    val exactAlarmPermissionSkipped: Boolean,
    val fullScreenIntentPermissionSkipped: Boolean,
)

@Singleton
class ObserveAppPreferencesUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) {
        operator fun invoke(): Flow<AppPreferences> {
            return combine(
                repository.isGlobalAlarmEnabled(),
                repository.getVibrateOnly(),
                repository.isAllDayAlarmsEnabled(),
                repository.getAllDayAlarmHour(),
                repository.getAlarmOffsetMinutes(),
                repository.isLocationAlarmEnabled(),
                repository.getPreferredTransportMode(),
                repository.getEnabledCalendarIds(),
                repository.getSnoozeTimeMinutes(),
                repository.getAutostartSuggestionDismissed(),
                repository.getDisabledEventIds(),
                repository.getDisabledSeriesIds(),
                repository.getVibrateOnlyEventIds(),
                repository.isExactAlarmPermissionSkipped(),
                repository.isFullScreenIntentPermissionSkipped(),
            ) { args ->
                AppPreferences(
                    isGlobalAlarmEnabled = args[0] as Boolean,
                    vibrateOnly = args[1] as Boolean,
                    allDayAlarmsEnabled = args[2] as Boolean,
                    allDayAlarmHour = args[3] as Int,
                    alarmOffsetMinutes = args[4] as Long,
                    isLocationAlarmEnabled = args[5] as Boolean,
                    preferredTransportMode = args[6] as String,
                    enabledCalendarIds = args[7] as Set<String>,
                    snoozeTimeMinutes = args[8] as Int,
                    autostartSuggestionDismissed = args[9] as Boolean,
                    disabledEventIds = args[10] as Set<String>,
                    disabledSeriesIds = args[11] as Set<String>,
                    vibrateOnlyEventIds = args[12] as Set<String>,
                    exactAlarmPermissionSkipped = args[13] as Boolean,
                    fullScreenIntentPermissionSkipped = args[14] as Boolean,
                )
            }
        }
    }
