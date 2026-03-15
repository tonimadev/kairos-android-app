package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.repository.CalendarRepository
import javax.inject.Inject

@BindType(installIn = BindType.Component.VIEW_MODEL)
class CreateEventUseCaseImpl
    @Inject
    constructor(
        private val calendarRepository: CalendarRepository,
    ) : CreateEventUseCase {
        override suspend operator fun invoke(
            calendarId: Long,
            title: String,
            description: String?,
            location: String?,
            startTime: Long,
            endTime: Long,
            isAllDay: Boolean,
        ): Long? {
            return calendarRepository.insertEvent(
                calendarId = calendarId,
                title = title,
                description = description,
                location = location,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
            )
        }
    }
