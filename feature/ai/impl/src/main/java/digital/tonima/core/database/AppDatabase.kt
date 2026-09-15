package digital.tonima.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import digital.tonima.core.database.dao.ChatHistoryDao
import digital.tonima.core.database.entity.ChatHistoryEntity
import digital.tonima.core.database.entity.ConversationEntity

@Database(
    entities = [ConversationEntity::class, ChatHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatHistoryDao(): ChatHistoryDao
}
