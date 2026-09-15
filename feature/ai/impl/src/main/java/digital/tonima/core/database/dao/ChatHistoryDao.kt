package digital.tonima.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import digital.tonima.core.database.entity.ChatHistoryEntity
import digital.tonima.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface ChatHistoryDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long): Int

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateConversationTimestamp(
        id: Long,
        timestamp: Long,
    ): Int

    @Query("SELECT * FROM chat_history WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeHistory(conversationId: Long): Flow<List<ChatHistoryEntity>>

    @Query("SELECT * FROM chat_history WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getHistory(conversationId: Long): List<ChatHistoryEntity>

    @Insert
    suspend fun insertMessage(message: ChatHistoryEntity): Long

    @Query("DELETE FROM chat_history WHERE conversationId = :conversationId")
    suspend fun clearHistory(conversationId: Long): Int
}
