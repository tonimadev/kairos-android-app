package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.repository.ChatHistoryRepository
import javax.inject.Inject

class CreateConversationUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        suspend operator fun invoke(title: String): Long = repository.createConversation(title)
    }
