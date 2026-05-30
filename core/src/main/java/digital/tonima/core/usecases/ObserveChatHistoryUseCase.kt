package digital.tonima.core.usecases

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveChatHistoryUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        operator fun invoke(): Flow<List<ChatMessage>> {
            return repository.observeHistory()
        }
    }
