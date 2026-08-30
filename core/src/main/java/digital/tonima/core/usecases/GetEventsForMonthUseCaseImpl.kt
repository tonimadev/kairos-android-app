package digital.tonima.core.usecases

import com.google.common.collect.ImmutableList
import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.CalendarRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GetEventsForMonthUseCase::class)
class GetEventsForMonthUseCaseImpl
    @Inject
    constructor(
        private val eventsRepository: CalendarRepository,
        private val appPreferencesRepository: AppPreferencesRepository,
    ) : GetEventsForMonthUseCase {
        override suspend operator fun invoke(yearMonth: Long): List<Event> {
            val enabledCalendarIdStrings = appPreferencesRepository.getEnabledCalendarIds().firstOrNull() ?: emptySet()
            val allowedCalendarIds = ImmutableList.copyOf(enabledCalendarIdStrings.mapNotNull { it.toLongOrNull() })
            return eventsRepository.getEventsForMonth(yearMonth, allowedCalendarIds)
        }
    }
