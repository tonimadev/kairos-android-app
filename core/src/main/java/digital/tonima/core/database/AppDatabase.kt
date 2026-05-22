package digital.tonima.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import digital.tonima.core.database.dao.ChatHistoryDao
import digital.tonima.core.database.entity.ChatHistoryEntity

@Database(
    entities = [ChatHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatHistoryDao(): ChatHistoryDao
}
