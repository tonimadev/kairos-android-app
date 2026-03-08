package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

interface EventPreferencesRepository {
    fun getDisabledEventIds(): Flow<Set<String>>

    suspend fun setDisabledEventIds(ids: Set<String>)

    fun getDisabledSeriesIds(): Flow<Set<String>>

    suspend fun setDisabledSeriesIds(ids: Set<String>)

    fun getVibrateOnlyEventIds(): Flow<Set<String>>

    suspend fun setVibrateOnlyEventIds(ids: Set<String>)

    /**
     * Returns the set of calendar IDs the user has chosen to include.
     * An empty set means "all calendars" (no filter applied).
     */
    fun getEnabledCalendarIds(): Flow<Set<String>>

    suspend fun setEnabledCalendarIds(ids: Set<String>)
}
