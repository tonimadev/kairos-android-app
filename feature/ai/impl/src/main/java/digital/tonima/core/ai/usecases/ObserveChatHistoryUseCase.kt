package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveChatHistoryUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        operator fun invoke(conversationId: Long): Flow<List<ChatMessage>> {
            return repository.observeHistory(conversationId)
        }
    }
