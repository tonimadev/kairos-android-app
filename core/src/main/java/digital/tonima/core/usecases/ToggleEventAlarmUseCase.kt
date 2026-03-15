package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.EventAlarmScheduler
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleEventAlarmUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
        private val scheduler: EventAlarmScheduler,
    ) {
        suspend operator fun invoke(
            event: Event,
            isEnabled: Boolean,
            disableAllOccurrences: Boolean,
        ) {
            val currentDisabledInstanceIds =
                repository
                    .getDisabledEventIds()
                    .firstOrNull()
                    ?.toMutableSet() ?: mutableSetOf()
            val currentDisabledSeriesIds =
                repository
                    .getDisabledSeriesIds().firstOrNull()
                    ?.toMutableSet() ?: mutableSetOf()
            val instanceIdStr = event.uniqueIntentId.toString()
            val seriesIdStr = event.id.toString()

            if (isEnabled) {
                if (disableAllOccurrences) {
                    currentDisabledSeriesIds.remove(seriesIdStr)
                } else {
                    currentDisabledInstanceIds.remove(instanceIdStr)
                }
            } else {
                if (disableAllOccurrences) {
                    currentDisabledSeriesIds.add(seriesIdStr)
                } else {
                    currentDisabledInstanceIds.add(instanceIdStr)
                }
            }

            repository.setDisabledEventIds(currentDisabledInstanceIds)
            repository.setDisabledSeriesIds(currentDisabledSeriesIds)

            if (repository.isGlobalAlarmEnabled().firstOrNull() == true) {
                if (isEnabled) {
                    scheduler.schedule(event)
                } else {
                    scheduler.cancel(event)
                }
            }
        }
    }
