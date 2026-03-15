package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleEventVibrateUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) {
        suspend operator fun invoke(
            event: Event,
            enabled: Boolean,
        ) {
            val currentVibrateOnlyIds =
                repository
                    .getVibrateOnlyEventIds()
                    .firstOrNull()
                    ?.toMutableSet() ?: mutableSetOf()
            val eventIdStr = event.uniqueIntentId.toString()

            if (enabled) {
                currentVibrateOnlyIds.add(eventIdStr)
            } else {
                currentVibrateOnlyIds.remove(eventIdStr)
            }
            repository.setVibrateOnlyEventIds(currentVibrateOnlyIds)
        }
    }
