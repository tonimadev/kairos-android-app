package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

interface UserStatsRepository {
    fun getSnoozeCount(): Flow<Int>

    suspend fun incrementSnoozeCount()

    fun getAiUsageCount(): Flow<Int>

    suspend fun incrementAiUsageCount()

    fun getWakeUpHistory(): Flow<List<Long>>

    suspend fun addWakeUpTimestamp(timestamp: Long)
}
