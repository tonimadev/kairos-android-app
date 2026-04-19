package digital.tonima.core.usecases

import digital.tonima.core.repository.AppPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateAppPreferenceUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) {
        suspend fun setGlobalAlarmEnabled(enabled: Boolean) = repository.setGlobalAlarmEnabled(enabled)

        suspend fun setVibrateOnly(enabled: Boolean) = repository.setVibrateOnly(enabled)

        suspend fun setAllDayAlarmsEnabled(enabled: Boolean) = repository.setAllDayAlarmsEnabled(enabled)

        suspend fun setAllDayAlarmHour(hour: Int) = repository.setAllDayAlarmHour(hour)

        suspend fun setAlarmOffsetMinutes(minutes: Long) = repository.setAlarmOffsetMinutes(minutes)

        suspend fun setSnoozeTimeMinutes(minutes: Int) = repository.setSnoozeTimeMinutes(minutes)

        suspend fun setSkipWeekendsEnabled(enabled: Boolean) = repository.setSkipWeekendsEnabled(enabled)

        suspend fun setAutoDismissMinutes(minutes: Int) = repository.setAutoDismissMinutes(minutes)

        suspend fun setAutostartSuggestionDismissed(dismissed: Boolean) =
            repository.setAutostartSuggestionDismissed(dismissed)

        suspend fun setLocationAlarmEnabled(enabled: Boolean) = repository.setLocationAlarmEnabled(enabled)

        suspend fun setPreferredTransportMode(mode: String) = repository.setPreferredTransportMode(mode)

        suspend fun setEnabledCalendarIds(ids: Set<String>) = repository.setEnabledCalendarIds(ids)

        suspend fun setDisabledEventIds(ids: Set<String>) = repository.setDisabledEventIds(ids)

        suspend fun setDisabledSeriesIds(ids: Set<String>) = repository.setDisabledSeriesIds(ids)

        suspend fun setVibrateOnlyEventIds(ids: Set<String>) = repository.setVibrateOnlyEventIds(ids)

        suspend fun setRatingPrompted(prompted: Boolean) = repository.setRatingPrompted(prompted)

        suspend fun setRatingCompleted(completed: Boolean) = repository.setRatingCompleted(completed)

        suspend fun setExactAlarmPermissionSkipped(skipped: Boolean) =
            repository.setExactAlarmPermissionSkipped(skipped)

        suspend fun setFullScreenIntentPermissionSkipped(skipped: Boolean) =
            repository.setFullScreenIntentPermissionSkipped(skipped)
    }
