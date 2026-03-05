package digital.tonima.core.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

interface AppPreferencesRepository {
    fun isGlobalAlarmEnabled(): Flow<Boolean>
    suspend fun setGlobalAlarmEnabled(enabled: Boolean)

    fun getDisabledEventIds(): Flow<Set<String>>
    suspend fun setDisabledEventIds(ids: Set<String>)

    fun getDisabledSeriesIds(): Flow<Set<String>>
    suspend fun setDisabledSeriesIds(ids: Set<String>)

    fun getVibrateOnlyEventIds(): Flow<Set<String>>
    suspend fun setVibrateOnlyEventIds(ids: Set<String>)

    fun getVibrateOnly(): Flow<Boolean>
    suspend fun setVibrateOnly(enabled: Boolean)

    fun getAutostartSuggestionDismissed(): Flow<Boolean>
    suspend fun setAutostartSuggestionDismissed(dismissed: Boolean)

    fun getInstallationDate(): Flow<Long>
    suspend fun setInstallationDate(date: Long)

    fun isRatingPrompted(): Flow<Boolean>
    suspend fun setRatingPrompted(prompted: Boolean)

    fun isRatingCompleted(): Flow<Boolean>
    suspend fun setRatingCompleted(completed: Boolean)

    fun isAllDayAlarmsEnabled(): Flow<Boolean>
    suspend fun setAllDayAlarmsEnabled(enabled: Boolean)

    fun getAllDayAlarmHour(): Flow<Int>
    suspend fun setAllDayAlarmHour(hour: Int)

    fun getAlarmOffsetMinutes(): Flow<Long>
    suspend fun setAlarmOffsetMinutes(minutes: Long)

    /**
     * Returns the set of calendar IDs the user has chosen to include.
     * An empty set means "all calendars" (no filter applied).
     */
    fun getEnabledCalendarIds(): Flow<Set<String>>
    suspend fun setEnabledCalendarIds(ids: Set<String>)
}
