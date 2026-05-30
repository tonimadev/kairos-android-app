package digital.tonima.core.usecases

import digital.tonima.core.repository.AppPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateAppStatusUseCase
    @Inject
    constructor(
        private val repository: AppPreferencesRepository,
    ) {
        suspend fun incrementAiUsageCount() = repository.incrementAiUsageCount()

        suspend fun setCustomRingtoneUri(uri: String?) = repository.setCustomRingtoneUri(uri)

        suspend fun incrementSnoozeCount() = repository.incrementSnoozeCount()

        suspend fun addWakeUpTimestamp(timestamp: Long) = repository.addWakeUpTimestamp(timestamp)
    }
