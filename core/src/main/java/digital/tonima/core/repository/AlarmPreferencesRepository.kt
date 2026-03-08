package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

interface AlarmPreferencesRepository {
    fun isGlobalAlarmEnabled(): Flow<Boolean>

    suspend fun setGlobalAlarmEnabled(enabled: Boolean)

    fun getVibrateOnly(): Flow<Boolean>

    suspend fun setVibrateOnly(enabled: Boolean)

    fun isAllDayAlarmsEnabled(): Flow<Boolean>

    suspend fun setAllDayAlarmsEnabled(enabled: Boolean)

    fun getAllDayAlarmHour(): Flow<Int>

    suspend fun setAllDayAlarmHour(hour: Int)

    fun getAlarmOffsetMinutes(): Flow<Long>

    suspend fun setAlarmOffsetMinutes(minutes: Long)

    fun getSnoozeTimeMinutes(): Flow<Int>

    suspend fun setSnoozeTimeMinutes(minutes: Int)
}
