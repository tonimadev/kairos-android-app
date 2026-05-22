package digital.tonima.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import digital.tonima.core.database.entity.ChatHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun observeHistory(): Flow<List<ChatHistoryEntity>>

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    suspend fun getHistory(): List<ChatHistoryEntity>

    @Insert
    suspend fun insertMessage(message: ChatHistoryEntity): Long

    @Query("DELETE FROM chat_history")
    suspend fun clearHistory(): Int
}
