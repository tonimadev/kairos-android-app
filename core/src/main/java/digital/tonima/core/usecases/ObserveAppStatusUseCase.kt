package digital.tonima.core.usecases

import digital.tonima.core.repository.AppPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveAppStatusUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) {
        fun getCustomRingtoneUri() = repository.getCustomRingtoneUri()

        fun isOnboardingCompleted() = repository.isOnboardingCompleted()

        fun getSnoozeCount() = repository.getSnoozeCount()

        fun getAiUsageCount() = repository.getAiUsageCount()

        fun getWakeUpHistory() = repository.getWakeUpHistory()
    }
