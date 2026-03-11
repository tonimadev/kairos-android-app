package digital.tonima.core.repository

import kotlinx.coroutines.flow.Flow

interface DailyBriefingRepository {
    fun getDailyBriefing(): Flow<String?>

    suspend fun saveDailyBriefing(briefing: String)
}
