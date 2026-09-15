package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.repository.DailyBriefingRepository
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
