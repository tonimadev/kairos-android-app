package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.ai.repository.ChatHistoryRepository
import javax.inject.Inject

class InsertChatMessageUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        suspend operator fun invoke(
            conversationId: Long,
            message: ChatMessage,
        ): Long {
            return repository.insertMessage(conversationId, message)
        }
    }
