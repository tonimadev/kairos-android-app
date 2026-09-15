package digital.tonima.core.ai.repository

import kotlinx.coroutines.flow.Flow

interface DailyBriefingRepository {
    fun getDailyBriefing(): Flow<String?>

    suspend fun saveDailyBriefing(briefing: String)
}
