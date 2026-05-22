package digital.tonima.core.usecases

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.repository.CalendarRepository
import digital.tonima.core.repository.GoogleMeetRepository
import javax.inject.Inject

@BindType(installIn = BindType.Component.VIEW_MODEL)
class CreateEventUseCaseImpl
    @Inject
    constructor(
        private val calendarRepository: CalendarRepository,
        private val googleMeetRepository: GoogleMeetRepository,
    ) : CreateEventUseCase {
        override suspend operator fun invoke(
            calendarId: Long,
            title: String,
            description: String?,
            location: String?,
            startTime: Long,
            endTime: Long,
            isAllDay: Boolean,
            requestMeetLink: Boolean,
        ): Long? {
            var finalDescription = description
            var finalLocation = location

            if (requestMeetLink) {
                val meetResult = googleMeetRepository.createMeeting()
                meetResult.onSuccess { space ->
                    val meetLink = space.meetingUri
                    finalDescription =
                        if (description.isNullOrBlank()) {
                            "Google Meet: $meetLink"
                        } else {
                            "$description\n\nGoogle Meet: $meetLink"
                        }
                    if (finalLocation.isNullOrBlank()) {
                        finalLocation = meetLink
                    }
                }
            }

            return calendarRepository.insertEvent(
                calendarId = calendarId,
                title = title,
                description = finalDescription,
                location = finalLocation,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
            )
        }
    }
