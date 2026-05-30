package digital.tonima.core.repository

import com.paulrybitskyi.hiltbinder.BindType
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.core.database.dao.ChatHistoryDao
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
        override fun observeHistory(): Flow<List<ChatMessage>> {
            return chatHistoryDao.observeHistory().map { entities ->
                entities.mapNotNull { it.toChatMessage() }
            }
        }

        override suspend fun getHistory(): List<ChatMessage> {
            return chatHistoryDao.getHistory().mapNotNull { it.toChatMessage() }
        }

        override suspend fun insertMessage(message: ChatMessage): Long {
            return chatHistoryDao.insertMessage(message.toEntity())
        }

        override suspend fun clearHistory(): Int {
            return chatHistoryDao.clearHistory()
        }
    }
