package digital.tonima.core.ai.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DailyBriefingRepository {
    fun getDailyBriefing(): Flow<String?>

    suspend fun saveDailyBriefing(
        briefing: String,
        generatedDate: LocalDate = LocalDate.now(),
    )

    /** The calendar date the currently saved briefing was generated for, or null if none yet. */
    suspend fun getLastGeneratedDate(): LocalDate?
}
