package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.repository.ChatHistoryRepository
import javax.inject.Inject

class DeleteConversationUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        suspend operator fun invoke(id: Long) = repository.deleteConversation(id)
    }
