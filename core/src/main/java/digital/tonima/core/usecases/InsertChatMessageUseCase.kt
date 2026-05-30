package digital.tonima.core.usecases

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.repository.ChatHistoryRepository
import javax.inject.Inject

class InsertChatMessageUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        suspend operator fun invoke(message: ChatMessage): Long {
            return repository.insertMessage(message)
        }
    }
