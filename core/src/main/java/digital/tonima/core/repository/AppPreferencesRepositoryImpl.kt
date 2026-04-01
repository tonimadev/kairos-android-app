package digital.tonima.core.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@BindType(installIn = BindType.Component.SINGLETON, to = AppPreferencesRepository::class)
class AppPreferencesRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AppPreferencesRepository {
        private object PreferencesKeys {
            val GLOBAL_ALARM_ENABLED = booleanPreferencesKey("global_alarm_enabled")
            val DISABLED_EVENT_IDS = stringSetPreferencesKey("disabled_event_ids")
            val DISABLED_SERIES_IDS = stringSetPreferencesKey("disabled_series_ids")
            val VIBRATE_ONLY_EVENT_IDS = stringSetPreferencesKey("vibrate_only_event_ids")
            val VIBRATE_ONLY = booleanPreferencesKey("vibrate_only")
            val AUTOSTART_SUGGESTION_DISMISSED = booleanPreferencesKey("autostart_suggestion_dismissed")
            val INSTALLATION_DATE = longPreferencesKey("installation_date")
            val RATING_PROMPTED = booleanPreferencesKey("rating_prompted")
            val RATING_COMPLETED = booleanPreferencesKey("rating_completed")
            val ALL_DAY_ALARMS_ENABLED = booleanPreferencesKey("all_day_alarms_enabled")
            val ALL_DAY_ALARM_HOUR = intPreferencesKey("all_day_alarm_hour")
            val ALARM_OFFSET_MINUTES = longPreferencesKey("alarm_offset_minutes")
            val ENABLED_CALENDAR_IDS = stringSetPreferencesKey("enabled_calendar_ids")
            val SNOOZE_TIME_MINUTES = intPreferencesKey("snooze_time_minutes")
            val WAKE_UP_HISTORY = stringSetPreferencesKey("wake_up_history")
            val PREFERRED_CITY = stringPreferencesKey("preferred_city")
            val LOCATION_ALARM_ENABLED = booleanPreferencesKey("location_alarm_enabled")
            val PREFERRED_TRANSPORT_MODE = stringPreferencesKey("preferred_transport_mode")
            val EXACT_ALARM_PERMISSION_SKIPPED = booleanPreferencesKey("exact_alarm_permission_skipped")
            val FULL_SCREEN_INTENT_PERMISSION_SKIPPED = booleanPreferencesKey("full_screen_intent_permission_skipped")
            val SYNC_ALERT_MUTED_UNTIL = longPreferencesKey("sync_alert_muted_until")
        }

