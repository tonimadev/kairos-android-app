package digital.tonima.core.ai.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.repository.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = DailyBriefingRepository::class)
class DailyBriefingRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DailyBriefingRepository {
        private object PreferencesKeys {
            val DAILY_BRIEFING = stringPreferencesKey("daily_briefing")
            val DAILY_BRIEFING_DATE = longPreferencesKey("daily_briefing_date")
        }

        override fun getDailyBriefing(): Flow<String?> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.DAILY_BRIEFING]
                }
        }

        override suspend fun saveDailyBriefing(
            briefing: String,
            generatedDate: LocalDate,
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DAILY_BRIEFING] = briefing
                preferences[PreferencesKeys.DAILY_BRIEFING_DATE] = generatedDate.toEpochDay()
            }
        }

        override suspend fun getLastGeneratedDate(): LocalDate? {
            val epochDay = context.dataStore.data.first()[PreferencesKeys.DAILY_BRIEFING_DATE] ?: return null
            return LocalDate.ofEpochDay(epochDay)
        }
    }
