package digital.tonima.core.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        }

        override fun getDailyBriefing(): Flow<String?> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.DAILY_BRIEFING]
                }
        }

        override suspend fun saveDailyBriefing(briefing: String) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DAILY_BRIEFING] = briefing
            }
        }
    }
