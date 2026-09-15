package digital.tonima.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.database.AppDatabase
import digital.tonima.core.database.dao.ChatHistoryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kairos_database",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChatHistoryDao(appDatabase: AppDatabase): ChatHistoryDao {
        return appDatabase.chatHistoryDao()
    }
}
