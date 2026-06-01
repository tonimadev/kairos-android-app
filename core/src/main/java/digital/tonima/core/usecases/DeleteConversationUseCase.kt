package digital.tonima.core.usecases

import digital.tonima.core.repository.ChatHistoryRepository
import javax.inject.Inject

class DeleteConversationUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        suspend operator fun invoke(id: Long) = repository.deleteConversation(id)
    }
