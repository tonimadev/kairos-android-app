package digital.tonima.core.ai.usecases

import digital.tonima.core.ai.repository.ChatHistoryRepository
import digital.tonima.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConversationsUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        operator fun invoke(): Flow<List<ConversationEntity>> = repository.observeConversations()
    }
