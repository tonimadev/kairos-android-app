package digital.tonima.core.data.usecases

import digital.tonima.core.notifications.SuggestedEvent
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the event the user accepted from a notification suggestion, in the first writable calendar. */
@Singleton
class CreateSuggestedEventUseCase
    @Inject
    constructor(
        private val getAvailableCalendarsUseCase: GetAvailableCalendarsUseCase,
        private val createEventUseCase: CreateEventUseCase,
    ) {
        /** Returns true when the event was created. */
        suspend operator fun invoke(suggestion: SuggestedEvent): Boolean {
            val calendar = getAvailableCalendarsUseCase().firstOrNull() ?: return false
            val eventId =
                createEventUseCase(
                    calendarId = calendar.id,
                    title = suggestion.title,
                    startTime = suggestion.startMillis,
                    endTime = suggestion.endMillis,
                )
            return eventId != null
        }
    }
