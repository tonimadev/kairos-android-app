package digital.tonima.core.usecases

import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.repository.RingerModeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveRingerModeUseCase
    @Inject
    constructor(
        private val repository: RingerModeRepository,
    ) {
        operator fun invoke(): Flow<AudioWarningState> {
            repository.startObserving()
            return repository.ringerMode
        }
    }
