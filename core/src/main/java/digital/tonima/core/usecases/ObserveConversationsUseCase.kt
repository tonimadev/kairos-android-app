package digital.tonima.core.usecases

import digital.tonima.core.database.entity.ConversationEntity
import digital.tonima.core.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConversationsUseCase
    @Inject
    constructor(
        private val repository: ChatHistoryRepository,
    ) {
        operator fun invoke(): Flow<List<ConversationEntity>> = repository.observeConversations()
    }
