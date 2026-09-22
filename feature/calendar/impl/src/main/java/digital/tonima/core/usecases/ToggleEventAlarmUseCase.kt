package digital.tonima.core.usecases

import digital.tonima.core.data.usecases.GetEventsForMonthUseCase
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.kairos.core.model.Event
import kotlinx.coroutines.flow.firstOrNull
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleEventAlarmUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
        private val scheduler: EventAlarmScheduler,
        private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
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

            if (repository.isGlobalAlarmEnabled().firstOrNull() != true) return

            if (disableAllOccurrences) {
                // A recurring series can have several occurrences already scheduled in
                // AlarmManager (from previous worker runs). Toggling the series must
                // reach every one of them, not just the occurrence the user tapped on.
                getSeriesOccurrences(event).forEach { occurrence ->
                    if (isEnabled) scheduler.schedule(occurrence) else scheduler.cancel(occurrence)
                }
            } else {
                if (isEnabled) {
                    scheduler.schedule(event)
                } else {
                    scheduler.cancel(event)
                }
            }
        }

        private suspend fun getSeriesOccurrences(event: Event): List<Event> {
            val currentMonth = YearMonth.now().atDay(1).toEpochDay()
            val nextMonth = YearMonth.now().plusMonths(1).atDay(1).toEpochDay()
            val occurrences =
                (getEventsForMonthUseCase(currentMonth) + getEventsForMonthUseCase(nextMonth))
                    .filter { it.id == event.id }
            // Fall back to the single occurrence if the provider can't locate the series
            // (e.g. it was already removed from the calendar).
            return occurrences.ifEmpty { listOf(event) }
        }
    }