        override fun isGlobalAlarmEnabled(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.GLOBAL_ALARM_ENABLED] ?: true
                }
        }

        override suspend fun setGlobalAlarmEnabled(enabled: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GLOBAL_ALARM_ENABLED] = enabled
            }
        }

        override fun getDisabledEventIds(): Flow<Set<String>> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.DISABLED_EVENT_IDS] ?: emptySet()
                }
        }

        override suspend fun setDisabledEventIds(ids: Set<String>) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DISABLED_EVENT_IDS] = ids
            }
        }

        override fun getDisabledSeriesIds(): Flow<Set<String>> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.DISABLED_SERIES_IDS] ?: emptySet()
                }
        }

        override suspend fun setDisabledSeriesIds(ids: Set<String>) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DISABLED_SERIES_IDS] = ids
            }
        }

        override fun getVibrateOnlyEventIds(): Flow<Set<String>> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.VIBRATE_ONLY_EVENT_IDS] ?: emptySet()
                }
        }

        override suspend fun setVibrateOnlyEventIds(ids: Set<String>) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.VIBRATE_ONLY_EVENT_IDS] = ids
            }
        }

        override fun getVibrateOnly(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.VIBRATE_ONLY] ?: false
                }
        }

        override suspend fun setVibrateOnly(enabled: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.VIBRATE_ONLY] = enabled
            }
        }

        override fun getAutostartSuggestionDismissed(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.AUTOSTART_SUGGESTION_DISMISSED] ?: false
                }
        }

        override suspend fun setAutostartSuggestionDismissed(dismissed: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTOSTART_SUGGESTION_DISMISSED] = dismissed
            }
        }

        override fun getInstallationDate(): Flow<Long> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.INSTALLATION_DATE] ?: 0L
                }
        }

        override suspend fun setInstallationDate(date: Long) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.INSTALLATION_DATE] = date
            }
        }

        override fun isRatingPrompted(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.RATING_PROMPTED] ?: false
                }
        }

        override suspend fun setRatingPrompted(prompted: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RATING_PROMPTED] = prompted
            }
        }

        override fun isRatingCompleted(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.RATING_COMPLETED] ?: false
                }
        }

        override suspend fun setRatingCompleted(completed: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.RATING_COMPLETED] = completed
            }
        }

        override fun isAllDayAlarmsEnabled(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.ALL_DAY_ALARMS_ENABLED] ?: true
                }
        }

        override suspend fun setAllDayAlarmsEnabled(enabled: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ALL_DAY_ALARMS_ENABLED] = enabled
            }
        }

        override fun getAllDayAlarmHour(): Flow<Int> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.ALL_DAY_ALARM_HOUR] ?: 9
                }
        }

        override suspend fun setAllDayAlarmHour(hour: Int) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ALL_DAY_ALARM_HOUR] = hour
            }
        }

        override fun getAlarmOffsetMinutes(): Flow<Long> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.ALARM_OFFSET_MINUTES] ?: 0L
                }
        }

        override suspend fun setAlarmOffsetMinutes(minutes: Long) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ALARM_OFFSET_MINUTES] = minutes
            }
        }

        override fun getEnabledCalendarIds(): Flow<Set<String>> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.ENABLED_CALENDAR_IDS] ?: emptySet()
                }
        }

        override suspend fun setEnabledCalendarIds(ids: Set<String>) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ENABLED_CALENDAR_IDS] = ids
            }
        }

        override fun getSnoozeTimeMinutes(): Flow<Int> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.SNOOZE_TIME_MINUTES] ?: 10
                }
        }

        override suspend fun setSnoozeTimeMinutes(minutes: Int) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SNOOZE_TIME_MINUTES] = minutes
            }
        }

        override fun getWakeUpHistory(): Flow<List<Long>> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.WAKE_UP_HISTORY]
                        ?.mapNotNull { it.toLongOrNull() }
                        ?.sorted()
                        ?: emptyList()
                }
        }

        override suspend fun addWakeUpTimestamp(timestamp: Long) {
            context.dataStore.edit { preferences ->
                val currentHistory =
                    preferences[PreferencesKeys.WAKE_UP_HISTORY]
                        ?.mapNotNull { it.toLongOrNull() }
                        ?.toMutableList() ?: mutableListOf()

                currentHistory.add(timestamp)

                // Manter apenas os últimos 14 dias de histórico para análise
                val limitedHistory =
                    currentHistory
                        .sortedDescending()
                        .take(14)
                        .map { it.toString() }
                        .toSet()

                preferences[PreferencesKeys.WAKE_UP_HISTORY] = limitedHistory
            }
        }

        override fun getPreferredCity(): Flow<String?> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.PREFERRED_CITY]
                }
        }

        override suspend fun setPreferredCity(city: String) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.PREFERRED_CITY] = city
            }
        }

        override fun isLocationAlarmEnabled(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.LOCATION_ALARM_ENABLED] ?: false
                }
        }

        override suspend fun setLocationAlarmEnabled(enabled: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.LOCATION_ALARM_ENABLED] = enabled
            }
        }

        override fun getPreferredTransportMode(): Flow<String> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.PREFERRED_TRANSPORT_MODE] ?: "driving"
                }
        }

        override suspend fun setPreferredTransportMode(mode: String) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.PREFERRED_TRANSPORT_MODE] = mode
            }
        }

        override fun isExactAlarmPermissionSkipped(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.EXACT_ALARM_PERMISSION_SKIPPED] ?: false
                }
        }

        override suspend fun setExactAlarmPermissionSkipped(skipped: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.EXACT_ALARM_PERMISSION_SKIPPED] = skipped
            }
        }

        override fun isFullScreenIntentPermissionSkipped(): Flow<Boolean> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.FULL_SCREEN_INTENT_PERMISSION_SKIPPED] ?: false
                }
        }

        override suspend fun setFullScreenIntentPermissionSkipped(skipped: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FULL_SCREEN_INTENT_PERMISSION_SKIPPED] = skipped
            }
        }

        override fun getSyncAlertMutedUntil(): Flow<Long> {
            return context.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.SYNC_ALERT_MUTED_UNTIL] ?: 0L
                }
        }

        override suspend fun setSyncAlertMutedUntil(timestamp: Long) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SYNC_ALERT_MUTED_UNTIL] = timestamp
            }
        }
    }
