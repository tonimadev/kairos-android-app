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

    fun isSkipWeekendsEnabled(): Flow<Boolean>

    suspend fun setSkipWeekendsEnabled(enabled: Boolean)

    fun getAutoDismissMinutes(): Flow<Int>

    suspend fun setAutoDismissMinutes(minutes: Int)

    /** When enabled, events with a meeting URL will auto-open the link instead of showing the alarm screen. */
    fun isAutoJoinEnabled(): Flow<Boolean>

    suspend fun setAutoJoinEnabled(enabled: Boolean)

    /** When enabled, DND is automatically toggled on/off at meeting start/end. */
    fun isAutoFocusModeEnabled(): Flow<Boolean>

    suspend fun setAutoFocusModeEnabled(enabled: Boolean)

    fun getCustomRingtoneUri(): Flow<String?>

    suspend fun setCustomRingtoneUri(uri: String?)
}
