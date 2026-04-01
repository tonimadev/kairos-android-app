package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

interface AppStatusRepository {
    fun getAutostartSuggestionDismissed(): Flow<Boolean>

    suspend fun setAutostartSuggestionDismissed(dismissed: Boolean)

    fun getInstallationDate(): Flow<Long>

    suspend fun setInstallationDate(date: Long)

    fun isRatingPrompted(): Flow<Boolean>

    suspend fun setRatingPrompted(prompted: Boolean)

    fun isRatingCompleted(): Flow<Boolean>

    suspend fun setRatingCompleted(completed: Boolean)

    fun getWakeUpHistory(): Flow<List<Long>>

    suspend fun addWakeUpTimestamp(timestamp: Long)

    fun getPreferredCity(): Flow<String?>

    suspend fun setPreferredCity(city: String)

    fun isLocationAlarmEnabled(): Flow<Boolean>

    suspend fun setLocationAlarmEnabled(enabled: Boolean)

    fun getPreferredTransportMode(): Flow<String>

    suspend fun setPreferredTransportMode(mode: String)

    fun isExactAlarmPermissionSkipped(): Flow<Boolean>

    suspend fun setExactAlarmPermissionSkipped(skipped: Boolean)

    fun isFullScreenIntentPermissionSkipped(): Flow<Boolean>

    suspend fun setFullScreenIntentPermissionSkipped(skipped: Boolean)

    fun getSyncAlertMutedUntil(): Flow<Long>

    suspend fun setSyncAlertMutedUntil(timestamp: Long)
}
