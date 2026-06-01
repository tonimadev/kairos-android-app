package digital.tonima.core.usecases

import digital.tonima.core.model.InsightsPeriod

interface GetMeetingTimeStatsUseCase {
    suspend operator fun invoke(period: InsightsPeriod): List<Pair<String, Float>>
}
