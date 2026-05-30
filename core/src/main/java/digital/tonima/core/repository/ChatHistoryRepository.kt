package digital.tonima.core.repository

import digital.tonima.core.ai.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun observeHistory(): Flow<List<ChatMessage>>

    suspend fun getHistory(): List<ChatMessage>

    suspend fun insertMessage(message: ChatMessage): Long

    suspend fun clearHistory(): Int
}
