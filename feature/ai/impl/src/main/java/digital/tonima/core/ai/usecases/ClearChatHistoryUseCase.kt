package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.repository.ChatHistoryRepository
import javax.inject.Inject

class ClearChatHistoryUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        suspend operator fun invoke(conversationId: Long): Int {
            return repository.clearHistory(conversationId)
        }
    }
