package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.CalendarRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = GetNextEventUseCase::class)
class GetNextEventUseCaseImpl @Inject constructor(
    private val eventsRepository: CalendarRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : GetNextEventUseCase {
    override suspend fun invoke(): Event? {
        val enabledCalendarIdStrings = appPreferencesRepository.getEnabledCalendarIds().firstOrNull() ?: emptySet()
        val allowedCalendarIds = enabledCalendarIdStrings.mapNotNull { it.toLongOrNull() }
        return eventsRepository.getNextUpcomingEvent(allowedCalendarIds)
    }
}
