package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

interface AppMetadataRepository {
    fun getAutostartSuggestionDismissed(): Flow<Boolean>

    suspend fun setAutostartSuggestionDismissed(dismissed: Boolean)

    fun getInstallationDate(): Flow<Long>

    suspend fun setInstallationDate(date: Long)

    fun isRatingPrompted(): Flow<Boolean>

    suspend fun setRatingPrompted(prompted: Boolean)

    fun isRatingCompleted(): Flow<Boolean>

    suspend fun setRatingCompleted(completed: Boolean)

    fun isOnboardingCompleted(): Flow<Boolean>

    suspend fun setOnboardingCompleted(completed: Boolean)

    fun isExactAlarmPermissionSkipped(): Flow<Boolean>

    suspend fun setExactAlarmPermissionSkipped(skipped: Boolean)

    fun isFullScreenIntentPermissionSkipped(): Flow<Boolean>

    suspend fun setFullScreenIntentPermissionSkipped(skipped: Boolean)
}
