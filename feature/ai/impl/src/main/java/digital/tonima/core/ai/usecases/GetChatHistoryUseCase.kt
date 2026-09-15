package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.repository.ChatHistoryRepository
import javax.inject.Inject

class GetChatHistoryUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        suspend operator fun invoke(conversationId: Long): List<ChatMessage> {
            return repository.getHistory(conversationId)
        }
    }
