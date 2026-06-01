package digital.tonima.core.repository

import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun observeConversations(): Flow<List<ConversationEntity>>

    suspend fun createConversation(title: String): Long

    suspend fun deleteConversation(id: Long)

    fun observeHistory(conversationId: Long): Flow<List<ChatMessage>>

    suspend fun getHistory(conversationId: Long): List<ChatMessage>

    suspend fun insertMessage(
        conversationId: Long,
        message: ChatMessage,
    ): Long

    suspend fun clearHistory(conversationId: Long): Int
}
