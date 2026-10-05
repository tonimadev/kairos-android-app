package digital.tonima.core.data.usecases

import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** Which notification-access features the user opted into. All of them are off by default. */
data class NotificationFeatures(
    val dedupCalendarReminder: Boolean = false,
    val focusDigest: Boolean = false,
    val eventSuggestions: Boolean = false,
) {
    val anyEnabled: Boolean get() = dedupCalendarReminder || focusDigest || eventSuggestions
}

@Singleton
class ObserveNotificationFeaturesUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) {
        operator fun invoke(): Flow<NotificationFeatures> =
            combine(
                repository.isNotificationDedupEnabled(),
                repository.isFocusDigestEnabled(),
                repository.isEventSuggestionsEnabled(),
            ) { dedup, digest, suggestions ->
                NotificationFeatures(dedup, digest, suggestions)
            }
    }
