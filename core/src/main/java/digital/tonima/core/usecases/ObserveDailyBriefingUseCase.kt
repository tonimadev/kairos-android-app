package digital.tonima.core.usecases

import digital.tonima.core.repository.DailyBriefingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveDailyBriefingUseCase
    @Inject
    constructor(
        private val repository: DailyBriefingRepository,
    ) {
        operator fun invoke(): Flow<String?> = repository.getDailyBriefing()
    }
