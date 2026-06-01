package digital.tonima.core.repository

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.dao.ChatHistoryDao
import digital.tonima.core.database.entity.ConversationEntity
import digital.tonima.core.database.mapper.toChatMessage
import digital.tonima.core.database.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = ChatHistoryRepository::class)
class ChatHistoryRepositoryImpl
    @Inject
    constructor(
        private val chatHistoryDao: ChatHistoryDao,
    ) : ChatHistoryRepository {
        override fun observeConversations(): Flow<List<ConversationEntity>> {
            return chatHistoryDao.observeConversations()
        }

        override suspend fun createConversation(title: String): Long {
            return chatHistoryDao.insertConversation(ConversationEntity(title = title))
        }

        override suspend fun deleteConversation(id: Long) {
            chatHistoryDao.deleteConversation(id)
        }

        override fun observeHistory(conversationId: Long): Flow<List<ChatMessage>> {
            return chatHistoryDao.observeHistory(conversationId).map { entities ->
                entities.mapNotNull { it.toChatMessage() }
            }
        }

        override suspend fun getHistory(conversationId: Long): List<ChatMessage> {
            return chatHistoryDao.getHistory(conversationId).mapNotNull { it.toChatMessage() }
        }

        override suspend fun insertMessage(
            conversationId: Long,
            message: ChatMessage,
        ): Long {
            chatHistoryDao.updateConversationTimestamp(conversationId, System.currentTimeMillis())
            return chatHistoryDao.insertMessage(message.toEntity(conversationId))
        }

        override suspend fun clearHistory(conversationId: Long): Int {
            return chatHistoryDao.clearHistory(conversationId)
        }
    }
